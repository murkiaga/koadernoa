package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Jarduera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;

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
class JardueraPersistenceTest {

    @Autowired
    TestEntityManager entityManager;

    @Test
    void jardueraBerriaEzKalifikagarriGordetzenDu() {
        Koadernoa koadernoa = new Koadernoa();
        entityManager.persist(koadernoa);

        Jarduera jarduera = new Jarduera();
        jarduera.setKoadernoa(koadernoa);
        jarduera.setData(LocalDate.of(2026, 11, 23));
        jarduera.setTitulua("Eskuz sortutako jarduera");
        jarduera.setMota("eginda");
        jarduera.setOrduak(1);

        Long id = entityManager.persistAndFlush(jarduera).getId();
        entityManager.clear();

        Jarduera gordeta = entityManager.find(Jarduera.class, id);
        assertThat(gordeta.isKalifikagarria()).isFalse();
    }
}
