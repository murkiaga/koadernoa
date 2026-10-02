package com.koadernoa.app.objektuak.koadernoak.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoPlangintzaKontrola.Egoera;
import com.koadernoa.app.objektuak.mezuak.service.MezuaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlangintzaKontrolScheduler {

    public static final String CRON = "0 0 7 * * MON-FRI";
    public static final String TIME_ZONE = "Europe/Madrid";

    private static final Logger logger = LoggerFactory.getLogger(PlangintzaKontrolScheduler.class);

    private final AplikazioAukeraService aplikazioAukeraService;
    private final KoadernoPlangintzaKontrolService kontrolService;
    private final MezuaService mezuaService;

    @Scheduled(cron = CRON, zone = TIME_ZONE)
    public void egunerokoKontrola() {
        try {
            if (!aplikazioAukeraService.isPlangintzaKontrolAutomatikoaAktibo()) {
                logger.info("Plangintza kontrol automatikoa desaktibatuta; ez da kontrolik egingo");
                return;
            }

            logger.info("Plangintza kontrol automatikoa hasita");
            List<KoadernoPlangintzaKontrola> kontrolak = kontrolService.lortuKontrola();
            List<KoadernoPlangintzaKontrola> osatuGabe = kontrolak.stream()
                    .filter(kontrola -> kontrola.egoera() == Egoera.OSATU_GABE)
                    .toList();
            int bidalitakoak = osatuGabe.isEmpty()
                    ? 0
                    : mezuaService.bidaliPlangintzaAbisuak(osatuGabe);

            logger.info(
                    "Plangintza kontrol automatikoa amaituta: {} koaderno aztertuta, {} osatu gabe, {} irakasleri mezua bidalita",
                    kontrolak.size(), osatuGabe.size(), bidalitakoak);
        } catch (Exception ex) {
            logger.error("Errorea plangintza kontrol automatikoa exekutatzean", ex);
        }
    }
}
