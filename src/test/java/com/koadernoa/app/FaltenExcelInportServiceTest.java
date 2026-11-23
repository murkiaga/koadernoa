package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Asistentzia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa;
import com.koadernoa.app.objektuak.koadernoak.repository.AsistentziaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.SaioaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.AsistentziaService;
import com.koadernoa.app.objektuak.koadernoak.service.FaltenEbaluaketaService;
import com.koadernoa.app.objektuak.koadernoak.service.FaltenEbaluaketaService.EbaluaketaAukera;
import com.koadernoa.app.objektuak.koadernoak.service.FaltenExcelInportService;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;

class FaltenExcelInportServiceTest {

    @Test
    void aurrekoFaltakBackupEginEbaluazioOsoaEzabatuEtaExcelBerriaInportatzenDu() throws Exception {
        MatrikulaRepository matrikulaRepository = mock(MatrikulaRepository.class);
        SaioaRepository saioaRepository = mock(SaioaRepository.class);
        AsistentziaRepository asistentziaRepository = mock(AsistentziaRepository.class);
        AsistentziaService asistentziaService = mock(AsistentziaService.class);
        FaltenEbaluaketaService ebaluaketaService = mock(FaltenEbaluaketaService.class);
        FaltenExcelInportService service = new FaltenExcelInportService(
                matrikulaRepository, saioaRepository, asistentziaRepository,
                asistentziaService, ebaluaketaService);

        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setId(4L);
        Ikaslea ikaslea = new Ikaslea();
        ikaslea.setIzena("Ane");
        ikaslea.setAbizena1("Agirre");
        Matrikula matrikula = new Matrikula();
        matrikula.setId(8L);
        matrikula.setIkaslea(ikaslea);
        matrikula.setKoadernoa(koadernoa);

        Saioa saioa = new Saioa();
        saioa.setId(12L);
        saioa.setKoadernoa(koadernoa);
        saioa.setData(LocalDate.of(2026, 10, 5));
        saioa.setHasieraSlot(2);
        saioa.setIraupenaSlot(1);
        Asistentzia aurrekoFalta = new Asistentzia();
        aurrekoFalta.setSaioa(saioa);
        aurrekoFalta.setMatrikula(matrikula);
        aurrekoFalta.setEgoera(Asistentzia.AsistentziaEgoera.HUTS);

        EbaluaketaAukera ebaluaketa = new EbaluaketaAukera(1, "1. ebaluazioa",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 18), 100);
        when(ebaluaketaService.lortuAukera(koadernoa, 1)).thenReturn(ebaluaketa);
        when(matrikulaRepository.findByKoadernoaIdAndEgoeraMatrikulatuta(4L)).thenReturn(List.of(matrikula));
        when(asistentziaRepository.findFaltakEbaluazioan(eq(4L), eq(ebaluaketa.hasieraData()),
                eq(ebaluaketa.bukaeraData()), any())).thenReturn(List.of(aurrekoFalta));
        when(saioaRepository.findByKoadernoaIdAndDataBetweenOrderByDataAscHasieraSlotAsc(
                4L, saioa.getData(), saioa.getData())).thenReturn(List.of(saioa));

        var emaitza = service.inportatu(koadernoa, 1,
                excel("Agirre, Ane", "05/10/2026", "2. ordua"));

        assertThat(emaitza.getSortuak()).isEqualTo(1);
        assertThat(emaitza.getBackupFaltak()).hasSize(1);
        assertThat(emaitza.backupTestua()).isEqualTo("Agirre, Ane;2026-10-05;2");
        verify(asistentziaRepository).deleteAll(List.of(aurrekoFalta));
        verify(asistentziaService).markatu(12L, 8L, Asistentzia.AsistentziaEgoera.HUTS, null, null);
    }

    @Test
    void ebaluaziotikKanpokoExcelakEzDuAurrekoFaltarikEzabatzen() throws Exception {
        MatrikulaRepository matrikulaRepository = mock(MatrikulaRepository.class);
        SaioaRepository saioaRepository = mock(SaioaRepository.class);
        AsistentziaRepository asistentziaRepository = mock(AsistentziaRepository.class);
        AsistentziaService asistentziaService = mock(AsistentziaService.class);
        FaltenEbaluaketaService ebaluaketaService = mock(FaltenEbaluaketaService.class);
        FaltenExcelInportService service = new FaltenExcelInportService(
                matrikulaRepository, saioaRepository, asistentziaRepository,
                asistentziaService, ebaluaketaService);
        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setId(4L);
        when(ebaluaketaService.lortuAukera(koadernoa, 1)).thenReturn(new EbaluaketaAukera(
                1, "1. ebaluazioa", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 18), 100));
        when(matrikulaRepository.findByKoadernoaIdAndEgoeraMatrikulatuta(4L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.inportatu(koadernoa, 1,
                excel("Agirre, Ane", "10/01/2027", "2. ordua")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ebaluaziotik kanpoko datak");

        verify(asistentziaRepository, never()).deleteAll(any());
        verify(asistentziaService, never()).ensureSaioakForDate(any(), any());
        verify(saioaRepository, never()).save(any());
    }

    private MockMultipartFile excel(String ikaslea, String data, String ordua) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("faltas");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("ALUMNO");
            header.createCell(1).setCellValue("FECHA");
            header.createCell(2).setCellValue("TIEMPO");
            header.createCell(3).setCellValue("JUSTIFICADA");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue(ikaslea);
            row.createCell(1).setCellValue(data);
            row.createCell(2).setCellValue(ordua);
            row.createCell(3).setCellValue("No");
            workbook.write(output);
            return new MockMultipartFile("fitxategia", "faltak.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }
}
