package com.koadernoa.app.common.seedconfig;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.entitateak.ZikloMaila;
import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;
import com.koadernoa.app.objektuak.zikloak.repository.FamiliaRepository;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;
import com.koadernoa.app.objektuak.zikloak.repository.ZikloaRepository;

@Configuration
public class ZikloEtaTaldeSeedConfig {

    @Bean
    CommandLineRunner seedFamiliaZikloEtaTaldeak(
            FamiliaRepository familiaRepo,
            ZikloaRepository zikloRepo,
            TaldeaRepository taldeaRepo) {

        return args -> {
            // 1) FAMILIAK (zure jatorrizko logika mantentzen dugu)
            for (String iz : new String[]{
                    "INFORMATIKA", "MERKATARITZA", "FABRIKAZIO_MEKANIKOA", "OSASUNGINTZA"
            }) {
                if (!familiaRepo.existsByIzenaIgnoreCase(iz)) {
                    Familia f = Familia.builder()
                            .izena(iz)
                            .slug(iz.toLowerCase())
                            .aktibo(true)
                            .build();
                    familiaRepo.save(f);
                }
            }

            // 2) INFORMATIKA familia hartu
            Familia informatika = familiaRepo.findByIzenaIgnoreCase("INFORMATIKA")
                    .orElseThrow(() -> new IllegalStateException(
                            "INFORMATIKA familia ez da aurkitu seed-ean."));

            // 3) ZIKLOAK (INFORMATIKA familiakoak)
            Zikloa zikloSmr = ensureZikloa(
                    zikloRepo,
                    "MIKROINFORMATIKA-SISTEMETAKO ETA SAREETAKO TEKNIKARIA",
                    ZikloMaila.ErdiMaila,
                    informatika
            );

            Zikloa zikloDam = ensureZikloa(
                    zikloRepo,
                    "PLATAFORMA ANITZEKO APLIKAZIOAK GARATZEKO GOI-MAILAKO TEKNIKARIA",
                    ZikloMaila.GoiMaila,
                    informatika
            );

            Zikloa zikloDaw = ensureZikloa(
                    zikloRepo,
                    "WEB APLIKAZIOEN GARAPENEKO GOI-MAILAKO TEKNIKARIA",
                    ZikloMaila.GoiMaila,
                    informatika
            );

            // 4) TALDEAK

            // MIKROINFORMATIKA-SISTEMETAKO ETA SAREETAKO TEKNIKARIA
            ensureTaldea(taldeaRepo, "1SMA", zikloSmr);
            ensureTaldea(taldeaRepo, "1SMD", zikloSmr);
            ensureTaldea(taldeaRepo, "2SMA", zikloSmr);
            ensureTaldea(taldeaRepo, "2SMD", zikloSmr);

            // PLATAFORMA ANITZEKO APLIKAZIOAK...
            // Hemen ez dakigu ze talde-kode erabili nahi dituzun zehazki;
            // nahi baduzu, gero gehitu beste ensureTaldea() gehiago.
            // adib.:
            // ensureTaldea(taldeaRepo, "1DAM", zikloDam);
            // ensureTaldea(taldeaRepo, "2DAM", zikloDam);

            // WEB APLIKAZIOEN GARAPENA
            ensureTaldea(taldeaRepo, "1AW3", zikloDaw);
            ensureTaldea(taldeaRepo, "2AW3", zikloDaw);
            
            // MULTI
            ensureTaldea(taldeaRepo, "2AM3", zikloDam);
        };
    }

    // --------- Laguntzaile metodoak ---------

    /** Zikloa ez badago (izena + familia), sortu; bestela existitzen dena bueltatu. */
    private Zikloa ensureZikloa(ZikloaRepository zikloRepo,
                                String izena,
                                ZikloMaila maila,
                                Familia familia) {

        return zikloRepo
                .findByIzenaIgnoreCaseAndFamilia(izena, familia)
                .orElseGet(() -> {
                    Zikloa z = new Zikloa();
                    z.setIzena(izena);
                    z.setMaila(maila);       // zure eremua "maila" bada (ZikloMaila)
                    z.setFamilia(familia);
                    // z.setAktibo(true);  // baduzu "aktibo" eremua, hemen aktibatu
                    return zikloRepo.save(z);
                });
    }

    /** Taldea ez badago (izena + zikloa), sortu; bestela existitzen dena bueltatu. */
    private Taldea ensureTaldea(TaldeaRepository taldeaRepo,
                                String izena,
                                Zikloa zikloa) {

        return taldeaRepo
                .findByIzenaIgnoreCaseAndZikloa(izena, zikloa)
                .orElseGet(() -> {
                    Taldea t = new Taldea();
                    t.setIzena(izena);
                    t.setZikloa(zikloa);
                    // t.setAktibo(true); // baduzu halako eremua
                    return taldeaRepo.save(t);
                });
    }
}
