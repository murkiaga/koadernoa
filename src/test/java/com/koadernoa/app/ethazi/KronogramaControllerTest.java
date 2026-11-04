package com.koadernoa.app.ethazi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
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

import com.koadernoa.app.ethazi.controller.KronogramaController;
import com.koadernoa.app.ethazi.entitateak.Erronka;
import com.koadernoa.app.ethazi.entitateak.kronograma.ErronkaKronogramaJarduera;
import com.koadernoa.app.ethazi.entitateak.kronograma.KronogramaJardueraMota;
import com.koadernoa.app.ethazi.service.KronogramaService;
import com.koadernoa.app.ethazi.service.KronogramaService.KronogramaAstea;
import com.koadernoa.app.ethazi.service.KronogramaService.KronogramaBlokea;
import com.koadernoa.app.ethazi.service.KronogramaService.KronogramaIkuspegia;
import com.koadernoa.app.ethazi.service.KronogramaService.KronogramaLerroa;
import com.koadernoa.app.ethazi.service.KronogramaService.ModuloOrduLaburpena;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;

import jakarta.servlet.http.HttpServletRequest;

class KronogramaControllerTest {
    @Test
    void kronogramaTxantiloiaDatuIraunkorrekinErrendatzenDa() throws Exception {
        KronogramaService service = mock(KronogramaService.class);
        var auth = new TestingAuthenticationToken("manager", null, "ROLE_KUDEATZAILEA");
        when(service.ikuspegia(7L, auth)).thenReturn(view());
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            String html = mvc(service).perform(get("/ethazi/erronkak/7/kronograma")
                    .principal(auth).param("zikloaId", "1").param("ikasturteaId", "4"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(html).contains("Erronkaren kronograma", "Sareko zerbitzaria", "1. astea",
                "Sarearen diseinua", "Aurkezpena", "(SARE)", "Moduloa",
                "4 ordu astean", "20 ordu erronkan", "data-activity-id=\"44\"", "is-no-class",
                "data-kronograma-cell=\"true\"", "kronograma.js")
                .doesNotContain("name=\"bukaeraData\"", "Aste honetan jarduerarik ez", "SAT", "SUN");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private KronogramaIkuspegia view() {
        var moduloa = new Moduloa(); moduloa.setId(10L); moduloa.setKodea("SARE"); moduloa.setIzena("Sare lokalak");
        var erronka = new Erronka(); erronka.setId(7L); erronka.setIzena("Sareko zerbitzaria");
        erronka.setHasieraData(LocalDate.of(2026, 10, 5)); erronka.setBukaeraData(LocalDate.of(2026, 10, 9));
        erronka.setModuluak(new LinkedHashSet<>(List.of(moduloa)));
        var activity = new ErronkaKronogramaJarduera(); activity.setId(44L); activity.setErronka(erronka);
        activity.setMota(KronogramaJardueraMota.MODULUA); activity.setModuloa(moduloa);
        activity.setIzenburua("Sarearen diseinua"); activity.setHasieraData(LocalDate.of(2026, 10, 7));
        activity.setBukaeraData(LocalDate.of(2026, 10, 8)); activity.setIraupenOrduak(new BigDecimal("2.00"));
        var common = new ErronkaKronogramaJarduera(); common.setId(45L); common.setErronka(erronka);
        common.setMota(KronogramaJardueraMota.KOMUNA); common.setAurreikusitakoModuloa(moduloa);
        common.setIzenburua("Aurkezpena"); common.setHasieraData(LocalDate.of(2026, 10, 5));
        common.setBukaeraData(LocalDate.of(2026, 10, 5)); common.setIraupenOrduak(new BigDecimal("1.00"));
        var commonRow = new KronogramaLerroa(null, "Erronka", "Jarduera komunak", null, true, 0, 1,
            true, Set.copyOf(days()), List.of());
        var moduleRow = new KronogramaLerroa(10L, "SARE", "Ane", new ModuloOrduLaburpena(4, 20), false, 1, 1,
            true, Set.of(LocalDate.of(2026, 10, 7)),
            List.of(new KronogramaBlokea(common, 3, 1, 1, true, new BigDecimal("1.00")),
                    new KronogramaBlokea(activity, 3, 1, 2, true, new BigDecimal("2.00"))));
        var days = days();
        var week = new KronogramaAstea(1, days.get(0), days.get(4), days, List.of(commonRow, moduleRow));
        return new KronogramaIkuspegia(erronka, List.of(moduloa), List.of(common, activity), List.of(week),
            true, Set.of(10L), 5);
    }

    private List<LocalDate> days() {
        return LocalDate.of(2026, 10, 5).datesUntil(LocalDate.of(2026, 10, 10)).toList();
    }

    private org.springframework.test.web.servlet.MockMvc mvc(KronogramaService service) {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setTemplateMode("HTML");
        var engine = new SpringTemplateEngine(); engine.setTemplateResolver(resolver); engine.addDialect(new SpringSecurityDialect());
        var views = new ThymeleafViewResolver(); views.setTemplateEngine(engine); views.setCharacterEncoding("UTF-8");
        return MockMvcBuilders.standaloneSetup(new KronogramaController(service),
                new org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler())
            .setControllerAdvice(new TemplateModel()).setViewResolvers(views).build();
    }

    @ControllerAdvice
    static class TemplateModel {
        @ModelAttribute("currentPath") String path(HttpServletRequest request) { return request.getRequestURI(); }
        @ModelAttribute("_csrf") DefaultCsrfToken csrf() {
            return new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token");
        }
    }
}
