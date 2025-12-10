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
                "INFORMATIKA", "MERKATARITZA", "FABRIKAZIO_MEKANIKOA", "OSASUNGINTZA"
            }) {
                if (!repo.existsByIzenaIgnoreCase(iz)) {
                    repo.save(Familia.builder().izena(iz).slug(iz.toLowerCase()).aktibo(true).build());
                }
            }
        };
    }
}
