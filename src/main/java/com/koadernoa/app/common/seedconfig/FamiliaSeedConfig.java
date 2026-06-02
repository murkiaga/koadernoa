package com.koadernoa.app.common.seedconfig;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;
import com.koadernoa.app.objektuak.zikloak.repository.FamiliaRepository;

// Desgaituta: datu hauek eskuz editatuta daude DBan, eta berrabiaraztean ez berriz sortzeko.
// @Configuration
public class FamiliaSeedConfig {
    // Desgaituta: seed hau ez exekutatzeko.
    // @Bean
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
