package com.koadernoa.app.ethazi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.koadernoa.app.ethazi.entitateak.EthaziKoadernoa;
import com.koadernoa.app.ethazi.service.EthaziDenboralizazioaService;
import com.koadernoa.app.ethazi.service.EthaziJarraipenaService;
import com.koadernoa.app.ethazi.service.EthaziKoadernoaService;
import com.koadernoa.app.ethazi.service.KronogramaService;
import com.koadernoa.app.funtzionalitateak.irakasle.EthaziKoadernoaController;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
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

class EthaziDenboralizazioaTemplateTest {
    @Test
    void egutegiakFiltroaJarduerakEtaAsistentziaEstekaErrendatzenDitu() throws Exception {
        LocalDate data = LocalDate.of(2026, 11, 2);
        var jarduera = new EthaziDenboralizazioaService.EgunekoJarduera(
                50L, 30L, "Web erronka", "APIa diseinatu", "Xehetasuna", "PROG", false,
                data, BigDecimal.ONE, true, true, "Backend oharra");
        var komuna = new EthaziDenboralizazioaService.EgunekoJarduera(
                51L, 30L, "Web erronka", "Aurkezpena", "", "Orokorra", true,
                data, BigDecimal.valueOf(2), true, false, null);
        var erronkaBarra = new EthaziDenboralizazioaService.ErronkaBarra(
                30L, 1, "Abiadura-kaxa", data, data.plusDays(18), "#c7d2fe",
                true, false, true,
                "1. erronka · Abiadura-kaxa\nHasiera: 2026-11-02\nAmaiera: 2026-11-20");
        var eguna = new EthaziDenboralizazioaService.Eguna(data, true, "lektibo1",
                List.of(jarduera, komuna),
                List.of(new EthaziDenboralizazioaService.AsistentziaAukera(10L, "PROG", true)),
                List.of(erronkaBarra));
        var gaurkoEguna = new EthaziDenboralizazioaService.Eguna(LocalDate.now(), true, "lektibo1",
                List.of(), List.of(), List.of());
        var ikuspegia = new EthaziDenboralizazioaService.DenboralizazioaIkuspegia(
                "2026-2027", "azaroa 2026", 2026, 11, null,
                List.of(new EthaziKoadernoaService.ModuluAukera(10L, "PROG")),
                List.of(List.of(eguna, gaurkoEguna, eguna, eguna, eguna)));

        Taldea taldea = new Taldea(); taldea.setId(4L); taldea.setIzena("1DAM");
        EthaziKoadernoa koadernoa = new EthaziKoadernoa(); koadernoa.setId(6L); koadernoa.setTaldea(taldea);
        Maila maila = new Maila(); maila.setId(2L);
        Moduloa modulua = new Moduloa(); modulua.setId(10L); modulua.setMaila(maila); modulua.setAktibo(true);
        Egutegia egutegia = new Egutegia(); egutegia.setId(8L);
        EgunBerezi egunBerezi = new EgunBerezi();
        egunBerezi.setData(data); egunBerezi.setDeskribapena("Ebaluazio bilera");
        egutegia.setEgunBereziak(List.of(egunBerezi));
        Irakaslea irakaslea = new Irakaslea(); irakaslea.setId(40L);

        EthaziKoadernoaService koadernoService = mock(EthaziKoadernoaService.class);
        IrakasleaService irakasleaService = mock(IrakasleaService.class);
        ModuloaRepository moduluak = mock(ModuloaRepository.class);
        EgutegiaRepository egutegiak = mock(EgutegiaRepository.class);
        EthaziDenboralizazioaService denboralizazioaService = mock(EthaziDenboralizazioaService.class);
        when(koadernoService.findById(6L)).thenReturn(koadernoa);
        when(irakasleaService.getLogeatutaDagoenIrakaslea(org.mockito.ArgumentMatchers.any())).thenReturn(irakaslea);
        when(koadernoService.irakasleakBadaukaSarbidea(irakaslea, koadernoa)).thenReturn(true);
        when(moduluak.findByTaldeaId(4L)).thenReturn(List.of(modulua));
        when(egutegiak.findByIkasturtea_AktiboaTrueAndMaila_Id(2L)).thenReturn(Optional.of(egutegia));
        when(denboralizazioaService.prestatu(koadernoa, egutegia, irakaslea, null, 2026, 11))
                .thenReturn(ikuspegia);
        var controller = new EthaziKoadernoaController(koadernoService, irakasleaService,
                mock(IkasleaRepository.class), moduluak, egutegiak, mock(EgutegiaService.class),
                mock(KronogramaService.class), mock(EthaziJarraipenaService.class), denboralizazioaService);

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
                            .param("urtea", "2026").param("hilabetea", "11")
                            .session(session).principal(auth))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(html).contains("Denboralizazioa", "Nire moduluak", "APIa diseinatu",
                    "Aurkezpena", "Orokorra", "Web erronka", "1 h", "2 h", "👤✔",
                    "moduloaId=10", "draggable=\"true\"", "id=\"jardueraDialog\"", "📝",
                    "Egutegi bista", "Falten bista", "Hilabete bista", "Asteko bista",
                    "lektibo1", "egutegi-oharra", "Ebaluazio bilera",
                    "class=\"day-number gaur-zenbakia\"", "challenge-bands",
                    "challenge-band  start", "1. erronka · Abiadura-kaxa",
                    "Hasiera: 2026-11-02", "Amaiera: 2026-11-20", "background:#c7d2fe");
            assertThat(html).doesNotContain("filtroModuloaId=10");

            String astekoHtml = mvc.perform(get("/irakasle/ethazi-koadernoa/denboralizazioa")
                            .param("urtea", "2026").param("hilabetea", "11")
                            .param("egutegiMota", "astea").param("asteHasiera", "2026-11-02")
                            .session(session).principal(auth))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(astekoHtml).contains("week-mode", "Asteko bista",
                    "asteHasiera=2026-11-02", "asteHasiera=2026-10-26", "asteHasiera=2026-11-09",
                    "<div class=\"activity-description\">Xehetasuna</div>");
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
