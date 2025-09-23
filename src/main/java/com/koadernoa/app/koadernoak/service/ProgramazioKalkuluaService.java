package com.koadernoa.app.koadernoak.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.egutegia.entitateak.Astegunak;
import com.koadernoa.app.koadernoak.entitateak.JardueraPlanifikatua;
import com.koadernoa.app.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.koadernoak.entitateak.Ordutegia;
import com.koadernoa.app.koadernoak.entitateak.Programazioa;
import com.koadernoa.app.koadernoak.entitateak.SaioProposamenaDTO;
import com.koadernoa.app.koadernoak.entitateak.UnitateDidaktikoa;
import com.koadernoa.app.koadernoak.repository.OrdutegiaRepository;
import com.koadernoa.app.koadernoak.repository.ProgramazioaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProgramazioKalkuluaService {

    private final ProgramazioaRepository programazioaRepository;
    private final OrdutegiaRepository ordutegiaRepository;


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
    @Transactional(readOnly = true)
    public List<SaioProposamenaDTO> sortuSaioProposamenak(Koadernoa koadernoa, LocalDate hasieraData, Set<LocalDate> egunOporraldiak) {
        Programazioa p = programazioaRepository.findByKoadernoa(koadernoa)
                .orElseThrow(() -> new IllegalArgumentException("Koaderno honek ez du programaziorik"));

        List<Ordutegia> slotak = ordutegiaRepository.findByKoadernoaOrderByEgunaAscOrduHasieraAsc(koadernoa);
        if (slotak.isEmpty()) return List.of();

        // AsteEguna -> Ordutegi zerrenda
        Map<Astegunak, List<Ordutegia>> asteMapa = slotak.stream()
                .collect(Collectors.groupingBy(Ordutegia::getEguna, LinkedHashMap::new, Collectors.toList()));

        // Iterazio estatua: UD zerrenda (posizioz) eta barruan azpijarduerak
        List<UnitateDidaktikoa> udZerrenda = new ArrayList<>(p.getUnitateak());
        // ziurtatu ordena
        udZerrenda.sort(Comparator.comparingInt(UnitateDidaktikoa::getPosizioa).thenComparing(UnitateDidaktikoa::getId));

        int udIdx = 0;
        int udOrduFaltan = udZerrenda.isEmpty() ? 0 : udZerrenda.get(0).getOrduakEfektiboak();

        // Azpijarduera iterazioa
        List<JardueraPlanifikatua> ajZerrenda = udZerrenda.isEmpty() ? List.of() : new ArrayList<>(udZerrenda.get(0).getAzpiJarduerak());
        int ajIdx = 0;
        int ajOrduFaltan = !ajZerrenda.isEmpty() ? ajZerrenda.get(0).getOrduak() : 0;

        List<SaioProposamenaDTO> emaitza = new ArrayList<>();
        LocalDate data = hasieraData;

        while (udIdx < udZerrenda.size()) {
            if (egunOporraldiak != null && egunOporraldiak.contains(data)) {
                data = data.plusDays(1);
                continue;
            }

            Astegunak egunaEnum = toAsteEguna(data.getDayOfWeek());
            List<Ordutegia> egungoSlotak = asteMapa.getOrDefault(egunaEnum, List.of());
            if (egungoSlotak.isEmpty()) {
                data = data.plusDays(1);
                continue;
            }

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
                    // saio bat proposatu: slot-eko tarte osoa unitate/azpijarduera berarentzat
                    int orduHasiera = slot.getOrduHasiera() + kontsumitu;
                    int orduAmaiera = orduHasiera + hartu - 1;

                    emaitza.add(new SaioProposamenaDTO(
                            data, egunaEnum, orduHasiera, orduAmaiera,
                            ud.getKodea(), ud.getIzenburua(), ajIzen
                    ));

                    slotIraupena -= hartu;
                    kontsumitu += hartu;

                    // eguneratu faltan
                    if (!ajZerrenda.isEmpty() && ajIdx < ajZerrenda.size()) {
                        ajOrduFaltan -= hartu;
                        if (ajOrduFaltan <= 0) {
                            ajIdx++;
                            if (ajIdx < ajZerrenda.size()) {
                                ajOrduFaltan = ajZerrenda.get(ajIdx).getOrduak();
                            } else {
                                // azpijarduerak bukatuta → pasatu UD-ko gainerakora edo 0
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
                            ajOrduFaltan = !ajZerrenda.isEmpty() ? ajZerrenda.get(0).getOrduak() : 0;
                        }
                    }
                }
            }

            data = data.plusDays(1);
        }

        return emaitza;
    }

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
