package com.koadernoa.app.common.seedconfig;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.koadernoa.app.objektuak.konfigurazioa.entitateak.AplikazioAukera;
import com.koadernoa.app.objektuak.konfigurazioa.repository.AplikazioAukeraRepository;
import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;

@Configuration
public class AplikazioAukeraSeedConfig {

    @Bean
    CommandLineRunner seedAplikazioAukerak(AplikazioAukeraRepository repo) {
        return args -> {
            ensure(repo, AplikazioAukeraService.AUTH_GOOGLE_ENABLED, "true");
            ensure(repo, AplikazioAukeraService.AUTH_AD_ENABLED, "false");
            ensure(repo, AplikazioAukeraService.AUTH_DEFAULT, "google");
        };
    }

    private void ensure(AplikazioAukeraRepository repo, String giltza, String balioa) {
        repo.findById(giltza).orElseGet(() -> {
            AplikazioAukera aukera = new AplikazioAukera();
            aukera.setGiltza(giltza);
            aukera.setBalioa(balioa);
            return repo.save(aukera);
        });
    }
}
