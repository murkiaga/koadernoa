package com.koadernoa.app.ethazi;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import com.koadernoa.app.ethazi.controller.EthaziController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import com.koadernoa.app.ethazi.service.EthaziService;
import com.koadernoa.app.ethazi.dto.EthaziForms.*;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.*;
import com.koadernoa.app.objektuak.modulua.entitateak.*;
import com.koadernoa.app.objektuak.zikloak.entitateak.*;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.config.import=", "spring.jpa.show-sql=false"
}, showSql = false)
@Import(EthaziService.class)
class EthaziServiceTest {
    @Autowired EthaziService service;
    @Autowired TestEntityManager em;
    Zikloa cycle;
    Long modelId;

    @BeforeEach void setup() {
        var family = new Familia(); family.setIzena("Informatika"); em.persist(family);
        cycle = new Zikloa(); cycle.setIzena("SMR"); cycle.setFamilia(family); em.persist(cycle);
        var f = new EreduaForm(); f.setZikloaId(cycle.getId()); f.setMota(GaitasunMota.TEKNIKOA); f.setIzena("SMR teknikoa");
        modelId = service.gordeEredua(null, f);
        addLevel("Ezagutu"); addLevel("Erabili");
    }
    void addLevel(String name) { var f = new MailaForm(); f.setIzena(name); service.gordeMaila(modelId, null, f); }
    Long createCompetency() {
        var f = service.gaitasunaForm(null, cycle.getId(), GaitasunMota.TEKNIKOA);
        f.setKodea("G1"); f.setIzena("Sistemak"); f.setDeskribapena("Sistema informatikoak kudeatzea");
        f.getMailak().forEach(m -> m.setDeskribapena("Mailaren deskribapena"));
        return service.gordeGaitasuna(null, f);
    }
    Moduloa module(Zikloa z, String code) {
        var level = new Maila(); level.setKodea(code); em.persist(level);
        var group = new Taldea(); group.setZikloa(z); group.setIzena("1 " + code); em.persist(group);
        var m = new Moduloa(); m.setKodea(code); m.setIzena(code); m.setMaila(level); m.setTaldea(group); return em.persist(m);
    }
    IkaskuntzaEmaitza outcome(Moduloa m) {
        var f = new EmaitzaForm(); f.setZikloaId(m.getTaldea().getZikloa().getId()); f.setModuloaId(m.getId());
        f.setKodea("RA1"); f.setOrdena(1); f.setDeskribapena("Sistemak identifikatzen ditu"); service.gordeEmaitza(null, f);
        return service.emaitzak(f.getZikloaId(), m.getId()).get(0);
    }
    AdierazleaForm indicator(Long... ids) {
        var f = new AdierazleaForm(); f.setOrdena(1); f.setDeskribapena("Osagaiak identifikatzen ditu"); f.setEmaitzaIds(Set.of(ids)); return f;
    }
    @Test void arbitraryLevelCountAndSwapsSurviveDatabaseConstraints() {
        addLevel("Ulertu"); addLevel("Proposatu"); addLevel("Sortu");
        var levels = service.eredua(modelId).getMailak(); Long last = levels.get(4).getId();
        service.mugituMaila(modelId, last, -1); em.flush(); em.clear();
        assertThat(service.eredua(modelId).getMailak().get(3).getId()).isEqualTo(last);
        createCompetency(); em.flush(); em.clear();
        var rubric = service.errubrika(cycle.getId(), GaitasunMota.TEKNIKOA);
        assertThat(rubric.eredua().getMailak()).hasSize(5);
        assertThat(rubric.lerroak().get(0).gelaxkak()).hasSize(5).doesNotContainNull();
    }
    @Test void dependentDeletionPreservesCurriculumAndRemovesJoinRows() {
        Long id = createCompetency();
        var first = outcome(module(cycle, "TRMM")); var second = outcome(module(cycle, "SSER"));
        Long gmId = service.gaitasuna(id).getMailak().get(0).getId();
        service.gordeAdierazlea(id, gmId, null, indicator(first.getId(), second.getId()));
        em.flush(); em.clear();
        var a = service.gaitasuna(id).getMailak().get(0).getLorpenAdierazleak().get(0);
        assertThat(a.getIkaskuntzaEmaitzak()).hasSize(2);
        service.ezabatuAdierazlea(id, gmId, a.getId()); em.flush(); em.clear();
        assertThat(service.emaitza(first.getId())).isNotNull();
        service.gordeAdierazlea(id, gmId, null, indicator(first.getId(), second.getId())); em.flush(); em.clear();
        service.ezabatuGaitasuna(id); em.flush(); em.clear();
        assertThat(em.getEntityManager().createQuery("select count(a) from LorpenAdierazlea a", Long.class).getSingleResult()).isZero();
        assertThat(em.getEntityManager().createNativeQuery("select count(*) from ethazi_lorpen_adierazlea_ikaskuntza_emaitza").getSingleResult().toString()).isEqualTo("0");
        assertThat(service.emaitza(first.getId())).isNotNull(); assertThat(service.emaitza(second.getId())).isNotNull();
    }
    @Test void inUseModelCannotBeDeleted() {
        createCompetency();
        assertThatThrownBy(() -> service.ezabatuEredua(modelId)).hasMessageContaining("erabiltzen");
    }
    @Test void inUseLevelCannotBeDeleted() {
        createCompetency();
        assertThatThrownBy(() -> service.ezabatuMaila(modelId, service.eredua(modelId).getMailak().get(0).getId())).hasMessageContaining("erabiltzen");
    }
    @Test void linkedOutcomeCannotBeDeleted() {
        Long id = createCompetency(); var ie = outcome(module(cycle, "TRMM"));
        service.gordeAdierazlea(id, service.gaitasuna(id).getMailak().get(0).getId(), null, indicator(ie.getId()));
        assertThatThrownBy(() -> service.ezabatuEmaitza(ie.getId())).hasMessageContaining("erabiltzen");
    }
    @Test void rejectsCrossCycleCurriculumLinks() {
        Long id = createCompetency(); var z = new Zikloa(); z.setIzena("DAW"); z.setFamilia(cycle.getFamilia()); em.persist(z);
        var ie = outcome(module(z, "WEB"));
        assertThatThrownBy(() -> service.gordeAdierazlea(id, service.gaitasuna(id).getMailak().get(0).getId(), null, indicator(ie.getId())))
                .hasMessageContaining("ziklokoak");
    }
    @Test void rejectsForeignLevelIdsAndStaleForms() {
        Long id = createCompetency(); var f = service.gaitasunaForm(id, null, null); addLevel("Berria");
        assertThatThrownBy(() -> service.gordeGaitasuna(id, f)).hasMessageContaining("Mailakatzea aldatu");
    }
    @Test void newlyAddedLevelAppearsAsEmptyCellAndIsCreatedOnSave() {
        Long id = createCompetency(); addLevel("Berria"); em.flush(); em.clear();
        assertThat(service.errubrika(cycle.getId(), GaitasunMota.TEKNIKOA).lerroak().get(0).gelaxkak()).hasSize(3).containsNull();
        service.gordeGaitasuna(id, service.gaitasunaForm(id, null, null)); em.flush(); em.clear();
        assertThat(service.errubrika(cycle.getId(), GaitasunMota.TEKNIKOA).lerroak().get(0).gelaxkak()).hasSize(3).doesNotContainNull();
    }
    @Test void rejectsDuplicateModelForSameCycleAndType() {
        var f = new EreduaForm(); f.setZikloaId(cycle.getId()); f.setIzena("Bikoiztua");
        assertThatThrownBy(() -> service.gordeEredua(null, f)).hasMessageContaining("badu mailakatze");
    }
    @Test void technicalAndTransversalModelsAreIndependent() {
        var f = new EreduaForm(); f.setZikloaId(cycle.getId()); f.setMota(GaitasunMota.ZEHARKAKOA); f.setIzena("Zeharkakoa");
        Long other = service.gordeEredua(null, f);
        assertThat(other).isNotEqualTo(modelId);
        assertThat(service.errubrika(cycle.getId(), GaitasunMota.ZEHARKAKOA).eredua().getMailak()).isEmpty();
    }

