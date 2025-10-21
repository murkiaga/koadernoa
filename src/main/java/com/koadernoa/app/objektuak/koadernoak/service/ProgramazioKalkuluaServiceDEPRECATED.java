package com.koadernoa.app.objektuak.koadernoak.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.koadernoak.entitateak.JardueraPlanifikatua;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Programazioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.SaioProposamenaDTO;
import com.koadernoa.app.objektuak.koadernoak.entitateak.UnitateDidaktikoa;
import com.koadernoa.app.objektuak.koadernoak.repository.ProgramazioaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProgramazioKalkuluaServiceDEPRECATED {

    private final ProgramazioaRepository programazioaRepository;
    //private final OrdutegiaRepository ordutegiaRepository;


    /** 
     * Programazio osoaren iraupena (ordu) — UD eta azpijarduerak kontuan hartuta.
     */
    @Transactional(readOnly = true)
    public int kalkulatuOrduTotala(Koadernoa koadernoa) {
        Programazioa p = programazioaRepository.findByKoadernoa(koadernoa)
                .orElseThrow(() -> new IllegalArgumentException("Koaderno honek ez du programaziorik"));
        return p.getOrduTotala();
    }

    /**
     * Denboralizaziorako “saio” proposamenak sortu.
     * @param koadernoa Koadernoa
     * @param hasieraData Noiztik hasi planifikatzea (adib. ikasturte hasiera)
     * @param egunOporraldiak Aukerakoa: saiorik egongo ez den datak (jai/ez-lektibo)
     */
    /**
    @Transactional(readOnly = true)
    public List<SaioProposamenaDTO> sortuSaioProposamenak(Koadernoa koadernoa, LocalDate hasieraData, Set<LocalDate> egunOporraldiak) {
        Programazioa p = programazioaRepository.findByKoadernoa(koadernoa)
                .orElseThrow(() -> new IllegalArgumentException("Koaderno honek ez du programaziorik"));

        List<Ordutegia> slotak = ordutegiaRepository.findByKoadernoaOrderByEgunaAscOrduHasieraAsc(koadernoa);
        if (slotak.isEmpty()) return List.of();

        // AsteEguna -> Ordutegi zerrenda
        Map<Astegunak, List<Ordutegia>> asteMapa = slotak.stream()
                .collect(Collectors.groupingBy(Ordutegia::getEguna, LinkedHashMap::new, Collectors.toList()));

        // ✅ EBAL → UD flatten (EBAL.ordena → UD.posizioa → UD.id)
        List<UnitateDidaktikoa> udZerrenda =
            p.getEbaluaketak() == null ? List.of()
            : p.getEbaluaketak().stream()
                .sorted(Comparator.comparing(e -> Optional.ofNullable(((com.koadernoa.app.objektuak.koadernoak.entitateak.Ebaluaketa)e).getOrdena()).orElse(0)))
                .flatMap(e -> e.getUnitateak() == null ? java.util.stream.Stream.<UnitateDidaktikoa>empty()
                                                        : e.getUnitateak().stream())
                .sorted(Comparator
                    .comparingInt(UnitateDidaktikoa::getPosizioa)
                    .thenComparing(UnitateDidaktikoa::getId))
                .collect(Collectors.toList());

        int udIdx = 0;
        int udOrduFaltan = udZerrenda.isEmpty() ? 0 : udZerrenda.get(0).getOrduakEfektiboak();

        // Azpijarduerak UD bakoitzean
        List<JardueraPlanifikatua> ajZerrenda = udZerrenda.isEmpty() ? List.of() : new ArrayList<>(udZerrenda.get(0).getAzpiJarduerak());
        int ajIdx = 0;
        int ajOrduFaltan = !ajZerrenda.isEmpty() ? Optional.ofNullable(ajZerrenda.get(0).getOrduak()).orElse(0) : 0;

        List<SaioProposamenaDTO> emaitza = new ArrayList<>();
        LocalDate data = hasieraData;

        while (udIdx < udZerrenda.size()) {
            if (egunOporraldiak != null && egunOporraldiak.contains(data)) { data = data.plusDays(1); continue; }

            Astegunak egunaEnum = toAsteEguna(data.getDayOfWeek());
            List<Ordutegia> egungoSlotak = asteMapa.getOrDefault(egunaEnum, List.of());
            if (egungoSlotak.isEmpty()) { data = data.plusDays(1); continue; }

            for (Ordutegia slot : egungoSlotak) {
                if (udIdx >= udZerrenda.size()) break;

                int slotIraupena = slot.getIraupenaOrdutan();
                int kontsumitu = 0;

                while (slotIraupena > 0 && udIdx < udZerrenda.size()) {
                    UnitateDidaktikoa ud = udZerrenda.get(udIdx);
                    String ajIzen = null;

                    int beharra;
                    if (!ajZerrenda.isEmpty() && ajIdx < ajZerrenda.size()) {
                        JardueraPlanifikatua aj = ajZerrenda.get(ajIdx);
                        beharra = ajOrduFaltan;
                        ajIzen = aj.getIzenburua();
                    } else {
                        beharra = udOrduFaltan;
                    }

                    int hartu = Math.min(slotIraupena, beharra);
                    int orduHasiera = slot.getOrduHasiera() + kontsumitu;
                    int orduAmaiera = orduHasiera + hartu - 1;

                    emaitza.add(new SaioProposamenaDTO(
                            data, egunaEnum, orduHasiera, orduAmaiera,
                            ud.getKodea(), ud.getIzenburua(), ajIzen
                    ));

                    slotIraupena -= hartu;
                    kontsumitu += hartu;

                    if (!ajZerrenda.isEmpty() && ajIdx < ajZerrenda.size()) {
                        ajOrduFaltan -= hartu;
                        if (ajOrduFaltan <= 0) {
                            ajIdx++;
                            if (ajIdx < ajZerrenda.size()) {
                                ajOrduFaltan = Optional.ofNullable(ajZerrenda.get(ajIdx).getOrduak()).orElse(0);
                            } else {
                                ajOrduFaltan = 0;
                            }
                        }
                    }
                    udOrduFaltan -= hartu;

                    if (udOrduFaltan <= 0) {
                        // Hurrengo UD-ra
                        udIdx++;
                        if (udIdx < udZerrenda.size()) {
                            UnitateDidaktikoa hurrengoa = udZerrenda.get(udIdx);
                            udOrduFaltan = hurrengoa.getOrduakEfektiboak();
                            ajZerrenda = new ArrayList<>(hurrengoa.getAzpiJarduerak());
                            ajIdx = 0;
                            ajOrduFaltan = !ajZerrenda.isEmpty()
                                    ? Optional.ofNullable(ajZerrenda.get(0).getOrduak()).orElse(0)
                                    : 0;
                        }
                    }
                }
            }
            data = data.plusDays(1);
        }
        return emaitza;
    }
    */

    private static Astegunak toAsteEguna(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> Astegunak.ASTELEHENA;
            case TUESDAY -> Astegunak.ASTEARTEA;
            case WEDNESDAY -> Astegunak.ASTEAZKENA;
            case THURSDAY -> Astegunak.OSTEGUNA;
            case FRIDAY -> Astegunak.OSTIRALA;
            case SATURDAY -> Astegunak.LARUNBATA;
            case SUNDAY -> Astegunak.IGANDEA;
        };
    }
}
