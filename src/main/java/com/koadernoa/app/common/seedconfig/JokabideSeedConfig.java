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
    @Bean
    CommandLineRunner jokabideDesegokiaSeed(PortaeraArrazoiaRepository arrazoiak,
                                             NeurriZuzentzaileaRepository neurriak) {
        return args -> {
            if (arrazoiak.count() == 0) {
                PortaeraArrazoia a = new PortaeraArrazoia();
                a.setKodea("30.d");
                a.setTestua("Irakasleek edo agintari akademikoek agindutakoa ez betetzea.");
                a.setAktibo(true); a.setDefektuzkoa(true); a.setOrdena(10);
                arrazoiak.save(a);
            }
            if (neurriak.count() == 0) {
                NeurriZuzentzailea n = new NeurriZuzentzailea();
                n.setKodea("34.a");
                n.setTestua("Izandako jokabide desegokiari eta haren ondorioei buruzko hausnarketa");
                n.setAktibo(true); n.setDefektuzkoa(true); n.setOrdena(10);
                neurriak.save(n);
            }
        };
    }
}
