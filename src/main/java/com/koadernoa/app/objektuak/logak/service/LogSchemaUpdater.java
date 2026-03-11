package com.koadernoa.app.objektuak.logak.service;

import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LogSchemaUpdater {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void expandLogMotaEnumIfNeeded() {
        try (Connection c = dataSource.getConnection()) {
            String product = c.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("mysql")) {
                return;
            }

            jdbcTemplate.execute("""
                ALTER TABLE log_sarrera
                MODIFY COLUMN mota ENUM('KOADERNO_EZABAKETA','DESMATRIKULATZEA','MATRIKULATZEA')
                """);
        } catch (Exception ignored) {
            // Beste DB batean edo jada bateragarria bada, ez dugu aplikazioa gelditu nahi.
        }
    }
}
