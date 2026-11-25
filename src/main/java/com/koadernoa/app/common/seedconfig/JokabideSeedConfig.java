package com.koadernoa.app.common.seedconfig;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.koadernoa.app.objektuak.jokabidea.entitateak.NeurriZuzentzailea;
import com.koadernoa.app.objektuak.jokabidea.entitateak.PortaeraArrazoia;
import com.koadernoa.app.objektuak.jokabidea.repository.NeurriZuzentzaileaRepository;
import com.koadernoa.app.objektuak.jokabidea.repository.PortaeraArrazoiaRepository;

@Configuration
public class JokabideSeedConfig {
    private static final String PORTAERA_KODEA = "30.d";
    private static final String PORTAERA_ES = "La simple desobediencia a los profesores, profesoras o autoridades académicas cuando no comporte actitudes de menosprecio, insulto o indisciplina deliberada, así como no atender las indicaciones del resto del personal del centro docente en ejercicio de sus funciones.";
    private static final String NEURRIA_KODEA = "34.a";
    private static final String NEURRIA_ES = "Reflexión sobre la conducta inadecuada concreta y sus consecuencias.";

    @Bean
    CommandLineRunner jokabideDesegokiaSeed(PortaeraArrazoiaRepository arrazoiak,
                                             NeurriZuzentzaileaRepository neurriak) {
        return args -> {
            if (arrazoiak.count() == 0) {
                PortaeraArrazoia a = new PortaeraArrazoia();
                a.setKodea(PORTAERA_KODEA);
                a.setTestua("Irakasleek edo agintari akademikoek agindutakoa ez betetzea.");
                a.setTestuaEs(PORTAERA_ES);
                a.setAktibo(true); a.setDefektuzkoa(true); a.setOrdena(10);
                arrazoiak.save(a);
            }
            if (neurriak.count() == 0) {
                NeurriZuzentzailea n = new NeurriZuzentzailea();
                n.setKodea(NEURRIA_KODEA);
                n.setTestua("Izandako jokabide desegokiari eta haren ondorioei buruzko hausnarketa");
                n.setTestuaEs(NEURRIA_ES);
                n.setAktibo(true); n.setDefektuzkoa(true); n.setOrdena(10);
                neurriak.save(n);
            }

            // Euskarazko testua pertsonalizatuta egon daiteke: ez da inoiz gainidazten.
            arrazoiak.findAll().stream()
                    .filter(a -> PORTAERA_KODEA.equals(a.getKodea()))
                    .filter(a -> a.getTestuaEs() == null || a.getTestuaEs().isBlank())
                    .forEach(a -> { a.setTestuaEs(PORTAERA_ES); arrazoiak.save(a); });
            neurriak.findAll().stream()
                    .filter(n -> NEURRIA_KODEA.equals(n.getKodea()))
                    .filter(n -> n.getTestuaEs() == null || n.getTestuaEs().isBlank())
                    .forEach(n -> { n.setTestuaEs(NEURRIA_ES); neurriak.save(n); });
        };
    }
}
