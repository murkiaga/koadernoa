package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class ProgramazioaTemplateInputLimitsTest {
    @Test
    void udEtaJardueraEremuekDatuBasekoLuzeraMugakDauzkate() throws IOException {
        try (var input = getClass().getResourceAsStream(
                "/templates/irakasleak/programazioa/index.html")) {
            assertThat(input).isNotNull();
            String html = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(html).contains(
                    "id=\"udKodea\" required maxlength=\"20\"",
                    "id=\"udIzenburua\" required maxlength=\"255\"",
                    "id=\"jpIzenburua\" required maxlength=\"255\"");
        }
    }
}