    @ControllerAdvice
    static class TemplateModel {
        @ModelAttribute("currentPath") String path(HttpServletRequest request) { return request.getRequestURI(); }
        @ModelAttribute("_csrf") DefaultCsrfToken csrf() { return new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token"); }
    }
    MockMvc mvc() {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setTemplateMode("HTML");
        var engine = new SpringTemplateEngine(); engine.setTemplateResolver(resolver); engine.addDialect(new SpringSecurityDialect());
        var views = new ThymeleafViewResolver(); views.setTemplateEngine(engine); views.setCharacterEncoding("UTF-8");
        return MockMvcBuilders.standaloneSetup(new EthaziController(service),
                new org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler()).setControllerAdvice(new TemplateModel())
                .setViewResolvers(views).build();
    }
    @Test void httpFormsBindDynamicLevelsAndCanClearCurriculumLinks() throws Exception {
        var mvc = mvc();
        var request = post("/ethazi/gaitasunak/berria")
                .param("zikloaId", cycle.getId().toString()).param("mota", "TEKNIKOA")
                .param("kodea", "G2").param("izena", "Sareak").param("deskribapena", "Sareak konfiguratu");
        var levels = service.eredua(modelId).getMailak();
        for (int n = 0; n < levels.size(); n++) {
            request.param("mailak[" + n + "].mailaId", levels.get(n).getId().toString());
            request.param("mailak[" + n + "].deskribapena", "HTTP maila " + n);
        }
        mvc.perform(request).andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("success"));
        var g = service.errubrika(cycle.getId(), GaitasunMota.TEKNIKOA).lerroak().get(0).gaitasuna();
        assertThat(g.getMailak()).hasSize(2);
        assertThat(g.getMailak().get(1).getDeskribapena()).isEqualTo("HTTP maila 1");
        var ie = outcome(module(cycle, "TRMM")); var gm = g.getMailak().get(0);
        service.gordeAdierazlea(g.getId(), gm.getId(), null, indicator(ie.getId()));
        Long aId = gm.getLorpenAdierazleak().get(0).getId();
        mvc.perform(post("/ethazi/gaitasunak/" + g.getId() + "/mailak/" + gm.getId() + "/adierazleak/" + aId + "/editatu")
                .param("ordena", "2").param("deskribapena", "Loturak kenduta"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("success"));
        em.flush(); em.clear();
        assertThat(service.gaitasuna(g.getId()).getMailak().get(0).getLorpenAdierazleak().get(0).getIkaskuntzaEmaitzak()).isEmpty();
        assertThat(service.emaitza(ie.getId())).isNotNull();
    }

