package com.koadernoa.app.common.seedconfig;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;
import com.koadernoa.app.objektuak.zikloak.repository.FamiliaRepository;

@Configuration
public class FamiliaSeedConfig {
    @Bean
    CommandLineRunner seedFamilia(FamiliaRepository repo) {
        return args -> {
            for (String iz : new String[]{
                "INFORMATIKA ETA KOMUNIKAZIOAK", 
                "MERKATARITZA ETA MARKETINA", 
                "FABRIKAZIO MEKANIKOA", 
                "OSASUNGINTZA", 
                "INSTALATZE ETA MANTENTZE LANAK"
            }) {
                if (!repo.existsByIzenaIgnoreCase(iz)) {
                    repo.save(Familia.builder().izena(iz).slug(iz.toLowerCase()).aktibo(true).build());
                }
            }
        };
    }
}
