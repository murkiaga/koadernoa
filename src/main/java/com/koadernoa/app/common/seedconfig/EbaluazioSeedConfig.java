package com.koadernoa.app.common.seedconfig;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioEgoera;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioMomentua;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioEgoeraRepository;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioMomentuaRepository;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.egutegia.repository.MailaRepository;


@Configuration
public class EbaluazioSeedConfig {

    @Bean
    CommandLineRunner seedMailakMomentuakEtaEgoerak(
            MailaRepository mailaRepository,
            EbaluazioMomentuaRepository emRepo,
            EbaluazioEgoeraRepository egoeraRepo) {

        return args -> {

            // 1) EGOERAK: EZ_AURKEZTUA, EZ_EBALUATUA_FALTAK
            seedEbaluazioEgoerak(egoeraRepo);

            // 2) MAILAK
            Maila lehen = ensureMaila(mailaRepository,
                    "LEHENENGOA", "1. maila", 1);
            Maila bigarren = ensureMaila(mailaRepository,
                    "BIGARRENA", "2. maila", 2);
            Maila cGradua = ensureMaila(mailaRepository,
                    "C_GRADUA", "C Gradua", 3);

            // 3) EBALUAZIO MOMENTUAK maila bakoitzerako
            seedMomentuakMailarentzat(emRepo, lehen);
            seedMomentuakMailarentzat(emRepo, bigarren);
            seedMomentuakMailarentzat(emRepo, cGradua);
        };
    }

    // ------------------ MAILAK ------------------

    private Maila ensureMaila(MailaRepository mailaRepository,
                              String kodea,
                              String izena,
                              int ordena) {

        return mailaRepository.findByKodea(kodea)
                .orElseGet(() -> {
                    Maila m = new Maila();
                    m.setKodea(kodea);
                    m.setIzena(izena);
                    m.setOrdena(ordena);
                    m.setAktibo(true);
                    return mailaRepository.save(m);
                });
    }

    // ------------------ EGOERAK ------------------

    private void seedEbaluazioEgoerak(EbaluazioEgoeraRepository egoeraRepo) {

        ensureEgoera(egoeraRepo,
                "EZ_AURKEZTUA",
                "Ez aurkeztua");

        ensureEgoera(egoeraRepo,
                "EZ_EBALUATUA_FALTAK",
                "Ez ebaluatua faltengatik");
    }

    private EbaluazioEgoera ensureEgoera(EbaluazioEgoeraRepository repo,
                                         String kodea,
                                         String izena) {

        return repo.findByKodea(kodea)
                .orElseGet(() -> {
                    EbaluazioEgoera e = new EbaluazioEgoera();
                    e.setKodea(kodea);
                    // izena eremua baduzu:
                    e.setIzena(izena);
                    return repo.save(e);
                });
    }

    // ------------------ MOMENTUAK ------------------

    private void seedMomentuakMailarentzat(EbaluazioMomentuaRepository emRepo,
                                           Maila maila) {

        // 3 ebaluazio arrunt
        ensureMomentu(emRepo, maila,
                "1_EBAL", "1. ebaluazioa",
                1, false, true, true);
        ensureMomentu(emRepo, maila,
                "2_EBAL", "2. ebaluazioa",
                2, false, true, true);
        ensureMomentu(emRepo, maila,
                "3_EBAL", "3. ebaluazioa",
                3, false, true, false);

        // Finalak: urte osoko datuak
        ensureMomentu(emRepo, maila,
                "1_FINAL", "1. finala",
                10, true, true, true);
        ensureMomentu(emRepo, maila,
                "2_FINAL", "2. finala",
                20, true, true, true);
    }

    private void ensureMomentu(EbaluazioMomentuaRepository emRepo,
                               Maila maila,
                               String kodea,
                               String izena,
                               int ordena,
                               boolean urteOsoa,
                               boolean onartuNotaZenb,
                               boolean aktibo) {

        emRepo.findByMailaAndKodea(maila, kodea)
                .orElseGet(() -> {
                    EbaluazioMomentua em = new EbaluazioMomentua();
                    em.setMaila(maila);
                    em.setKodea(kodea);
                    em.setIzena(izena);
                    em.setOrdena(ordena);
                    em.setUrteOsoa(urteOsoa);
                    em.setOnartuNotaZenbakizkoa(onartuNotaZenb);
                    em.setAktibo(aktibo);
                    return emRepo.save(em);
                });
    }
}