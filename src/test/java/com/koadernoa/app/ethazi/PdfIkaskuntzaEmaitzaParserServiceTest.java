package com.koadernoa.app.ethazi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.koadernoa.app.ethazi.service.PdfIkaskuntzaEmaitzaParserService;
import com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza;

class PdfIkaskuntzaEmaitzaParserServiceTest {
    private final PdfIkaskuntzaEmaitzaParserService parser = new PdfIkaskuntzaEmaitzaParserService();

    @Test
    void euskarazkoEmaitzakAteraEtaEbaluazioIrizpideakBaztertzenDitu() {
        String text = """
                Lanbide-modulua: Informatika-sistemak
                Kodea: 0483
                b) Ikaskuntzaren emaitzak eta ebaluazio-irizpideak
                1. Informatika-sistemak ebaluatzen ditu, eta haien osagaiak eta ezaugarriak identifikatzen ditu.
                Ebaluazio-irizpideak:
                a) Informatika-sistema baten osagai fisikoak eta interkonexio-mekanismoak ezagutu ditu.
                b) Ordenagailu bat abian jartzeko prozesua egiaztatu du.
                c) Hainbat motatako gailu periferikoak sailkatu ditu.
                1. LANBIDE-M ODULUA: INFORMATIKA-SISTEMAK
                2. Sistema eragileak instalatzen ditu, eta, horretarako, prozesua planifikatzen du eta
                25
                1. Lanbide-modulua: INFORMATIKA-SISTEMAK
                Kodea: 0483
                dokumentazio teknikoa interpretatzen du.
                Ebaluazio-irizpideak:
                a) Informatika-sistema baten elementu funtzionalak identifikatu ditu.
                c) Oinarrizko edukiak
                1. MIKROINFORMATIKA SISTEMAK USTIATZEA
                1. Lanbide-modulua: INFORMATIKA-SISTEMAK
                """;

        var result = parser.parseText(text, Hizkuntza.EUSKARA);

        assertThat(result.lerroak()).hasSize(2);
        assertThat(result.lerroak().get(0).eeiKodea()).isEqualTo("0483");
        assertThat(result.lerroak().get(0).kodea()).isEqualTo("IE1");
        assertThat(result.lerroak().get(0).deskribapena()).isEqualTo(
                "Informatika-sistemak ebaluatzen ditu, eta haien osagaiak eta ezaugarriak identifikatzen ditu.");
        assertThat(result.lerroak().get(1).deskribapena()).isEqualTo(
                "Sistema eragileak instalatzen ditu, eta, horretarako, prozesua planifikatzen du eta dokumentazio teknikoa interpretatzen du.");
        assertThat(result.lerroak()).extracting(row -> row.deskribapena()).allSatisfy(description ->
                assertThat(description).doesNotContain("Ikaskuntzaren emaitzak", "Ebaluazio-irizpideak", "identifikatu ditu"));
    }

    @Test
    void gaztelaniazkoEmaitzakEtaKodeAlfanumerikoaAteratzenDitu() {
        String text = """
                Módulo profesional: Proyecto
                Código: E100
                Resultados de aprendizaje y criterios de evaluación
                1. Define el proyecto justificando las decisiones adoptadas.
                a) Se han identificado las necesidades.
                2. Planifica la ejecución del proyecto.
                a) Se han secuenciado las actividades.
                Contenidos
                """;

        var result = parser.parseText(text, Hizkuntza.GAZTELERA);

        assertThat(result.lerroak()).extracting(row -> row.eeiKodea()).containsOnly("E100");
        assertThat(result.lerroak()).extracting(row -> row.ordena()).containsExactly(1, 2);
        assertThat(result.lerroak()).extracting(row -> row.deskribapena())
                .allMatch(value -> !value.contains("Se han"));
    }

    @Test
    void zenbakiaEtaDeskribapenaLerroBanatanBadiraEmaitzaHartuEtaOrientabideetanGelditzenDa() {
        String text = """
                Lanbide-modulua: Markatzeko lengoaiak eta informazioa kudeatzeko sistemak
                Kodea: 0373
                b) Ikaskuntzaren emaitzak eta ebaluazio-irizpideak
                1. Markatzeko lengoaien ezaugarriak ezagutzen ditu.
                Ebaluazio-irizpideak:
                a) Ezaugarri orokorrak identifikatu ditu.
                2. Informazioa transmititzeko dokumentuak erabiltzen ditu.
                Ebaluazio-irizpideak:
                a) Dokumentuak sortu ditu.
                3. XML dokumentuen edukia atzitzen du.
                Ebaluazio-irizpideak:
                a) Atzipen-metodoak identifikatu ditu.
                4.
                XML dokumentuetarako baliozko eskemak ezartzen ditu, eta haien egitura eta sintaxia definitzen ditu.
                Ebaluazio-irizpideak:
                a) Eskemak sortu ditu.
                d) Orientabide metodologikoak
                1. Unitate didaktikoak sekuentziatzea gomendatzen da.
                2. Jarduera praktikoak egitea gomendatzen da.
                """;

        var result = parser.parseText(text, Hizkuntza.EUSKARA);

        assertThat(result.lerroak()).hasSize(4);
        assertThat(result.lerroak()).extracting(row -> row.ordena()).containsExactly(1, 2, 3, 4);
        assertThat(result.lerroak().get(3).eeiKodea()).isEqualTo("0373");
        assertThat(result.lerroak().get(3).deskribapena()).startsWith("XML dokumentuetarako baliozko eskemak");
        assertThat(result.lerroak()).extracting(row -> row.deskribapena())
                .noneMatch(value -> value.contains("Unitate didaktikoak") || value.contains("Jarduera praktikoak"));
    }
}
