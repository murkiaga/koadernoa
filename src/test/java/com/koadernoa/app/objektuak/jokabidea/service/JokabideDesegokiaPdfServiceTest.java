package com.koadernoa.app.objektuak.jokabidea.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

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

    @Test
    void txantiloiPertsonalizatuanAplikatutakoNeurriaOrdezkatzenDu() {
        Context context = new Context();
        context.setVariable("aplikatutakoNeurria", "Hausnarketa idatzia & sinatua");

        String html = service.ordezkatuPlaceholderak("""
                <div th:text="${aplikatutakoNeurria}"></div>
                <p>${aplikatutakoNeurria}</p>
                """, context);

        assertThat(html).doesNotContain("${aplikatutakoNeurria}");
        assertThat(html).contains("<div >Hausnarketa idatzia &amp; sinatua</div>");
        assertThat(html).contains("<p>Hausnarketa idatzia &amp; sinatua</p>");
    }

    @Test
    void txantiloiPertsonalizatuanNeurriZuzentzailearenLerroJauziakMantentzenDitu() {
        Context context = new Context();
        context.setVariable("neurriZuzentzailea", "34.a - Lehen lerroa\nBigarren lerroa");

        String html = service.ordezkatuPlaceholderak("""
                <div th:text="${neurriZuzentzailea}"></div>
                <p>${neurriZuzentzailea}</p>
                """, context);

        assertThat(html).contains("<div >34.a - Lehen lerroa<br/>Bigarren lerroa</div>");
        assertThat(html).contains("<p>34.a - Lehen lerroa<br/>Bigarren lerroa</p>");
    }
}
