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

/** Removes the obsolete (indicator, outcome) uniqueness left by ddl-auto=update. */
@Component
@RequiredArgsConstructor
@Slf4j
public class KinielaSchemaUpdater {
    private static final String INDEXES = """
        SELECT INDEX_NAME
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ethazi_kiniela_lotura'
          AND NON_UNIQUE=0 AND INDEX_NAME<>'PRIMARY'
        GROUP BY INDEX_NAME
        HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)=?
        """;

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void konponduUniqueZaharra() {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("mysql")) return;

            Long missingModules = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ethazi_kiniela_lotura WHERE moduloa_id IS NULL", Long.class);
            if (missingModules != null && missingModules > 0) {
                log.warn("Ezin da Kinielako unique zaharra automatikoki kendu: {} loturak ez du modulurik. "
                        + "Exekutatu ETHAZI hizkuntzen migrazioa.", missingModules);
                return;
            }

            List<String> contextual = jdbcTemplate.queryForList(INDEXES, String.class,
                    "adierazlea_id,emaitza_id,moduloa_id");
            if (contextual.isEmpty()) {
                jdbcTemplate.execute("ALTER TABLE ethazi_kiniela_lotura "
                        + "ADD CONSTRAINT uk_kiniela_modulua UNIQUE(adierazlea_id,emaitza_id,moduloa_id)");
            }

            List<String> obsolete = jdbcTemplate.queryForList(INDEXES, String.class,
                    "adierazlea_id,emaitza_id");
            for (String index : obsolete) {
                String safeIndex = index.replace("`", "``");
                jdbcTemplate.execute("ALTER TABLE ethazi_kiniela_lotura DROP INDEX `" + safeIndex + "`");
                log.info("Kinielako bi zutabeko unique zaharra kendu da: {}", index);
            }
        } catch (Exception ex) {
            log.error("Ezin izan da Kinielako unique murrizketa zaharra egiaztatu edo kendu. "
                    + "Exekutatu docs/migrations/20260930-kiniela-lotura-unique-konponketa.sql.", ex);
        }
    }
}
