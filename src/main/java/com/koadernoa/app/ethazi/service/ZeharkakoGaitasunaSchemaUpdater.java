package com.koadernoa.app.ethazi.service;

import java.sql.Connection;
import javax.sql.DataSource;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Allows school-wide competencies and their private rubrics to have no cycle. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ZeharkakoGaitasunaSchemaUpdater {
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void zikloaNullableEgin() {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("mysql")) return;
            nullableEgin("ethazi_gaitasuna", "zikloa_id");
            nullableEgin("ethazi_mailakatze_eredua", "zikloa_id");
        } catch (Exception ex) {
            log.error("Ezin izan dira zeharkako gaitasunen ziklo-zutabeak nullable egin. "
                    + "Exekutatu docs/migrations/20261008-zeharkako-gaitasunak.sql.", ex);
        }
    }

    private void nullableEgin(String taula, String zutabea) {
        String nullable = jdbcTemplate.queryForObject("""
                SELECT IS_NULLABLE FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?
                """, String.class, taula, zutabea);
        if ("NO".equalsIgnoreCase(nullable)) {
            jdbcTemplate.execute("ALTER TABLE " + taula + " MODIFY COLUMN " + zutabea + " BIGINT NULL");
            log.info("{}.{} nullable bihurtu da zeharkako gaitasunetarako.", taula, zutabea);
        }
    }
}
