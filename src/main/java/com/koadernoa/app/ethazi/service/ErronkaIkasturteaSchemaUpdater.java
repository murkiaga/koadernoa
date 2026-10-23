package com.koadernoa.app.ethazi.service;

import java.sql.Connection;
import javax.sql.DataSource;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Aurreko instalazioetako erronkei une horretako ikasturte aktiboa esleitzen die. */
@Component @RequiredArgsConstructor @Slf4j
public class ErronkaIkasturteaSchemaUpdater {
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void osatuIkasturtea() {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("mysql")) return;
            Integer column = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ethazi_erronka' AND COLUMN_NAME='ikasturtea_id'
                """, Integer.class);
            if (column == null || column == 0) return;
            Long missing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ethazi_erronka WHERE ikasturtea_id IS NULL", Long.class);
            if (missing != null && missing > 0) {
                // Lehenik daten arabera saiatu: irailetik abendura urte bera;
                // urtarriletik abuztuaren amaierara aurreko hasiera-urtea.
                jdbcTemplate.update("""
                    UPDATE ethazi_erronka e
                    JOIN (
                      SELECT CAST(LEFT(izena,4) AS UNSIGNED) AS hasiera_urtea, MAX(id) AS id
                      FROM ikasturtea WHERE izena REGEXP '^[0-9]{4}'
                      GROUP BY CAST(LEFT(izena,4) AS UNSIGNED)
                    ) i ON i.hasiera_urtea = CASE WHEN MONTH(e.hasiera_data)>=9
                          THEN YEAR(e.hasiera_data) ELSE YEAR(e.hasiera_data)-1 END
                    SET e.ikasturtea_id=i.id WHERE e.ikasturtea_id IS NULL
                    """);
                missing = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ethazi_erronka WHERE ikasturtea_id IS NULL", Long.class);
            }
            if (missing != null && missing > 0) {
                var active = jdbcTemplate.queryForList(
                    "SELECT id FROM ikasturtea WHERE aktiboa=1 ORDER BY id DESC LIMIT 1", Long.class);
                if (active.isEmpty()) {
                    log.warn("{} erronkak ez dute ikasturterik eta ez dago ikasturte aktiborik. "
                        + "Aktibatu ikasturte bat eta berrabiarazi aplikazioa.", missing);
                    return;
                }
                jdbcTemplate.update("UPDATE ethazi_erronka SET ikasturtea_id=? WHERE ikasturtea_id IS NULL", active.get(0));
                log.info("Datagatik identifikatu ezin ziren aurreko {} erronkari ikasturte aktiboa esleitu zaie.", missing);
            }
            String nullable = jdbcTemplate.queryForObject("""
                SELECT IS_NULLABLE FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ethazi_erronka' AND COLUMN_NAME='ikasturtea_id'
                """, String.class);
            if ("YES".equalsIgnoreCase(nullable))
                jdbcTemplate.execute("ALTER TABLE ethazi_erronka MODIFY COLUMN ikasturtea_id BIGINT NOT NULL");
        } catch (Exception ex) {
            log.error("Ezin izan da erronken ikasturtea osatu. Exekutatu "
                + "docs/migrations/20261007-erronka-ikasturtea.sql.", ex);
        }
    }
}
