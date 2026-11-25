package com.koadernoa.app.ethazi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.koadernoa.app.ethazi.entitateak.EthaziKoadernoa;
import com.koadernoa.app.ethazi.service.EthaziJarraipenaService;
import com.koadernoa.app.ethazi.service.EthaziDenboralizazioaService;
import com.koadernoa.app.ethazi.service.EthaziKoadernoaService;
import com.koadernoa.app.ethazi.service.KronogramaService;
import com.koadernoa.app.funtzionalitateak.irakasle.EthaziKoadernoaController;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.egutegia.repository.EgutegiaRepository;
import com.koadernoa.app.objektuak.egutegia.service.EgutegiaService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.repository.IkasleaRepository;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

import jakarta.servlet.http.HttpServletRequest;

class EthaziJarraipenaTemplateTest {
    @Test
    void jarraipenTaulaErronkaEgunTaldeEtaEgoerarekinErrendatzenDa() throws Exception {
        LocalDate data = LocalDate.of(2026, 11, 2);
        var erronka = new EthaziJarraipenaService.ErronkaZutabeMultzoa(
                20L, 1, "Web aplikazioa", data, data, true, List.of(
                        new EthaziJarraipenaService.EgunZutabea(data, 2)), 2);
        var laburpena = new EthaziJarraipenaService.ErronkaIkasleLaburpena(
                new BigDecimal("0.50"), 25d, true, "Modulua suspendituta");
        var gelaxka = new EthaziJarraipenaService.JarraipenGelaxka(
                new BigDecimal("0.50"), true);
        var ikaslea = new EthaziJarraipenaService.IkasleLerroa(
                30L, "Alberdi, Ane", "1. taldea", true, 1,
                Map.of(20L, laburpena), Map.of("20|2026-11-02", gelaxka));
        var ikuspegia = new EthaziJarraipenaService.JarraipenaIkuspegia(
                "2026-2027", List.of(new EthaziJarraipenaService.ModuluAukera(10L, "PROG")),
                10L, "PROG", 20, List.of(erronka), List.of(ikaslea));
        Taldea taldea = new Taldea(); taldea.setId(4L); taldea.setIzena("1DAM");
        EthaziKoadernoa koadernoa = new EthaziKoadernoa(); koadernoa.setId(6L); koadernoa.setTaldea(taldea);
        Maila maila = new Maila(); maila.setId(2L);
        Moduloa modulua = new Moduloa(); modulua.setId(10L); modulua.setMaila(maila); modulua.setAktibo(true);
        Egutegia egutegia = new Egutegia(); egutegia.setId(8L);
        Irakaslea irakaslea = new Irakaslea(); irakaslea.setId(40L);

        EthaziKoadernoaService koadernoService = mock(EthaziKoadernoaService.class);
        IrakasleaService irakasleaService = mock(IrakasleaService.class);
        IkasleaRepository ikasleak = mock(IkasleaRepository.class);
        ModuloaRepository moduluak = mock(ModuloaRepository.class);
        EgutegiaRepository egutegiak = mock(EgutegiaRepository.class);
        EgutegiaService egutegiaService = mock(EgutegiaService.class);
        KronogramaService kronogramaService = mock(KronogramaService.class);
        EthaziJarraipenaService jarraipenaService = mock(EthaziJarraipenaService.class);
        EthaziDenboralizazioaService denboralizazioaService = mock(EthaziDenboralizazioaService.class);
        when(koadernoService.findById(6L)).thenReturn(koadernoa);
        when(irakasleaService.getLogeatutaDagoenIrakaslea(org.mockito.ArgumentMatchers.any()))
                .thenReturn(irakaslea);
        when(koadernoService.irakasleakBadaukaSarbidea(irakaslea, koadernoa)).thenReturn(true);
        when(moduluak.findByTaldeaId(4L)).thenReturn(List.of(modulua));
        when(egutegiak.findByIkasturtea_AktiboaTrueAndMaila_Id(2L)).thenReturn(Optional.of(egutegia));
        when(jarraipenaService.prestatu(koadernoa, egutegia, irakaslea, false, 10L)).thenReturn(ikuspegia);
        when(denboralizazioaService.prestatu(koadernoa, egutegia, irakaslea, 10L, 2026, 11))
                .thenReturn(new EthaziDenboralizazioaService.DenboralizazioaIkuspegia(
                        "2026-2027", "azaroa 2026", 2026, 11, 10L, List.of(), List.of()));
        var controller = new EthaziKoadernoaController(koadernoService, irakasleaService, ikasleak,
                moduluak, egutegiak, egutegiaService, kronogramaService, jarraipenaService,
                denboralizazioaService);

        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setTemplateMode("HTML");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver); engine.addDialect(new SpringSecurityDialect());
        var views = new ThymeleafViewResolver(); views.setTemplateEngine(engine); views.setCharacterEncoding("UTF-8");
        var mvc = MockMvcBuilders.standaloneSetup(controller,
                new org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler())
                .setControllerAdvice(new TemplateModel(koadernoa)).setViewResolvers(views).build();
        var auth = new TestingAuthenticationToken("teacher", null, "ROLE_IRAKASLEA");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("ethaziKoadernoAktiboa", koadernoa);
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            String html = mvc.perform(get("/irakasle/ethazi-koadernoa/denboralizazioa")
                            .param("bista", "faltak").param("urtea", "2026")
                            .param("hilabetea", "11").param("moduloaId", "10")
                            .session(session).principal(auth))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(html).contains("Egutegi bista", "Falten bista", "Kontsulta soilik",
                    "Web aplikazioa", "Alberdi, Ane",
                    "1. taldea", "25.00%", "Modulua suspendituta", "2026/11/02", "📝");
            assertThat(html).contains("data-erronka-toggle=\"20\"", "aria-expanded=\"false\"",
                    "erronka-xehetasuna-ezkututa", ">+</span>", "class=\"talde-hasiera\"",
                    "id=\"jarraipenScrollKontrola\"", "id=\"jarraipenScrollBarra\"");
            assertThat(html).doesNotContain("jarraipenDialog", "irekiJarraipenGelaxka", ">Gorde<");
            mvc.perform(post("/irakasle/ethazi-koadernoa/jarraipena/gelaxka")
                            .contentType(MediaType.APPLICATION_JSON).content("{}")
                            .session(session).principal(auth))
                    .andExpect(status().isForbidden());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @ControllerAdvice
    static class TemplateModel {
        private final EthaziKoadernoa koadernoa;
        TemplateModel(EthaziKoadernoa koadernoa) { this.koadernoa = koadernoa; }
        @ModelAttribute("currentPath") String path(HttpServletRequest request) { return request.getRequestURI(); }
        @ModelAttribute("_csrf") DefaultCsrfToken csrf() {
            return new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token");
        }
        @ModelAttribute("irakasleKoadernoAktiboak") List<Object> koadernoak() { return List.of(); }
        @ModelAttribute("irakasleEthaziKoadernoAktiboak") List<EthaziKoadernoa> ethaziKoadernoak() {
            return List.of(koadernoa);
        }
    }
}
