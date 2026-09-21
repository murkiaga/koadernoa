package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import com.koadernoa.app.objektuak.koadernoak.service.FaltenJakinarazpenPdfService;
import com.koadernoa.app.objektuak.koadernoak.service.FaltenJakinarazpenPdfService.FaltenJakinarazpenaData;

class FaltenJakinarazpenPdfServiceTest {
    @ParameterizedTest
    @CsvSource({"4,96", "20,80", "25,75"})
    void biHizkuntzetakoBertaratzeMugaMailatikKalkulatzenDa(int faltaMuga, int bertaratzeMuga) throws Exception {
        var service = new FaltenJakinarazpenPdfService();
        ReflectionTestUtils.setField(service, "txantiloia",
            new ClassPathResource("templates/txostenak/MD6309-falten-jakinarazpena.dotx"));
        var data = FaltenJakinarazpenaData.of("Taldea", "Ikaslea", 30, "Modulua",
            31, faltaMuga, 2026, LocalDate.of(2026, 9, 21), "Irakaslea");

        try (var pdf = PDDocument.load(service.sortuPdf(data))) {
            String text = new PDFTextStripper().getText(pdf).replaceAll("\\s+", " ");
            assertThat(text)
                .contains("Modulu batean bertaratzea % " + bertaratzeMuga + " baino txikiagoa denean")
                .contains("Cuando la asistencia a un módulo sea inferior al " + bertaratzeMuga + "%");
            assertThat(pdf.getNumberOfPages()).isEqualTo(2);
        }
    }
}
