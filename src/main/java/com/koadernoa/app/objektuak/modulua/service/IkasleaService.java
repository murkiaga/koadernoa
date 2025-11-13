package com.koadernoa.app.objektuak.modulua.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.entitateak.MatrikulaEgoera;
import com.koadernoa.app.objektuak.modulua.repository.IkasleaRepository;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IkasleaService {

    private final KoadernoaRepository koadernoaRepo;
    private final IkasleaRepository ikasleaRepo;
    private final MatrikulaRepository matrikulaRepo;

    public static record ImportResult(int sortuak, int baztertuak, String ohartarazpena) {}

    /**
     * KOADERNO BAKARRA sinkronizatu:
     *  - Excel bidez eguneratutako taldeko HNA zerrenda hartzen du (DB-tik),
     *  - Koaderno horretan dauden baina HNA zerrendan EZ dauden matrikulak ezabatzen ditu,
     *    (horrek kaskadaz asistentziak ere ezabatuko ditu),
     *  - Eta falta diren ikasleak gehitzen ditu (MATRIKULATUA egoerarekin).
     */
    @Transactional
    public ImportResult syncKoadernoBakarra(Long koadernoaId) {
        Koadernoa koa = koadernoaRepo.findById(koadernoaId)
            .orElseThrow(() -> new IllegalArgumentException("Koadernoa ez da aurkitu: " + koadernoaId));

        if (koa.getModuloa() == null || koa.getModuloa().getTaldea() == null) {
            return new ImportResult(0, 0, "Koadernoak ez du talderik lotuta (moduloa/taldea falta).");
        }

        Long taldeId = koa.getModuloa().getTaldea().getId();

        // 1) Taldeko HNA zerrenda (Excel inportazioaren ostean DBan dagoena)
        List<String> hnasExcel = ikasleaRepo.findHnasByTaldeaId(taldeId);
        if (hnasExcel == null || hnasExcel.isEmpty()) {
            // Segurtasun neurri: ez badago HNArik, ez dugu ezer ezabatzen
            return new ImportResult(0, 0, "Ez dago HNA baliorik talde horretan; ez da ezer ezabatu.");
        }

        // 2) Ezabatu koaderno honetan dauden baina HNA zerrendan EZ dauden matrikulak
        List<Matrikula> soberan = matrikulaRepo
                .findToRemoveByKoadernoAndNotInHnas(koadernoaId, hnasExcel);
        if (!soberan.isEmpty()) {
            matrikulaRepo.deleteAll(soberan); 
            // Hemen aktibatzen da:
            //  - Matrikula.asistentziak -> cascade = REMOVE + orphanRemoval = true
        }

        // 3) Gehitu: taldeko baina koaderno honetan matrikulatu gabe dauden ikasleak
        List<Ikaslea> falta = ikasleaRepo.findTeamStudentsNotEnrolledInKoaderno(taldeId, koadernoaId);
        int sortuak = 0;
        for (Ikaslea ik : falta) {
            Matrikula m = new Matrikula();
            m.setIkaslea(ik);
            m.setKoadernoa(koa);
            m.setEgoera(MatrikulaEgoera.MATRIKULATUA);
            matrikulaRepo.save(m);
            sortuak++;
        }

        return new ImportResult(sortuak, 0, null);
    }

    /**
     * TALDE batean dauden KOADERNO GUZTIAK sinkronizatu.
     * Kudeatzailearen Excel-inportazioaren ondoren deitzen duzu.
     */
    @Transactional
    public ImportResult syncKoadernoakTalderako(Long taldeaId) {
        var koadernoIds = koadernoaRepo.findKoadernoIdsByTaldeaId(taldeaId);
        if (koadernoIds.isEmpty()) {
            return new ImportResult(0, 0, "Ez dago koadernorik talde horretarako.");
        }

        int sortuakGuztira = 0;
        for (Long koaId : koadernoIds) {
            ImportResult res = syncKoadernoBakarra(koaId);
            sortuakGuztira += res.sortuak();
        }
        return new ImportResult(sortuakGuztira, 0, null);
    }

}

