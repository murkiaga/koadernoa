package com.koadernoa.app.objektuak.koadernoak.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoOrdutegiBlokea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoOrdutegiBlokeaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FaltenEbaluaketaService {

    private final KoadernoOrdutegiBlokeaRepository koadernoOrdutegiBlokeaRepository;
    private final KoadernoKlaseEgunService koadernoKlaseEgunService;

    @Transactional(readOnly = true)
    public List<EbaluaketaAukera> lortuAukerak(Koadernoa koadernoa) {
        if (koadernoa == null || koadernoa.getId() == null || koadernoa.getEgutegia() == null) {
            return List.of();
        }

        Egutegia egutegia = koadernoa.getEgutegia();
        LocalDate ikasturteHasiera = egutegia.getHasieraData();
        LocalDate ikasturteBukaera = egutegia.getBukaeraData();
        if (ikasturteHasiera == null || ikasturteBukaera == null || ikasturteHasiera.isAfter(ikasturteBukaera)) {
            return List.of();
        }

        List<LocalDate> bitartekoBukaerak = new ArrayList<>();
        gehituBukaeraBaliozkoa(bitartekoBukaerak, egutegia.getLehenEbalBukaera(),
                ikasturteHasiera, ikasturteBukaera);
        gehituBukaeraBaliozkoa(bitartekoBukaerak, egutegia.getBigarrenEbalBukaera(),
                ikasturteHasiera, ikasturteBukaera);
        bitartekoBukaerak.sort(LocalDate::compareTo);

        var blokeak = koadernoOrdutegiBlokeaRepository.findByKoadernoa_Id(koadernoa.getId());
        List<EbaluaketaAukera> aukerak = new ArrayList<>();
        LocalDate hasiera = ikasturteHasiera;

        for (LocalDate bukaera : bitartekoBukaerak) {
            if (bukaera.isBefore(hasiera)) continue;
            aukerak.add(sortuAukera(aukerak.size() + 1, egutegia, blokeak, hasiera, bukaera));
            hasiera = bukaera.plusDays(1);
        }
        if (!hasiera.isAfter(ikasturteBukaera)) {
            aukerak.add(sortuAukera(aukerak.size() + 1, egutegia, blokeak, hasiera, ikasturteBukaera));
        }
        return List.copyOf(aukerak);
    }

    @Transactional(readOnly = true)
    public EbaluaketaAukera lortuAukera(Koadernoa koadernoa, Integer zenbakia) {
        if (zenbakia == null) {
            throw new IllegalArgumentException("Ebaluazioa aukeratu behar da.");
        }
        return lortuAukerak(koadernoa).stream()
                .filter(aukera -> aukera.zenbakia() == zenbakia)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Aukeratutako ebaluazioa ez da koaderno honetakoa."));
    }

    private void gehituBukaeraBaliozkoa(List<LocalDate> bukaerak, LocalDate hautagaia,
                                         LocalDate ikasturteHasiera, LocalDate ikasturteBukaera) {
        if (hautagaia == null || hautagaia.isBefore(ikasturteHasiera) || !hautagaia.isBefore(ikasturteBukaera)) {
            return;
        }
        if (!bukaerak.contains(hautagaia)) bukaerak.add(hautagaia);
    }

    private EbaluaketaAukera sortuAukera(int zenbakia,
                                         Egutegia egutegia,
                                         List<KoadernoOrdutegiBlokea> blokeak,
                                         LocalDate hasiera,
                                         LocalDate bukaera) {
        int klaseOrduak = koadernoKlaseEgunService.egunekoSlotak(egutegia, blokeak, hasiera, bukaera)
                .values().stream().mapToInt(Integer::intValue).sum();
        return new EbaluaketaAukera(zenbakia, zenbakia + ". ebaluazioa", hasiera, bukaera, klaseOrduak);
    }

    public record EbaluaketaAukera(
            int zenbakia,
            String izena,
            LocalDate hasieraData,
            LocalDate bukaeraData,
            int klaseOrduak) {
    }
}
