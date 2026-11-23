package com.koadernoa.app.funtzionalitateak.irakasle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.service.FaltenExcelInportService;
import com.koadernoa.app.objektuak.koadernoak.service.FaltenExcelInportService.BackupFalta;
import com.koadernoa.app.objektuak.koadernoak.service.FaltenExcelInportService.InportEmaitza;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoaService;

@ExtendWith(MockitoExtension.class)
class FaltenInportControllerTest {

    @Mock KoadernoaService koadernoaService;
    @Mock FaltenExcelInportService faltenExcelInportService;
    @InjectMocks DenboralizazioaController controller;

    @Test
    void aurrekoFaltakDaudeneanTxtBackupDeskargagarriaItzultzenDu() throws Exception {
        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setId(4L);
        MockMultipartFile fitxategia = new MockMultipartFile(
                "fitxategia", "faltak.xlsx", "application/vnd.ms-excel", new byte[] {1});
        InportEmaitza emaitza = new InportEmaitza(1, 1, 0, List.of(),
                List.of(new BackupFalta("Agirre, Ane", LocalDate.of(2026, 10, 5), 2)));
        when(koadernoaService.findByIdWithEgutegiaAndEgunBereziak(4L)).thenReturn(Optional.of(koadernoa));
        when(faltenExcelInportService.inportatu(koadernoa, 1, fitxategia)).thenReturn(emaitza);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        request.addHeader("X-Falten-Inport-Ajax", "true");
        request.setAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
        request.setAttribute(DispatcherServlet.FLASH_MAP_MANAGER_ATTRIBUTE, new SessionFlashMapManager());

        Object erantzuna = controller.inportatuFaltakExcel(
                koadernoa, 1, fitxategia, 2026, 11, new RedirectAttributesModelMap(),
                request, servletResponse, null);

        assertThat(erantzuna).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> response = (ResponseEntity<?>) erantzuna;
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"faltak-backup-1-ebaluazioa.txt\"");
        assertThat(new String((byte[]) response.getBody(), StandardCharsets.UTF_8))
                .isEqualTo("Agirre, Ane;2026-10-05;2" + System.lineSeparator());
        FlashMap flashMap = (FlashMap) request.getAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE);
        assertThat(flashMap.get("success").toString())
                .isEqualTo("Inportazioa eginda. Irakurrita: 1 · Sortuta: 1 · Baztertuta: 0");
    }
}
