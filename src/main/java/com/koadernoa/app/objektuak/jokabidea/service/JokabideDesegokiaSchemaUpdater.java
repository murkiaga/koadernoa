package com.koadernoa.app.objektuak.jokabidea.service;

import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Hibernate update-k MySQLen lehendik dagoen NOT NULL murrizketa kentzen ez duenean konpontzen du. */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JokabideDesegokiaSchemaUpdater implements CommandLineRunner {
    private static final String TAULA = "jokabide_desegokia";
    private static final String ZUTABEA = "moduloa_id";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("mysql")) return;

            gehituZutabeaFaltaBada("portaera_arrazoia", "testua_es", "VARCHAR(2000) NULL");
            gehituZutabeaFaltaBada("neurri_zuzentzailea", "testua_es", "VARCHAR(2000) NULL");
            gehituZutabeaFaltaBada("jokabide_desegokia", "deskribapen_zehatza_es", "TEXT NULL");
            gehituZutabeaFaltaBada("jokabide_desegokia", "aplikatutako_neurria_es", "TEXT NULL");
            nullableEginFaltaBada("jokabide_desegokia", "deskribapen_zehatza", "VARCHAR(10000) NULL");

            String nullable = jdbcTemplate.query("""
                    SELECT IS_NULLABLE FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?
                    """, rs -> rs.next() ? rs.getString(1) : null, TAULA, ZUTABEA);
            if ("NO".equalsIgnoreCase(nullable)) {
                jdbcTemplate.execute("ALTER TABLE jokabide_desegokia MODIFY COLUMN moduloa_id BIGINT NULL");
                log.info("jokabide_desegokia.moduloa_id nullable bihurtu da kudeatzailearen altetarako.");
            }
        } catch (Exception ex) {
            log.error("Ezin izan da jokabide_desegokiaren eskema eguneratu. Exekutatu "
                    + "docs/migrations/20261125-jokabide-elebiduna.sql eta, beharrezkoa bada, "
                    + "docs/migrations/20261102-jokabide-modulua-nullable.sql.", ex);
        }
    }

    private void gehituZutabeaFaltaBada(String taula, String zutabea, String definizioa) {
        Integer kopurua = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?
                """, Integer.class, taula, zutabea);
        if (kopurua != null && kopurua == 0) {
            jdbcTemplate.execute("ALTER TABLE " + taula + " ADD COLUMN " + zutabea + " " + definizioa);
            log.info("{}.{} zutabea sortu da.", taula, zutabea);
        }
    }

    private void nullableEginFaltaBada(String taula, String zutabea, String definizioa) {
        String nullable = jdbcTemplate.query("""
                SELECT IS_NULLABLE FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?
                """, rs -> rs.next() ? rs.getString(1) : null, taula, zutabea);
        if ("NO".equalsIgnoreCase(nullable)) {
            jdbcTemplate.execute("ALTER TABLE " + taula + " MODIFY COLUMN " + zutabea + " " + definizioa);
            log.info("{}.{} zutabea nullable bihurtu da.", taula, zutabea);
        }
    }
}
