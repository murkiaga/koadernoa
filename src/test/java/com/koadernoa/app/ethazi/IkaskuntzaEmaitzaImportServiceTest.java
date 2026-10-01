package com.koadernoa.app.ethazi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import com.koadernoa.app.ethazi.dto.IkaskuntzaEmaitzaImportRow;
import com.koadernoa.app.ethazi.dto.IkaskuntzaEmaitzaImportRow.Egoera;
import com.koadernoa.app.ethazi.service.IkaskuntzaEmaitzaImportService;
import com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza;
import com.koadernoa.app.objektuak.modulua.entitateak.IkaskuntzaEmaitza;
import com.koadernoa.app.objektuak.modulua.repository.IkaskuntzaEmaitzaRepository;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.config.import=",
        "spring.jpa.show-sql=false"
}, showSql = false)
@Import(IkaskuntzaEmaitzaImportService.class)
@TestExecutionListeners(listeners = {
        DependencyInjectionTestExecutionListener.class,
        TransactionalTestExecutionListener.class
}, mergeMode = MergeMode.REPLACE_DEFAULTS)
class IkaskuntzaEmaitzaImportServiceTest {
    @Autowired IkaskuntzaEmaitzaImportService service;
    @Autowired IkaskuntzaEmaitzaRepository repository;
    @Autowired TestEntityManager entityManager;

    @Test
    void euskarazInportatzeakEzDuGaztelaniazkoBalioaUkitzenEtaUpdateEgitenDu() {
        IkaskuntzaEmaitza existing = existing("0221", 1, "EU zaharra", "ES mantendu");
        Long id = existing.getId();
        var row = row("0221", 1, "EU berria", Hizkuntza.EUSKARA);

        var result = service.inportatu(service.aurrebista(List.of(row)));
        entityManager.flush(); entityManager.clear();

        var saved = repository.findByEeiKodeaAndOrdena("0221", 1).orElseThrow();
        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getDeskribapena()).isEqualTo("EU berria");
        assertThat(saved.getDeskribapenaEs()).isEqualTo("ES mantendu");
        assertThat(result.sortuak()).isZero();
        assertThat(result.eguneratuak()).isOne();
        assertThat(repository.count()).isOne();
    }

    @Test
    void gaztelaniazInportatzeakEzDuEuskarazkoBalioaUkitzen() {
        existing("0156", 2, "EU mantendu", "ES zaharra");

        service.inportatu(service.aurrebista(List.of(row("0156", 2, "ES nueva", Hizkuntza.GAZTELERA))));
        entityManager.flush(); entityManager.clear();

        var saved = repository.findByEeiKodeaAndOrdena("0156", 2).orElseThrow();
        assertThat(saved.getDeskribapena()).isEqualTo("EU mantendu");
        assertThat(saved.getDeskribapenaEs()).isEqualTo("ES nueva");
    }

    @Test
    void aurrekoZeroaMantenduzErregistroBerriaSortzenDu() {
        var result = service.inportatu(service.aurrebista(List.of(
                row("0222", 1, "Lehen emaitza", Hizkuntza.EUSKARA))));

        entityManager.flush(); entityManager.clear();
        var saved = repository.findAll().get(0);
        assertThat(saved.getEeiKodea()).isEqualTo("0222");
        assertThat(saved.getKodea()).isEqualTo("IE1");
        assertThat(result.sortuak()).isOne();
    }

    @Test
    void aurrebistanArazoGisaMarkatutakoLerroBakarraHautatzeanBerrizBalidatuEtaInportatzenDu() {
        var selected = row("0483", 1, "Informatika-sistemak ebaluatzen ditu", Hizkuntza.EUSKARA)
                .preview(Egoera.ARAZOA, "PDFan gako bera errepikatuta zegoen.", null);

        var result = service.inportatu(List.of(selected));

        entityManager.flush(); entityManager.clear();
        assertThat(repository.findByEeiKodeaAndOrdena("0483", 1)).isPresent();
        assertThat(result.sortuak()).isOne();
        assertThat(result.arazoak()).isZero();
    }

    private IkaskuntzaEmaitza existing(String code, int order, String eu, String es) {
        var entity = new IkaskuntzaEmaitza();
        entity.setEeiKodea(code); entity.setOrdena(order); entity.setKodea("IE" + order);
        entity.setDeskribapena(eu); entity.setDeskribapenaEs(es);
        return entityManager.persistAndFlush(entity);
    }

    private IkaskuntzaEmaitzaImportRow row(String code, int order, String description, Hizkuntza language) {
        return new IkaskuntzaEmaitzaImportRow(code, order, "IE" + order, description,
                language, null, null, null);
    }
}
