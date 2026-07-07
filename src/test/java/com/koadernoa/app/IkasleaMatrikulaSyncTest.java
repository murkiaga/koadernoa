package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.entitateak.MatrikulaEgoera;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.repository.IkasleaRepository;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;
import com.koadernoa.app.objektuak.modulua.service.IkasleaService;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;
import com.koadernoa.app.objektuak.zikloak.service.InportazioZerbitzua;

class IkasleaMatrikulaSyncTest {

    private final KoadernoaRepository koadernoaRepo = mock(KoadernoaRepository.class);
    private final IkasleaRepository ikasleaRepo = mock(IkasleaRepository.class);
    private final MatrikulaRepository matrikulaRepo = mock(MatrikulaRepository.class);
    private final TaldeaRepository taldeaRepo = mock(TaldeaRepository.class);
    private final IkasturteaRepository ikasturteaRepo = mock(IkasturteaRepository.class);

    @Test
    void syncKoadernoakTalderakoUsesOnlyActiveYearNotebookIds() {
        IkasleaService service = new IkasleaService(koadernoaRepo, ikasleaRepo, matrikulaRepo, taldeaRepo, ikasturteaRepo);
        Koadernoa koadernoa = koadernoa(10L, taldea(1L, "2SMA"), true, true);
        when(koadernoaRepo.findActiveYearKoadernoIdsByTaldea(1L)).thenReturn(List.of(10L));
        when(koadernoaRepo.findById(10L)).thenReturn(Optional.of(koadernoa));
        when(ikasleaRepo.findHnasByTaldeaId(1L)).thenReturn(List.of("A1"));
        when(matrikulaRepo.findToRemoveByKoadernoAndNotInHnas(10L, List.of("A1"))).thenReturn(List.of());
        when(ikasleaRepo.findTeamStudentsNotEnrolledInKoaderno(1L, 10L)).thenReturn(List.of());

        service.syncKoadernoakTalderako(1L);

        verify(koadernoaRepo).findActiveYearKoadernoIdsByTaldea(1L);
        verify(koadernoaRepo, never()).findKoadernoIdsByTaldeaId(any());
    }

    @Test
    void syncKoadernoBakarraRemovesOnlyRepositorySelectedMatrikulatuakAndDoesNotUpdateExistingStates() {
        IkasleaService service = new IkasleaService(koadernoaRepo, ikasleaRepo, matrikulaRepo, taldeaRepo, ikasturteaRepo);
        Taldea taldea = taldea(1L, "2SMA");
        Koadernoa koadernoa = koadernoa(10L, taldea, true, true);
        Matrikula removableMatrikulatua = matrikula(50L, ikaslea(5L, "OLD"), koadernoa, MatrikulaEgoera.MATRIKULATUA);
        Ikaslea withoutEnrollment = ikaslea(6L, "NEW");
        when(koadernoaRepo.findById(10L)).thenReturn(Optional.of(koadernoa));
        when(ikasleaRepo.findHnasByTaldeaId(1L)).thenReturn(List.of("NEW"));
        when(matrikulaRepo.findToRemoveByKoadernoAndNotInHnas(10L, List.of("NEW"))).thenReturn(List.of(removableMatrikulatua));
        when(ikasleaRepo.findTeamStudentsNotEnrolledInKoaderno(1L, 10L)).thenReturn(List.of(withoutEnrollment));

        service.syncKoadernoBakarra(10L);

        verify(matrikulaRepo).deleteAll(List.of(removableMatrikulatua));
        ArgumentCaptor<Matrikula> captor = ArgumentCaptor.forClass(Matrikula.class);
        verify(matrikulaRepo).save(captor.capture());
        assertThat(captor.getValue().getEgoera()).isEqualTo(MatrikulaEgoera.MATRIKULATUA);
        assertThat(captor.getValue().getIkaslea()).isSameAs(withoutEnrollment);
    }

    @Test
    void taldeAldaketakDeletesOnlyOtherActiveYearMatrikulatuakAndKeepsPendingOrHistoricalByQuery() {
        IkasleaService service = new IkasleaService(koadernoaRepo, ikasleaRepo, matrikulaRepo, taldeaRepo, ikasturteaRepo);
        Taldea berria = taldea(2L, "2SMA");
        Ikaslea ikaslea = ikaslea(7L, "A1");
        ikaslea.setTaldea(taldea(1L, "1SMA"));
        Koadernoa berrikoKoadernoa = koadernoa(20L, berria, true, true);
        Matrikula oldActiveMatrikulatua = matrikula(100L, ikaslea, koadernoa(10L, taldea(1L, "1SMA"), true, true), MatrikulaEgoera.MATRIKULATUA);
        when(ikasleaRepo.findById(7L)).thenReturn(Optional.of(ikaslea));
        when(taldeaRepo.findById(2L)).thenReturn(Optional.of(berria));
        when(ikasturteaRepo.findFirstByAktiboaTrueOrderByIdDesc()).thenReturn(Optional.of(new Ikasturtea()));
        when(koadernoaRepo.findActiveYearKoadernoIdsByTaldea(2L)).thenReturn(List.of(20L));
        when(koadernoaRepo.findAllById(List.of(20L))).thenReturn(List.of(berrikoKoadernoa));
        when(matrikulaRepo.findActiveYearMatrikulatuakByIkasleaAndNotTaldea(7L, 2L)).thenReturn(List.of(oldActiveMatrikulatua));
        when(matrikulaRepo.existsByIkasleaIdAndKoadernoaId(7L, 20L)).thenReturn(false);

        var emaitza = service.aldatuIkaslearenTaldea(7L, 2L);

        assertThat(emaitza.kendutakoMatrikulak()).isEqualTo(1);
        verify(matrikulaRepo).deleteAll(List.of(oldActiveMatrikulatua));
        verify(matrikulaRepo, never()).findByIkasleaIdAndKoadernoaIdIn(any(), any());
        assertThat(ikaslea.getTaldea()).isSameAs(berria);
    }

