package com.koadernoa.app.objektuak.audit.service;

import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditSchemaUpdater {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void expandAuditEkintzaEnumIfNeeded() {
        try (Connection c = dataSource.getConnection()) {
            String product = c.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("mysql")) {
                return;
            }

            jdbcTemplate.execute("""
                ALTER TABLE audit_event
                MODIFY COLUMN ekintza ENUM(
                    'PROGRAMAZIOA_INPORTATU',
                    'AURREKO_URTEKO_DENBORALIZAZIOA_INPORTATU',
                    'UDA_SORTU',
                    'UDA_EZABATU',
                    'UDAN_JARDUERA_PLANIFIKATU',
                    'UDAN_JARDUERA_EZABATU',
                    'EBALUAZIOA_EDITATU',
                    'EBALUAKETA_BERRIA',
                    'UDA_DENBORALIZAZIORA_BOLKATU',
                    'PROGRAMAZIOA_DENBORALIZAZIORA_BOLKATU',
                    'JARDUERA_SORTU',
                    'JARDUERA_MUGITU',
                    'JARDUERA_EDITATU',
                    'ASISTENTZIA_PASATU',
                    'FALTAK_HEZKUNTZATIK_INPORTATU',
                    'FALTEN_JAKINARAZPENA_DESKARGATU',
                    'NOTAK_GORDE',
                    'ESTATISTIKA_BERKALKULATU',
                    'KOADERNOA_SORTU',
                    'KOADERNOA_EZABATU',
                    'ORDUTEGI_BERRIA_SORTU',
                    'KOADERNOA_PARTEKATU'
                ) NULL
                """);
        } catch (Exception ignored) {
            // DB bateragarritasunagatik edo jada eguneratuta badago, ez dugu aplikazioa eten nahi.
        }
    }
}
