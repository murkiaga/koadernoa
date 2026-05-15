package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Rola;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoJabeEsleipenService;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoJabeInportazioEmaitza;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;

class KoadernoJabeEsleipenServiceTest {

    private final KoadernoaRepository koadernoaRepository = mock(KoadernoaRepository.class);
    private final IrakasleaRepository irakasleaRepository = mock(IrakasleaRepository.class);
    private final KoadernoJabeEsleipenService service = new KoadernoJabeEsleipenService(koadernoaRepository, irakasleaRepository);

    @Test
    void exportatuJabeGabekoKoadernoakIncludesTechnicalIdentifiers() throws Exception {
        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setId(42L);
        Moduloa moduloa = new Moduloa();
        moduloa.setId(77L);
        moduloa.setEeiKodea("0484");
        moduloa.setKodea("DB");
        moduloa.setIzena("Datu-baseak");
        koadernoa.setModuloa(moduloa);
        when(koadernoaRepository.findJabeGabekoakWithRelations()).thenReturn(List.of(koadernoa));

        byte[] edukia = service.exportatuJabeGabekoKoadernoak();

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(edukia))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("koaderno_id");
            assertThat(sheet.getRow(0).getCell(6).getStringCellValue()).isEqualTo("modulo_id");
            assertThat(sheet.getRow(1).getCell(0).getNumericCellValue()).isEqualTo(42D);
            assertThat(sheet.getRow(1).getCell(6).getNumericCellValue()).isEqualTo(77D);
            assertThat(sheet.getRow(1).getCell(10).getStringCellValue()).isBlank();
        }
    }

    @Test
    void inportatuJabeEsleipenakAssignsOwnerAndAdditionalTeachersWithoutDuplicates() throws Exception {
        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setId(42L);
        koadernoa.setIrakasleak(List.of());
        Irakaslea jabea = irakaslea(1L, "jabea@example.com");
        Irakaslea gehigarria = irakaslea(2L, "bestea@example.com");
        when(koadernoaRepository.findByIdWithJabeaEtaIrakasleak(42L)).thenReturn(Optional.of(koadernoa));
        when(irakasleaRepository.findByEmailaIgnoreCase("jabea@example.com")).thenReturn(Optional.of(jabea));
        when(irakasleaRepository.findByEmailaIgnoreCase("bestea@example.com")).thenReturn(Optional.of(gehigarria));

        KoadernoJabeInportazioEmaitza emaitza = service.inportatuJabeEsleipenak(fitxategia(
                "42", "jabea@example.com", "bestea@example.com;jabea@example.com"));

        assertThat(emaitza.getEsleitutakoKoadernoak()).isEqualTo(1);
        assertThat(emaitza.getErroreak()).isEmpty();
        ArgumentCaptor<Koadernoa> captor = ArgumentCaptor.forClass(Koadernoa.class);
        verify(koadernoaRepository).save(captor.capture());
        Koadernoa gordeta = captor.getValue();
        assertThat(gordeta.getJabea()).isSameAs(jabea);
        assertThat(gordeta.getIrakasleak()).extracting(Irakaslea::getEmaila)
                .containsExactlyInAnyOrder("jabea@example.com", "bestea@example.com");
    }

    @Test
    void inportatuJabeEsleipenakDoesNotOverwriteExistingOwner() throws Exception {
        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setId(42L);
        koadernoa.setJabea(irakaslea(9L, "zaharra@example.com"));
        when(koadernoaRepository.findByIdWithJabeaEtaIrakasleak(42L)).thenReturn(Optional.of(koadernoa));

        KoadernoJabeInportazioEmaitza emaitza = service.inportatuJabeEsleipenak(fitxategia(
                "42", "jabea@example.com", ""));

        assertThat(emaitza.getEsleitutakoKoadernoak()).isZero();
        assertThat(emaitza.getErroreak()).anyMatch(e -> e.contains("jabea dauka jada"));
    }

    private Irakaslea irakaslea(Long id, String emaila) {
        Irakaslea irakaslea = new Irakaslea();
        irakaslea.setId(id);
        irakaslea.setEmaila(emaila);
        irakaslea.setIzena(emaila);
        irakaslea.setRola(Rola.IRAKASLEA);
        return irakaslea;
    }

    private MockMultipartFile fitxategia(String koadernoId, String jabeEmaila, String irakasleEmailak) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("jabe-gabeak");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("koaderno_id");
            header.createCell(1).setCellValue("jabe_emaila");
            header.createCell(2).setCellValue("irakasle_emailak");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(koadernoId);
            row.createCell(1).setCellValue(jabeEmaila);
            row.createCell(2).setCellValue(irakasleEmailak);
            wb.write(out);
            return new MockMultipartFile("fitxategia", "jabe-gabeko-koadernoak.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }
}