    @Test
    void importCreatesNewTeamActiveEnrollmentsDeletesOtherTeamMatrikulatuakAndDoesNotDuplicateOrChangeExistingEnrollment() throws Exception {
        Taldea taldea = taldea(2L, "2SMA");
        Koadernoa berriaSortzeko = koadernoa(20L, taldea, true, true);
        Koadernoa lehendikPendiente = koadernoa(21L, taldea, true, true);
        Ikaslea ikaslea = ikaslea(7L, "A1");
        Matrikula besteTaldeAktiboMatrikulatua = matrikula(100L, ikaslea, koadernoa(10L, taldea(1L, "1SMA"), true, true), MatrikulaEgoera.MATRIKULATUA);
        when(taldeaRepo.findById(2L)).thenReturn(Optional.of(taldea));
        when(koadernoaRepo.findActiveYearKoadernoIdsByTaldea(2L)).thenReturn(List.of(20L, 21L));
        when(koadernoaRepo.findAllById(List.of(20L, 21L))).thenReturn(List.of(berriaSortzeko, lehendikPendiente));
        when(ikasleaRepo.findByHna("A1")).thenReturn(Optional.of(ikaslea));
        when(ikasleaRepo.save(any(Ikaslea.class))).thenAnswer(inv -> inv.getArgument(0));
        when(matrikulaRepo.existsByIkasleaIdAndKoadernoaId(7L, 20L)).thenReturn(false);
        when(matrikulaRepo.existsByIkasleaIdAndKoadernoaId(7L, 21L)).thenReturn(true);
        when(matrikulaRepo.findActiveYearMatrikulatuakByIkasleaAndNotTaldea(7L, 2L)).thenReturn(List.of(besteTaldeAktiboMatrikulatua));
        InportazioZerbitzua service = new InportazioZerbitzua(taldeaRepo, koadernoaRepo, ikasleaRepo, matrikulaRepo);
        ReflectionTestUtils.setField(service, "uploadsDir", "/tmp");

        service.inportatuTaldekoXlsx(2L, xlsx("A1"));

        assertThat(ikaslea.getTaldea()).isSameAs(taldea);
        verify(matrikulaRepo).deleteAll(List.of(besteTaldeAktiboMatrikulatua));
        ArgumentCaptor<Matrikula> captor = ArgumentCaptor.forClass(Matrikula.class);
        verify(matrikulaRepo).save(captor.capture());
        assertThat(captor.getValue().getKoadernoa()).isSameAs(berriaSortzeko);
        verify(matrikulaRepo, never()).findByIkasleaIdAndKoadernoaId(7L, 21L);
    }

    private MockMultipartFile xlsx(String hna) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = wb.createSheet("Zerrenda");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("HNA");
            header.createCell(1).setCellValue("NAN");
            header.createCell(2).setCellValue("ABIZENA 1");
            header.createCell(3).setCellValue("ABIZENA 2");
            header.createCell(4).setCellValue("IZENA");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue(hna);
            row.createCell(1).setCellValue("12345678A");
            row.createCell(2).setCellValue("Abizena");
            row.createCell(4).setCellValue("Izena");
            wb.write(out);
            return new MockMultipartFile("file", "ikasleak.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new ByteArrayInputStream(out.toByteArray()));
        }
    }

    private Taldea taldea(Long id, String izena) { Taldea t = new Taldea(); t.setId(id); t.setIzena(izena); return t; }
    private Ikaslea ikaslea(Long id, String hna) { Ikaslea i = new Ikaslea(); i.setId(id); i.setHna(hna); return i; }
    private Matrikula matrikula(Long id, Ikaslea i, Koadernoa k, MatrikulaEgoera e) { Matrikula m = new Matrikula(); m.setId(id); m.setIkaslea(i); m.setKoadernoa(k); m.setEgoera(e); return m; }
    private Koadernoa koadernoa(Long id, Taldea taldea, boolean activeYear, boolean activeModule) {
        Ikasturtea ikasturtea = new Ikasturtea(); ikasturtea.setAktiboa(activeYear);
        Egutegia egutegia = new Egutegia(); egutegia.setIkasturtea(ikasturtea);
        Moduloa moduloa = new Moduloa(); moduloa.setTaldea(taldea); moduloa.setAktibo(activeModule);
        Koadernoa k = new Koadernoa(); k.setId(id); k.setEgutegia(egutegia); k.setModuloa(moduloa); return k;
    }
}
