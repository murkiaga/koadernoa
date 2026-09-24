package com.koadernoa.app.ethazi;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;
import java.util.List;
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
@Import({EthaziService.class, com.koadernoa.app.ethazi.service.KinielaService.class})
class EthaziServiceTest {
    @Autowired EthaziService service;
    @Autowired com.koadernoa.app.ethazi.service.KinielaService kiniela;
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
        f.setKodea("G1"); f.setDeskribapena("Sistema informatikoak kudeatzea");
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
        var f = new AdierazleaForm(); f.setDeskribapena("Osagaiak identifikatzen ditu"); f.setEmaitzaIds(Set.of(ids)); return f;
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
    @Test void rubricCanDeleteEmptyLevelEvenWithMaterializedCells() throws Exception {
        Long competencyId = createCompetency();
        Long levelId = service.gehituErrubrikaMaila(cycle.getId(), GaitasunMota.TEKNIKOA);
        var form = service.gaitasunaForm(competencyId, null, null);
        form.getMailak().stream().filter(m -> m.getMailaId().equals(levelId)).forEach(m -> m.setDeskribapena("  \n "));
        service.gordeGaitasuna(competencyId, form);
        em.flush(); em.clear();
        mvc().perform(post("/ethazi/errubrika/mailak/" + levelId + "/ezabatu")
                .param("zikloaId", cycle.getId().toString()).param("mota", "TEKNIKOA"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("success"))
                .andExpect(redirectedUrl("/ethazi/gaitasunak?zikloaId=" + cycle.getId() + "&mota=TEKNIKOA"));
        em.flush(); em.clear();
        assertThat(em.find(MailakatzeMaila.class, levelId)).isNull();
        assertThat(service.gaitasuna(competencyId).getMailak()).hasSize(2);
    }

    @Test void levelDeletionListsAllCompetenciesWithDescriptionsAndPreservesContent() throws Exception {
        createCompetency();
        var second = service.gaitasunaForm(null, cycle.getId(), GaitasunMota.TEKNIKOA);
        second.setKodea("G2"); second.setDeskribapena("Sareak konfiguratu");
        second.getMailak().forEach(m -> m.setDeskribapena("Sareko maila"));
        service.gordeGaitasuna(null, second);
        Long levelId = service.eredua(modelId).getMailak().get(0).getId();
        mvc().perform(post("/ethazi/errubrika/mailak/" + levelId + "/ezabatu")
                .param("zikloaId", cycle.getId().toString()).param("mota", "TEKNIKOA"))
                .andExpect(flash().attribute("error", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("G1 — Sistema informatikoak kudeatzea"),
                        org.hamcrest.Matchers.containsString("G2 — Sareak konfiguratu"))));
        assertThat(service.eredua(modelId).getMailak()).hasSize(2);
    }

    @Test void levelWithIndicatorsCannotBeDeletedEvenWithoutDescription() {
        Long competencyId = createCompetency();
        Long levelId = service.gehituErrubrikaMaila(cycle.getId(), GaitasunMota.TEKNIKOA);
        service.gordeErrubrikaAdierazlea(competencyId, levelId, null, indicator());
        assertThatThrownBy(() -> service.ezabatuErrubrikaMaila(cycle.getId(), GaitasunMota.TEKNIKOA, levelId))
                .hasMessageContaining("G1").hasMessageContaining("lorpen-adierazleak");
        assertThat(service.gaitasuna(competencyId).getMailak().get(2).getLorpenAdierazleak()).hasSize(1);
    }

