package com.koadernoa.app.objektuak.koadernoak.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.egutegia.repository.EgutegiaRepository;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KoadernoAutomatikoSorreraService {

    private final IkasturteaRepository ikasturteaRepository;
    private final EgutegiaRepository egutegiaRepository;
    private final ModuloaRepository moduloaRepository;
    private final KoadernoaRepository koadernoaRepository;
    private final KoadernoaService koadernoaService;

    @Transactional
    public Emaitza sortuFaltaDirenKoadernoak(Long ikasturteaId) {
        Ikasturtea ikasturtea = ikasturteaRepository.findById(ikasturteaId)
                .orElseThrow(() -> new IllegalArgumentException("Ikasturtea ez da aurkitu."));

        if (!ikasturtea.isAktiboa()) {
            throw new IllegalStateException("Jasotako ikasturtea ez dago aktibo. Ez da koadernorik sortu.");
        }

        int sortuak = 0;
        int lehendikZeudenak = 0;
        int estatistikakSortuta = 0;
        List<Egutegia> egutegiak = egutegiaRepository.findByIkasturtea_Id(ikasturtea.getId());

        for (Egutegia egutegia : egutegiak) {
            Maila maila = egutegia.getMaila();
            if (maila == null || maila.getId() == null) {
                continue;
            }

            List<Moduloa> moduluak = moduloaRepository.findByMaila_IdAndAktiboTrue(maila.getId());
            for (Moduloa moduloa : moduluak) {
                if (moduloa.getId() == null) {
                    continue;
                }

                var existitzenDenKoadernoa = koadernoaRepository
                        .findFirstByModuloa_IdAndEgutegia_IdOrderByIdAsc(moduloa.getId(), egutegia.getId());
                if (existitzenDenKoadernoa.isPresent()) {
                    lehendikZeudenak++;
                    if (koadernoaService.sortuEstatistikakFaltaBadira(existitzenDenKoadernoa.get()) > 0) {
                        estatistikakSortuta++;
                    }
                    continue;
                }

                Koadernoa koadernoa = new Koadernoa();
                koadernoa.setEgutegia(egutegia);
                koadernoa.setModuloa(moduloa);
                koadernoa.setIrakasleak(new ArrayList<>());
                koadernoa.setJabea(null);
                Koadernoa gordeta = koadernoaRepository.save(koadernoa);
                if (koadernoaService.sortuEstatistikakFaltaBadira(gordeta) > 0) {
                    estatistikakSortuta++;
                }
                sortuak++;
            }
        }

        estatistikakSortuta += koadernoaService.sortuFaltaDirenEstatistikakIkasturtean(ikasturtea.getId());

        return new Emaitza(sortuak, lehendikZeudenak, estatistikakSortuta);
    }

    public record Emaitza(int sortutakoak, int lehendikZeudenak, int estatistikakSortuta) {
    }
}
