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

    //Jada ikasle inpotazioa exceletik eginda dagoenean eta koaderno berri bat sortzean, bertara inportatzeko
    @Transactional
    public ImportResult importTeamStudentsIntoKoaderno(Long koadernoaId) {
        Koadernoa koa = koadernoaRepo.findById(koadernoaId)
            .orElseThrow(() -> new IllegalArgumentException("Koadernoa ez da aurkitu"));

        if (koa.getModuloa() == null || koa.getModuloa().getTaldea() == null) {
            return new ImportResult(0, 0, "Koadernoak ez du talderik lotuta (moduloa/taldea falta).");
        }

        Long taldeId = koa.getModuloa().getTaldea().getId();

        // Koaderno honetan oraindik matrikulatuta EZ dauden taldeko ikasleak
        List<Ikaslea> hautagaiak =
            ikasleaRepo.findTeamStudentsNotEnrolledInKoaderno(taldeId, koadernoaId);

        int sortuak = 0;
        for (Ikaslea ik : hautagaiak) {
            Matrikula m = new Matrikula();
            m.setIkaslea(ik);
            m.setKoadernoa(koa);
            m.setEgoera(MatrikulaEgoera.MATRIKULATUA);
            matrikulaRepo.save(m);
            sortuak++;
        }

        return new ImportResult(sortuak, 0, null);
    }
}