    @Test void levelDeletionRejectsAnotherRubric() {
        Long foreignId = service.gehituErrubrikaMaila(cycle.getId(), GaitasunMota.ZEHARKAKOA);
        assertThatThrownBy(() -> service.ezabatuErrubrikaMaila(cycle.getId(), GaitasunMota.TEKNIKOA, foreignId))
                .hasMessageContaining("Maila ez da eredu honetakoa");
        assertThat(service.eredua(cycle.getId(), GaitasunMota.ZEHARKAKOA).getMailak()).hasSize(1);
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

    @Test void rubricPlusBootstrapsModelAndAppendsUnlimitedColumns() {
        Long first = service.gehituErrubrikaMaila(cycle.getId(), GaitasunMota.ZEHARKAKOA);
        Long second = service.gehituErrubrikaMaila(cycle.getId(), GaitasunMota.ZEHARKAKOA);
        em.flush(); em.clear();
        var model = service.eredua(cycle.getId(), GaitasunMota.ZEHARKAKOA);
        assertThat(model.getMailak()).extracting(MailakatzeMaila::getId).containsExactly(first, second);
        assertThat(model.getMailak()).extracting(MailakatzeMaila::getOrdena).containsExactly(1, 2);
        var name = new MailaForm(); name.setIzena("Elkarlanean aritu");
        service.izendatuErrubrikaMaila(cycle.getId(), GaitasunMota.ZEHARKAKOA, second, name);
        em.flush(); em.clear();
        assertThat(service.eredua(model.getId()).getMailak().get(1).getIzena()).isEqualTo("Elkarlanean aritu");
    }

    @Test void inlineCreationMaterializesNewCellAndAlwaysAppends() {
        Long id = createCompetency();
        Long level = service.gehituErrubrikaMaila(cycle.getId(), GaitasunMota.TEKNIKOA);
        service.gordeErrubrikaAdierazlea(id, level, null, indicator());
        service.gordeErrubrikaAdierazlea(id, level, null, indicator());
        em.flush(); em.clear();
        var cell = service.gaitasuna(id).getMailak().get(2);
        assertThat(cell.getLorpenAdierazleak()).extracting(LorpenAdierazlea::getOrdena).containsExactly(1, 2);
        Long first = cell.getLorpenAdierazleak().get(0).getId();
        service.gordeErrubrikaAdierazlea(id, level, first, indicator());
        service.ezabatuErrubrikaAdierazlea(id, level, first);
        em.flush(); em.clear();
        assertThat(service.gaitasuna(id).getMailak().get(2).getLorpenAdierazleak())
                .extracting(LorpenAdierazlea::getOrdena).containsExactly(1);
    }

    @Test void dragOrderPersistsConsecutivePositionsWithoutChangingCellOrLinks() {
        Long id = createCompetency(); Long level = service.eredua(modelId).getMailak().get(0).getId();
        var ie = outcome(module(cycle, "TRMM"));
        for (int i = 0; i < 3; i++) service.gordeErrubrikaAdierazlea(id, level, null, indicator(ie.getId()));
        var before = service.gaitasuna(id).getMailak().get(0).getLorpenAdierazleak();
        var order = List.of(before.get(2).getId(), before.get(0).getId(), before.get(1).getId());
        Long cellId = before.get(0).getGaitasunMaila().getId();
        service.berrordenatuAdierazleak(id, level, order); em.flush(); em.clear();
        var after = service.gaitasuna(id).getMailak().get(0).getLorpenAdierazleak();
        assertThat(after).extracting(LorpenAdierazlea::getId).containsExactlyElementsOf(order);
        assertThat(after).extracting(LorpenAdierazlea::getOrdena).containsExactly(1, 2, 3);
        assertThat(after).allSatisfy(a -> {
            assertThat(a.getGaitasunMaila().getId()).isEqualTo(cellId);
            assertThat(a.getIkaskuntzaEmaitzak()).extracting(IkaskuntzaEmaitza::getId).containsExactly(ie.getId());
        });
    }

    @Test void dragRejectsAnIndicatorFromAnotherCell() {
        Long id = createCompetency(); var levels = service.eredua(modelId).getMailak();
        for (var level : levels) service.gordeErrubrikaAdierazlea(id, level.getId(), null, indicator());
        Long foreign = service.gaitasuna(id).getMailak().get(1).getLorpenAdierazleak().get(0).getId();
        assertThatThrownBy(() -> service.berrordenatuAdierazleak(id, levels.get(0).getId(), List.of(foreign)))
                .hasMessageContaining("Ordena ez da baliozkoa");
    }

    @Test void dragRejectsMissingOrDuplicateIds() {
        Long id = createCompetency(); Long level = service.eredua(modelId).getMailak().get(0).getId();
        service.gordeErrubrikaAdierazlea(id, level, null, indicator());
        service.gordeErrubrikaAdierazlea(id, level, null, indicator());
        Long a = service.gaitasuna(id).getMailak().get(0).getLorpenAdierazleak().get(0).getId();
        assertThatThrownBy(() -> service.berrordenatuAdierazleak(id, level, List.of(a, a))).hasMessageContaining("Ordena ez da baliozkoa");
        assertThatThrownBy(() -> service.berrordenatuAdierazleak(id, level, List.of(a))).hasMessageContaining("Ordena ez da baliozkoa");
    }

    @Test void inlineCreationRejectsALevelFromAnotherModel() {
        Long id = createCompetency(); Long foreign = service.gehituErrubrikaMaila(cycle.getId(), GaitasunMota.ZEHARKAKOA);
        assertThatThrownBy(() -> service.gordeErrubrikaAdierazlea(id, foreign, null, indicator()))
                .hasMessageContaining("Maila ez da eredu honetakoa");
    }

    @Test void kinielaLinksAndDecimalWeightsSurviveReloadAndClearFromBothScreens() {
        var m = module(cycle, "KIN"); var ie = outcome(m); Long g = createCompetency();
        Long level = service.eredua(modelId).getMailak().get(0).getId();
        service.gordeErrubrikaAdierazlea(g, level, null, indicator());
        Long a = service.gaitasuna(g).getMailak().get(0).getLorpenAdierazleak().get(0).getId();
        kiniela.gordeLoturak(cycle.getId(), ie.getId(), Set.of(a));
        kiniela.gordePisua(cycle.getId(), ie.getId(), a, new java.math.BigDecimal("12.35"));
        em.flush(); em.clear();
        assertThat(kiniela.kiniela(cycle.getId()).get(0).guztira()).isEqualByComparingTo("12.35");
        assertThat(kiniela.kiniela(cycle.getId()).get(0).emaitzak().get(0).guztira()).isEqualByComparingTo("12.35");
        service.gordeErrubrikaAdierazlea(g, level, a, indicator()); em.flush(); em.clear();
        assertThat(kiniela.kiniela(cycle.getId()).get(0).emaitzak().get(0).loturak()).isEmpty();
        kiniela.gordeLoturak(cycle.getId(), ie.getId(), Set.of(a));
        assertThat(kiniela.kiniela(cycle.getId()).get(0).guztira()).isEqualByComparingTo("0");
        kiniela.gordeLoturak(cycle.getId(), ie.getId(), Set.of()); em.flush(); em.clear();
        assertThat(service.gaitasuna(g).getMailak().get(0).getLorpenAdierazleak().get(0).getIkaskuntzaEmaitzak()).isEmpty();
    }
    @Test void weightsArePerOutcomeAndTotalsSumAcrossTheModule() {
        var m = module(cycle, "SUM"); var ie = outcome(m); Long g = createCompetency();
        var next = new EmaitzaForm(); next.setZikloaId(cycle.getId()); next.setModuloaId(m.getId());
        next.setKodea("IE2"); next.setOrdena(2); next.setDeskribapena("Sareak konfiguratu"); service.gordeEmaitza(null, next);
        var ie2 = service.emaitzak(cycle.getId(), m.getId()).get(1);
        Long level = service.eredua(modelId).getMailak().get(0).getId();
        service.gordeErrubrikaAdierazlea(g, level, null, indicator(ie.getId(), ie2.getId()));
        Long a = service.gaitasuna(g).getMailak().get(0).getLorpenAdierazleak().get(0).getId();
        kiniela.gordePisua(cycle.getId(), ie.getId(), a, new java.math.BigDecimal("35.25"));
        kiniela.gordePisua(cycle.getId(), ie2.getId(), a, new java.math.BigDecimal("64.75"));
        em.flush(); em.clear();
        var result = kiniela.kiniela(cycle.getId()).get(0);
        assertThat(result.guztira()).isEqualByComparingTo("100");
        assertThat(result.emaitzak().get(0).guztira()).isEqualByComparingTo("35.25");
        assertThat(result.emaitzak().get(1).guztira()).isEqualByComparingTo("64.75");
    }
    @Test void kinielaRejectsForeignLinksAndInvalidWeights() {
        var ie = outcome(module(cycle, "KIN")); Long g = createCompetency();
        Long level = service.eredua(modelId).getMailak().get(0).getId();
        service.gordeErrubrikaAdierazlea(g, level, null, indicator());
        Long a = service.gaitasuna(g).getMailak().get(0).getLorpenAdierazleak().get(0).getId();
        assertThatThrownBy(() -> kiniela.gordeLoturak(cycle.getId(), ie.getId(), Set.of(-1L))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> kiniela.gordeLoturak(-1L, ie.getId(), Set.of(a))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> kiniela.gordePisua(cycle.getId(), ie.getId(), a, java.math.BigDecimal.ONE)).isInstanceOf(IllegalArgumentException.class);
        kiniela.gordeLoturak(cycle.getId(), ie.getId(), Set.of(a));
        for (String invalid : List.of("-1", "100.01", "0.001"))
            assertThatThrownBy(() -> kiniela.gordePisua(cycle.getId(), ie.getId(), a, new java.math.BigDecimal(invalid))).isInstanceOf(IllegalArgumentException.class);
    }
    ErronkaForm challenge(Moduloa m) {
        var f = new ErronkaForm(); f.setZikloaId(cycle.getId()); f.setMailaId(m.getMaila().getId());
        f.setIzena("Sarearen erronka"); f.setDeskribapena("Sarea diseinatu"); f.setHizkuntza(Hizkuntza.EUSKARA);
        f.setHasieraData(java.time.LocalDate.of(2026,9,1)); f.setBukaeraData(java.time.LocalDate.of(2026,10,1));
        f.setModuloIds(Set.of(m.getId())); return f;
    }
    @Test void legacyGazteleraValuesRemainReadableAndNewValuesUseTheNewName() {
        var m = module(cycle, "GAZ");
        var f = challenge(m); f.setHizkuntza(Hizkuntza.GAZTELERA);
        kiniela.gordeErronka(null, f);
        Long challengeId = kiniela.erronkak(cycle.getId()).get(0).getId();
        em.flush();
        em.getEntityManager().createNativeQuery("update moduloa set hizkuntza = 'ERDERA' where id = :id")
                .setParameter("id", m.getId()).executeUpdate();
        em.getEntityManager().createNativeQuery("update ethazi_erronka set hizkuntza = 'ERDERA' where id = :id")
                .setParameter("id", challengeId).executeUpdate();
        em.clear();
        assertThat(em.find(Moduloa.class, m.getId()).getHizkuntza()).isEqualTo(Hizkuntza.GAZTELERA);
        assertThat(kiniela.erronka(challengeId).getHizkuntza()).isEqualTo(Hizkuntza.GAZTELERA);
        var fresh = module(cycle, "GAZ2"); fresh.setHizkuntza(Hizkuntza.GAZTELERA); em.flush();
        assertThat(em.getEntityManager().createNativeQuery("select hizkuntza from moduloa where id = :id")
                .setParameter("id", fresh.getId()).getSingleResult()).isEqualTo("GAZTELERA");
    }

    @Test void challengesValidateLanguageLevelCycleDatesAndActiveLevels() {
        var m = module(cycle, "ERR"); var f = challenge(m);
        kiniela.gordeErronka(null, f); em.flush();
        assertThat(kiniela.erronkak(cycle.getId())).hasSize(1);
        m.setHizkuntza(Hizkuntza.GAZTELERA);
        assertThatThrownBy(() -> kiniela.gordeErronka(null, f)).hasMessageContaining("hizkuntza");
        f.setHizkuntza(Hizkuntza.ZEHAZTU_GABE); kiniela.gordeErronka(null, f);
        f.setBukaeraData(f.getHasieraData().minusDays(1));
        assertThatThrownBy(() -> kiniela.gordeErronka(null, f)).hasMessageContaining("Bukaera");
        f.setBukaeraData(f.getHasieraData()); m.getMaila().setAktibo(false);
        assertThatThrownBy(() -> kiniela.gordeErronka(null, f)).hasMessageContaining("aktibo");
        m.getMaila().setAktibo(true);
        var other = module(cycle, "OTHER"); f.setModuloIds(Set.of(other.getId()));
        assertThatThrownBy(() -> kiniela.gordeErronka(null, f)).hasMessageContaining("maila");
        f.setModuloIds(Set.of(-1L));
        assertThatThrownBy(() -> kiniela.gordeErronka(null, f)).hasMessageContaining("baliozko");
    }
    @Test void oldKinielaUrlRedirectsAndPreservesCycle() throws Exception {
        mvc().perform(get("/ethazi/kinielak").param("zikloaId", cycle.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ethazi/kiniela?zikloaId=" + cycle.getId()));
        mvc().perform(get("/ethazi/kinielak"))
                .andExpect(redirectedUrl("/ethazi/kiniela"));
    }

    @Test void rendersKinielaAndChallengeScreensAndBindsSelections() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("manager", "", AuthorityUtils.createAuthorityList("ROLE_KUDEATZAILEA"));
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            var m = module(cycle, "KIN"); var ie = outcome(m); Long g = createCompetency();
            Long level = service.eredua(modelId).getMailak().get(0).getId();
            service.gordeErrubrikaAdierazlea(g, level, null, indicator(ie.getId()));
            var second = new IkaskuntzaEmaitza();
            second.setModuloa(m); second.setKodea("IE2"); second.setOrdena(2);
            second.setDeskribapena("Sarea konfiguratzen du"); em.persist(second);
            Long indicatorId = service.gaitasuna(g).getMailak().get(0).getLorpenAdierazleak().get(0).getId();
            kiniela.gordeLoturak(cycle.getId(), second.getId(), Set.of(indicatorId));
            kiniela.gordePisua(cycle.getId(), ie.getId(), indicatorId, new java.math.BigDecimal("40"));
            kiniela.gordePisua(cycle.getId(), second.getId(), indicatorId, new java.math.BigDecimal("60"));
            kiniela.gordeErronka(null, challenge(m)); Long e = kiniela.erronkak(cycle.getId()).get(0).getId();
            var mvc = mvc();
            for (String path : List.of("/ethazi/kiniela", "/ethazi/kiniela?zikloaId=" + cycle.getId(),
                "/ethazi/erronkak", "/ethazi/erronkak?zikloaId=" + cycle.getId(), "/ethazi/erronkak/berria",
                "/ethazi/erronkak/" + e + "/editatu")) {
                String html = mvc.perform(get(path).principal(auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
                assertThat(html).contains("ETHAZI", "Erronkak", "Kinielak").doesNotContain("th:replace=");
                if (path.startsWith("/ethazi/kiniela?")) {
                    assertThat(html.split("class=\"module-total\"", -1)).hasSize(2);
                    assertThat(html).contains("rowspan=\"0\"", "IE2. Sarea konfiguratzen du", "100%");
                    assertThat(html).doesNotContain(">Gorde</button>");
                }
                if (System.getProperty("ethazi.previewDir") != null && path.contains("zikloaId=")) {
                    var preview = java.nio.file.Path.of(System.getProperty("ethazi.previewDir"));
                    java.nio.file.Files.createDirectories(preview);
                    java.nio.file.Files.writeString(preview.resolve(path.contains("kiniela") ? "kiniela.html" : "erronkak.html"), html);
                }
            }
            mvc.perform(post("/ethazi/kiniela/" + ie.getId() + "/loturak").param("zikloaId", cycle.getId().toString()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("success"));
            assertThat(kiniela.kiniela(cycle.getId()).get(0).emaitzak().get(0).loturak()).isEmpty();
            mvc.perform(post("/ethazi/erronkak/berria").principal(auth).param("zikloaId", cycle.getId().toString())
                .param("izena", "Mantendu erronka").param("hasieraData", "invalid"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("error"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mantendu erronka")));
        } finally { SecurityContextHolder.clearContext(); }
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
        return MockMvcBuilders.standaloneSetup(new EthaziController(service), new com.koadernoa.app.ethazi.controller.KinielaController(kiniela, service),
                new com.koadernoa.app.ethazi.controller.ErronkaController(kiniela, service),
                new org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler()).setControllerAdvice(new TemplateModel())
                .setViewResolvers(views).build();
    }
    @Test void httpFormsBindDynamicLevelsAndCanClearCurriculumLinks() throws Exception {
        var mvc = mvc();
        var request = post("/ethazi/gaitasunak/berria")
                .param("zikloaId", cycle.getId().toString()).param("mota", "TEKNIKOA")
                .param("kodea", "G2").param("deskribapena", "Sareak konfiguratu");
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
            var second = indicator(); second.setDeskribapena("Hardware eta softwarea bereizten ditu");
            service.gordeAdierazlea(id, gmId, null, second);
            var third = indicator(); third.setDeskribapena("Sistemaren funtzionamendua azaltzen du");
            service.gordeAdierazlea(id, gmId, null, third);
            service.gordeAdierazlea(id, service.gaitasuna(id).getMailak().get(1).getId(), null, indicator());
            var mvc = mvc();
            for (String path : new String[] {
                "/ethazi/gaitasunak", "/ethazi/gaitasunak?zikloaId=" + cycle.getId(),
                "/ethazi/gaitasunak?zikloaId=" + cycle.getId() + "&mota=ZEHARKAKOA",
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
            assertThat(rubric).contains("TRMM · RA1", "Mailaren deskribapena", "Osagaiak identifikatzen ditu",
                    "Sistema informatikoak kudeatzea", "rubric-competency-description",
                    "ethazi-rubric-page", "rubric-add-level", "class=\"drag-handle rubric-edit-only\"")
                    .doesNotContain("name=\"ordena\"", ">Mailakatzeak</a>");
            String edit = mvc.perform(get("/ethazi/gaitasunak/" + id + "/editatu").principal(auth)).andReturn().getResponse().getContentAsString();
            if (System.getProperty("ethazi.previewDir") != null) {
                var preview = java.nio.file.Path.of(System.getProperty("ethazi.previewDir"));
                java.nio.file.Files.createDirectories(preview);
                java.nio.file.Files.writeString(preview.resolve("rubric.html"), rubric);
                java.nio.file.Files.writeString(preview.resolve("edit.html"), edit);
            }
            assertThat(edit).contains("checked=\"checked\"").doesNotContain("id=\"izena\"", ">Izena</label>");
            assertThat(service.gaitasuna(id).getLegacyIzena()).isEqualTo("Sistema informatikoak kudeatzea");
            mvc.perform(post("/ethazi/gaitasunak/" + id + "/mailak/" + gmId + "/adierazleak/berria")
                .principal(auth).param("emaitzaIds", "-1").param("deskribapena", "Mantendu testu hau"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("error", "failedForm"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mantendu testu hau")));
            Long levelId = service.eredua(modelId).getMailak().get(0).getId();
            String cellUrl = "/ethazi/errubrika/gaitasunak/" + id + "/mailak/" + levelId + "/adierazleak";
            mvc.perform(post(cellUrl + "/berria").principal(auth).param("emaitzaIds", "-1").param("deskribapena", "Mantendu gelaxkan"))
                .andExpect(status().isOk()).andExpect(view().name("Ethazi/gaitasunak/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mantendu gelaxkan")));
            mvc.perform(post(cellUrl + "/berria").principal(auth).param("ordena", "999").param("deskribapena", "Automatikoki azkena"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("success"))
                .andExpect(redirectedUrl("/ethazi/gaitasunak?zikloaId=" + cycle.getId() + "&mota=TEKNIKOA#gelaxka-" + id + "-" + levelId));
            var indicators = service.gaitasuna(id).getMailak().get(0).getLorpenAdierazleak();
            assertThat(indicators.get(indicators.size() - 1).getOrdena()).isEqualTo(4);
            var orderRequest = post(cellUrl + "/berrordenatu").principal(auth);
            for (int i = indicators.size() - 1; i >= 0; i--) orderRequest.param("adierazleaIds", indicators.get(i).getId().toString());
            mvc.perform(orderRequest).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Lorpen-adierazleen ordena gorde da."));
            mvc.perform(post(cellUrl + "/berrordenatu").principal(auth).param("adierazleaIds", "-1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        } finally { SecurityContextHolder.clearContext(); }
    }
}
