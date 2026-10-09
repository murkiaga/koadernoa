package com.koadernoa.app.objektuak.jokabidea.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;

import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;

class JokabideDesegokiaPdfServiceTest {

    private final AplikazioAukeraService aukerak = mock(AplikazioAukeraService.class);
    private final JokabideDesegokiaPdfService service = new JokabideDesegokiaPdfService(
            mock(TemplateEngine.class), mock(JokabideDesegokiaTxantiloiService.class), aukerak);

    @Test
    void konfiguratutakoHerriaErabiltzenDuEtaAldaketakBerehalaIrakurtzenDitu() {
        when(aukerak.get(AplikazioAukeraService.IKASTETXEAREN_HERRIA))
                .thenReturn("  Gernika-Lumo  ", "Durango");

        assertThat(service.herriaTestua()).isEqualTo("Gernika-Lumo");
        assertThat(service.herriaTestua()).isEqualTo("Durango");
    }

    @Test
    void konfiguratuGabekoHerriakOrdezkoBalioaMantentzenDu() {
        when(aukerak.get(AplikazioAukeraService.IKASTETXEAREN_HERRIA))
                .thenReturn(null, "   ");

        assertThat(service.herriaTestua()).isEqualTo("………….………….");
        assertThat(service.herriaTestua()).isEqualTo("………….………….");
    }
}
