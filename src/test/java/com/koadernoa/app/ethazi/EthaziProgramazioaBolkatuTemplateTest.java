package com.koadernoa.app.ethazi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class EthaziProgramazioaBolkatuTemplateTest {
    @Test
    void bolkatzekoIkonoaErronkarenGoiburuanDago() throws IOException {
        try (var input = getClass().getResourceAsStream(
                "/templates/irakasleak/ethazi/programazioa.html")) {
            assertThat(input).isNotNull();
            String html = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            int erronkaElementua = html.indexOf("<li class=\"erronka-item\"");
            int bolkatuFormularioa = html.indexOf("class=\"erronka-bolkatu-form\"", erronkaElementua);
            int erronkaGoiburua = html.indexOf("class=\"erronka-header\"", erronkaElementua);
            int kronograma = html.indexOf("class=\"kronograma-laburpena\"", erronkaElementua);

            assertThat(bolkatuFormularioa).isGreaterThan(erronkaElementua).isLessThan(erronkaGoiburua);
            assertThat(bolkatuFormularioa).isLessThan(kronograma);
            assertThat(html).contains("@{/img/eb-bolkatu.png}",
                    "Erronka hau denboralizaziora bolkatu");
        }
    }
}
