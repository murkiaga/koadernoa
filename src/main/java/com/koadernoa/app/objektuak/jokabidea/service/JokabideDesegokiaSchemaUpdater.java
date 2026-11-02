package com.koadernoa.app.objektuak.jokabidea.service;

import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Hibernate update-k MySQLen lehendik dagoen NOT NULL murrizketa kentzen ez duenean konpontzen du. */
@Component
@RequiredArgsConstructor
@Slf4j
public class JokabideDesegokiaSchemaUpdater {
    private static final String TAULA = "jokabide_desegokia";
    private static final String ZUTABEA = "moduloa_id";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void moduloaNullableEgin() {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("mysql")) return;

            String nullable = jdbcTemplate.query("""
                    SELECT IS_NULLABLE FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?
                    """, rs -> rs.next() ? rs.getString(1) : null, TAULA, ZUTABEA);
            if ("NO".equalsIgnoreCase(nullable)) {
                jdbcTemplate.execute("ALTER TABLE jokabide_desegokia MODIFY COLUMN moduloa_id BIGINT NULL");
                log.info("jokabide_desegokia.moduloa_id nullable bihurtu da kudeatzailearen altetarako.");
            }
        } catch (Exception ex) {
            log.error("Ezin izan da jokabide_desegokia.moduloa_id nullable bihurtu. "
                    + "Exekutatu docs/migrations/20261102-jokabide-modulua-nullable.sql.", ex);
        }
    }
}