    @Test void rendersAllScreensWithRealThymeleafAndPreservesInvalidInput() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("manager", "", AuthorityUtils.createAuthorityList("ROLE_KUDEATZAILEA"));
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            addLevel("Ulertu"); addLevel("Proposatu"); addLevel("Sortu");
            Long id = createCompetency(); var m = module(cycle, "TRMM"); var ie = outcome(m);
            Long gmId = service.gaitasuna(id).getMailak().get(0).getId();
            service.gordeAdierazlea(id, gmId, null, indicator(ie.getId()));
            var mvc = mvc();
            for (String path : new String[] {
                "/ethazi/gaitasunak", "/ethazi/gaitasunak?zikloaId=" + cycle.getId(),
                "/ethazi/gaitasunak/berria?zikloaId=" + cycle.getId(), "/ethazi/gaitasunak/" + id + "/editatu",
                "/ethazi/mailakatzeak", "/ethazi/mailakatzeak/berria", "/ethazi/mailakatzeak/" + modelId + "/editatu",
                "/ethazi/ikaskuntza-emaitzak", "/ethazi/ikaskuntza-emaitzak?zikloaId=" + cycle.getId() + "&moduloaId=" + m.getId(),
                "/ethazi/ikaskuntza-emaitzak/berria?zikloaId=" + cycle.getId() + "&moduloaId=" + m.getId(),
                "/ethazi/ikaskuntza-emaitzak/" + ie.getId() + "/editatu"
            }) {
                String html = mvc.perform(get(path).principal(auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
                assertThat(html).contains("ETHAZI", "ethazi-nav", "k-navbar").doesNotContain("th:replace=");
            }
            String rubric = mvc.perform(get("/ethazi/gaitasunak").param("zikloaId", cycle.getId().toString()).principal(auth))
                    .andReturn().getResponse().getContentAsString();
            assertThat(rubric).contains("TRMM · RA1", "Mailaren deskribapena", "Osagaiak identifikatzen ditu");
            String edit = mvc.perform(get("/ethazi/gaitasunak/" + id + "/editatu").principal(auth)).andReturn().getResponse().getContentAsString();
            if (System.getProperty("ethazi.previewDir") != null) {
                var preview = java.nio.file.Path.of(System.getProperty("ethazi.previewDir"));
                java.nio.file.Files.createDirectories(preview);
                java.nio.file.Files.writeString(preview.resolve("rubric.html"), rubric);
                java.nio.file.Files.writeString(preview.resolve("edit.html"), edit);
            }
            assertThat(edit).contains("checked=\"checked\"");
            mvc.perform(post("/ethazi/gaitasunak/" + id + "/mailak/" + gmId + "/adierazleak/berria")
                .principal(auth).param("ordena", "0").param("deskribapena", "Mantendu testu hau"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("error", "failedForm"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mantendu testu hau")));
        } finally { SecurityContextHolder.clearContext(); }
    }
}
