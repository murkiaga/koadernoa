package com.koadernoa.app.ethazi.service;

import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Bateratzen ditu errubrikako ebidentzia-adierazle lotura-taularen garapeneko
 * bi zutabe-izendapenak. Hibernate-ren ddl-auto=update-k ez ditu ordezkatutako
 * zutabeak kentzen eta bi bikoteak NOT NULL utz ditzake.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ErrubrikaSchemaUpdater {
    private static final String TAULA = "ethazi_erronka_ebidentzia_adierazlea";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void bateratuEbidentziaAdierazleZutabeak() {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("mysql")) return;
            if (!zutabeaDago("erronka_ebidentzia_id") || !zutabeaDago("lorpen_adierazleak_id")
                    || !zutabeaDago("ebidentzia_id") || !zutabeaDago("adierazlea_id")) return;

            // Garapeneko izendapen laburrarekin gordetako loturak izendapen
            // kanonikora ekarri, halakorik balego.
            jdbcTemplate.update("""
                    UPDATE ethazi_erronka_ebidentzia_adierazlea
                    SET erronka_ebidentzia_id=ebidentzia_id,
                        lorpen_adierazleak_id=adierazlea_id
                    WHERE (erronka_ebidentzia_id IS NULL OR erronka_ebidentzia_id=0
                           OR lorpen_adierazleak_id IS NULL OR lorpen_adierazleak_id=0)
                      AND ebidentzia_id IS NOT NULL AND ebidentzia_id<>0
                      AND adierazlea_id IS NOT NULL AND adierazlea_id<>0
                    """);

            // JPAk lehenengo bi zutabeak erabiltzen ditu. Beste biek nullable
            // izan behar dute INSERT berriek baliorik eman behar ez izateko.
            nullableEginBeharrezkoaBada("ebidentzia_id");
            nullableEginBeharrezkoaBada("adierazlea_id");
        } catch (Exception ex) {
            log.error("Ezin izan da errubrikako ebidentzia-adierazle lotura-taula bateratu. "
                    + "Exekutatu docs/migrations/20261007-errubrika-adierazle-zutabeak.sql.", ex);
        }
    }

    private boolean zutabeaDago(String zutabea) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?
                """, Integer.class, TAULA, zutabea);
        return count != null && count > 0;
    }

    private void nullableEginBeharrezkoaBada(String zutabea) {
        String nullable = jdbcTemplate.queryForObject("""
                SELECT IS_NULLABLE FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?
                """, String.class, TAULA, zutabea);
        if ("NO".equalsIgnoreCase(nullable)) {
            jdbcTemplate.execute("ALTER TABLE " + TAULA + " MODIFY COLUMN " + zutabea + " BIGINT NULL");
            log.info("{}.{} nullable bihurtu da, errubrikako loturen eskema bateratzeko.", TAULA, zutabea);
        }
    }
}
