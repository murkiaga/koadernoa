package com.koadernoa.app.objektuak.jokabidea.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.LocalDate;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.jokabidea.entitateak.JokabideDesegokia;
import com.koadernoa.app.objektuak.jokabidea.entitateak.NeurriZuzentzailea;
import com.koadernoa.app.objektuak.jokabidea.entitateak.PortaeraArrazoia;
import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;

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

    @Test
    void gaztelaniazkoEremuetanEscapingEtaLerroJauziakMantentzenDitu() {
        Context context = new Context();
        context.setVariable("deskribapenZehatzaEs", "Primera línea <segura>\nSegunda línea");

        String html = service.ordezkatuPlaceholderak(
                "<div th:text=\"${deskribapenZehatzaEs}\"></div>", context);

        assertThat(html).contains("Primera línea &lt;segura&gt;<br/>Segunda línea");
        assertThat(html).doesNotContain("${deskribapenZehatzaEs}");
    }

    @Test
    void txantiloiPertsonalizatuakEremuHutsarenThIfEdukiontziaKentzenDu() {
        Context context = new Context();
        context.setVariable("aplikatutakoNeurriaEs", "");

        String html = service.ordezkatuPlaceholderak("""
                <section><div class="deskribapena" th:if="${aplikatutakoNeurriaEs != null and !#strings.isEmpty(aplikatutakoNeurriaEs)}" th:text="${aplikatutakoNeurriaEs}">ezkutatu</div></section>
                """, context);

        assertThat(html).isEqualTo("<section></section>\n");
    }

    @Test
    void txantiloiPertsonalizatuakHizkuntzarenOrrialdeJauziaProzesatzenDu() {
        Context context = new Context();
        context.setVariable("euskarazBadago", false);

        String html = service.ordezkatuPlaceholderak(
                "<div th:class=\"${euskarazBadago} ? 'orrialde-berria' : ''\">ES</div>", context);

        assertThat(html).isEqualTo("<div class=\"\">ES</div>");
    }

    @Test
    void txantiloiLehenetsiaBiHizkuntzetakoAldagaiekinEtaOrrialdeJauziarekinDator() throws Exception {
        JokabideDesegokiaTxantiloiService txantiloiak = new JokabideDesegokiaTxantiloiService(
                "target/test-jokabide-txantiloia.html");

        String html = txantiloiak.irakurriDefektuzkoa();

        assertThat(html).contains("page-break-before: always", "lang=\"es\"",
                "${portaeraArrazoiaEs}", "${deskribapenZehatzaEs}",
                "${neurriZuzentzaileaEs}", "${herriaEtaDataEs}");
    }

    @Test
    void pdfElebidunErrealakBiOrrialdeEtaBiHizkuntzakDitu(@TempDir Path tempDir) throws Exception {
        JokabideDesegokiaPdfService pdfService = pdfServiceErreala(tempDir);
        JokabideDesegokiaPdfService.SortutakoPdfa sortutakoa = pdfService.sortu(jokabideaElebiduna());

        try (PDDocument dokumentua = PDDocument.load(Path.of(sortutakoa.path()).toFile())) {
            assertThat(dokumentua.getNumberOfPages()).isEqualTo(2);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            assertThat(stripper.getText(dokumentua)).contains("IDATZIZKO OHARTARAZPENA", "Gelan gertatutakoa");
            stripper.setStartPage(2);
            stripper.setEndPage(2);
            assertThat(stripper.getText(dokumentua)).contains("APERCIBIMIENTO ESCRITO", "Lo ocurrido en el aula");
        }
    }

    @Test
    void gaztelaniaHutsezkoPdfakOrrialdeBakarraDauka(@TempDir Path tempDir) throws Exception {
        JokabideDesegokia jokabidea = jokabideaElebiduna();
        jokabidea.setDeskribapenZehatza(null);

        var sortutakoa = pdfServiceErreala(tempDir).sortu(jokabidea);

        try (PDDocument dokumentua = PDDocument.load(Path.of(sortutakoa.path()).toFile())) {
            assertThat(dokumentua.getNumberOfPages()).isEqualTo(1);
            assertThat(new PDFTextStripper().getText(dokumentua))
                    .contains("APERCIBIMIENTO ESCRITO", "Lo ocurrido en el aula")
                    .doesNotContain("IDATZIZKO OHARTARAZPENA");
        }
    }

    @Test
    void euskaraHutsezkoPdfakOrrialdeBakarraDauka(@TempDir Path tempDir) throws Exception {
        JokabideDesegokia jokabidea = jokabideaElebiduna();
        jokabidea.setDeskribapenZehatzaEs(null);

        var sortutakoa = pdfServiceErreala(tempDir).sortu(jokabidea);

        try (PDDocument dokumentua = PDDocument.load(Path.of(sortutakoa.path()).toFile())) {
            assertThat(dokumentua.getNumberOfPages()).isEqualTo(1);
            assertThat(new PDFTextStripper().getText(dokumentua))
                    .contains("IDATZIZKO OHARTARAZPENA", "Gelan gertatutakoa")
                    .doesNotContain("APERCIBIMIENTO ESCRITO");
        }
    }

    private JokabideDesegokiaPdfService pdfServiceErreala(Path tempDir) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        JokabideDesegokiaTxantiloiService txantiloiak = new JokabideDesegokiaTxantiloiService(
                tempDir.resolve("pertsonalizatua.html").toString());
        AplikazioAukeraService aukerak = mock(AplikazioAukeraService.class);
        when(aukerak.get(AplikazioAukeraService.IKASTETXEAREN_HERRIA)).thenReturn("Zornotza");
        JokabideDesegokiaPdfService pdfService = new JokabideDesegokiaPdfService(engine, txantiloiak, aukerak);
        ReflectionTestUtils.setField(pdfService, "uploadsDir", tempDir.toString());
        ReflectionTestUtils.setField(pdfService, "subdir", "jokabideak");
        return pdfService;
    }

    private JokabideDesegokia jokabideaElebiduna() {
        Ikasturtea ikasturtea = new Ikasturtea();
        ikasturtea.setIzena("2026-2027");
        Egutegia egutegia = new Egutegia();
        egutegia.setIkasturtea(ikasturtea);
        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setId(4L);
        koadernoa.setEgutegia(egutegia);
        Ikaslea ikaslea = new Ikaslea();
        ikaslea.setId(8L);
        ikaslea.setIzena("Ane");
        ikaslea.setAbizena1("Ikaslea");
        Irakaslea irakaslea = new Irakaslea();
        irakaslea.setIzena("Irati Irakaslea");
        PortaeraArrazoia arrazoia = new PortaeraArrazoia();
        arrazoia.setKodea("30.d");
        arrazoia.setTestua("Irakasleek agindutakoa ez betetzea.");
        arrazoia.setTestuaEs("La simple desobediencia a los profesores, profesoras o autoridades académicas cuando no comporte actitudes de menosprecio, insulto o indisciplina deliberada, así como no atender las indicaciones del resto del personal del centro docente en ejercicio de sus funciones.");
        NeurriZuzentzailea neurria = new NeurriZuzentzailea();
        neurria.setKodea("34.a");
        neurria.setTestua("Jokabideari buruzko hausnarketa.");
        neurria.setTestuaEs("Reflexión sobre la conducta inadecuada.");
        JokabideDesegokia jokabidea = new JokabideDesegokia();
        jokabidea.setIkaslea(ikaslea);
        jokabidea.setKoadernoa(koadernoa);
        jokabidea.setIrakaslea(irakaslea);
        jokabidea.setData(LocalDate.of(2026, 11, 25));
        jokabidea.setPortaeraArrazoia(arrazoia);
        jokabidea.setNeurriZuzentzailea(neurria);
        jokabidea.setDeskribapenZehatza("Gelan gertatutakoa.");
        jokabidea.setDeskribapenZehatzaEs("Lo ocurrido en el aula.");
        return jokabidea;
    }
}
