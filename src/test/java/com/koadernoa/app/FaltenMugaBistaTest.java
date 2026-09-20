package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import com.koadernoa.app.objektuak.koadernoak.entitateak.denboralizazioa.FaltaIkasleRow;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;

class FaltenMugaBistaTest {
    @ParameterizedTest
    @CsvSource({"20,20,false,true", "20,20.01,true,false", "25,21,false,true",
            "25,25,false,true", "25,25.01,true,false", "25,9.99,false,false", "25,10,false,true",
            "0,0,false,false", "0,0.01,true,false", "100,100,false,true"})
    void estiloaEtaPdfIkonoaMugaGainditzeanBakarrik(int muga, double pct, boolean altua, boolean ertaina) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        MockServletContext servlet = new MockServletContext();
        var exchange = JakartaServletWebApplication.buildApplication(servlet).buildExchange(
                new MockHttpServletRequest(servlet), new MockHttpServletResponse());
        WebContext context = new WebContext(exchange, Locale.ROOT);
        FaltaIkasleRow row = new FaltaIkasleRow();
        Matrikula matrikula = new Matrikula();
        matrikula.setId(1L);
        row.setMatrikula(matrikula);
        row.setFaltaPortzentaia(pct);
        context.setVariable("faltakIkasleak", List.of(row));
        context.setVariable("faltaEgunak", List.of());
        context.setVariable("faltenMugaPortzentaia", muga);
        context.setVariable("hilabetea", 9);
        context.setVariable("urtea", 2026);
        String html = engine.process("irakasleak/denboralizazioa/falten-bista-fragment", context);
        assertThat(html.contains("falta-pct-altua")).isEqualTo(altua);
        assertThat(html.contains("falta-pct-ertaina")).isEqualTo(ertaina);
        assertThat(html.contains("📄")).isEqualTo(altua);
    }
}
