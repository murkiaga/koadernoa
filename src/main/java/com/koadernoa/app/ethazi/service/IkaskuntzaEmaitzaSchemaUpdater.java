package com.koadernoa.app.ethazi.service;

import java.sql.Connection;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Garapeneko eskema zaharretatik jada erabiltzen ez den moduloa_id zutabea kentzen du. */
@Component
@RequiredArgsConstructor
@Slf4j
public class IkaskuntzaEmaitzaSchemaUpdater {
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void kenduLegacyModuloa() {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("mysql")) return;

            Integer exists = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ikaskuntza_emaitza'
                      AND COLUMN_NAME='moduloa_id'
                    """, Integer.class);
            if (exists == null || exists == 0) return;

            List<String> foreignKeys = jdbcTemplate.queryForList("""
                    SELECT DISTINCT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
                    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ikaskuntza_emaitza'
                      AND COLUMN_NAME='moduloa_id' AND REFERENCED_TABLE_NAME IS NOT NULL
                    """, String.class);
            for (String foreignKey : foreignKeys) {
                jdbcTemplate.execute("ALTER TABLE ikaskuntza_emaitza DROP FOREIGN KEY `"
                        + foreignKey.replace("`", "``") + "`");
            }
            jdbcTemplate.execute("ALTER TABLE ikaskuntza_emaitza DROP COLUMN moduloa_id");
            log.info("ikaskuntza_emaitza.moduloa_id legacy zutabea kendu da.");
        } catch (Exception ex) {
            log.error("Ezin izan da ikaskuntza_emaitza.moduloa_id legacy zutabea kendu; "
                    + "ikaskuntza-emaitza berriak gordetzeak huts egin dezake.", ex);
        }
    }
}
