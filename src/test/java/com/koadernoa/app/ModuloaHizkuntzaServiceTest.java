package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;
import com.koadernoa.app.objektuak.modulua.service.ModuloaService;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.config.import=",
        "spring.jpa.show-sql=false"
}, showSql = false)
@TestExecutionListeners(listeners = {
        DependencyInjectionTestExecutionListener.class,
        TransactionalTestExecutionListener.class
}, mergeMode = MergeMode.REPLACE_DEFAULTS)
class ModuloaHizkuntzaServiceTest {
    @Autowired ModuloaRepository repository;
    @Autowired TestEntityManager entityManager;

    @Test
    void hizkuntzaZuzeneanEguneratzenDuBesteEremuakAldatuGabe() {
        var maila = new Maila();
        maila.setKodea("HIZKUNTZA-TEST");
        maila.setIzena("Test maila");
        entityManager.persist(maila);

        var moduloa = new Moduloa();
        moduloa.setIzena("Markatzeko lengoaiak");
        moduloa.setKodea("ML");
        moduloa.setEeiKodea("0373");
        moduloa.setOrduak(100);
        moduloa.setHizkuntza(Hizkuntza.EUSKARA);
        moduloa.setMaila(maila);
        entityManager.persistAndFlush(moduloa);

        var service = new ModuloaService(repository, null, null);
        assertThat(service.eguneratuHizkuntza(moduloa.getId(), Hizkuntza.GAZTELERA))
                .isEqualTo(Hizkuntza.GAZTELERA);
        entityManager.flush(); entityManager.clear();

        var saved = repository.findById(moduloa.getId()).orElseThrow();
        assertThat(saved.getHizkuntza()).isEqualTo(Hizkuntza.GAZTELERA);
        assertThat(saved.getEeiKodea()).isEqualTo("0373");
        assertThat(saved.getIzena()).isEqualTo("Markatzeko lengoaiak");
    }
}
