package com.koadernoa.app.objektuak.koadernoak.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioMomentua;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioNota;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Asistentzia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Asistentzia.AsistentziaEgoera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Ebaluaketa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EstatistikaEbaluazioan;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Jarduera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Programazioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa.SaioEgoera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.UnitateDidaktikoa;
import com.koadernoa.app.objektuak.koadernoak.repository.AsistentziaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.EstatistikaEbaluazioanRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.JardueraRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.ProgramazioaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.SaioaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.entitateak.MatrikulaEgoera;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstatistikaService {

    private final EstatistikaEbaluazioanRepository estatRepo;
    private final ProgramazioaRepository programazioaRepository;
    private final JardueraRepository jardueraRepository;
    private final SaioaRepository saioaRepository;
    private final MatrikulaRepository matrikulaRepository;
    private final AsistentziaRepository asistentziaRepository; // hutsegiteetarako, aurrerago

 // Tarte bat adierazteko record txiki bat
    private record DateRange(LocalDate from, LocalDate to) {}

    // ============================================================
    //  PUBLIKOAK: pantailarako eta berrkalkulurako
    // ============================================================

    /** Estatistika guztiak bueltatzen ditu koaderno honetarako (EZ du kalkulurik egiten). */
    @Transactional(readOnly = true)
    public List<EstatistikaEbaluazioan> kalkulatuKoadernoarenEstatistikak(Koadernoa koadernoa) {
        if (koadernoa == null || koadernoa.getId() == null) {
            return List.of();
        }
        return estatRepo.findByKoadernoaIdOrderByEbaluazioMomentua_OrdenaAscIdAsc(koadernoa.getId());
    }

    /**
     * Ebaluazio momentu bakar baten estatistika berriz kalkulatu (Berkalkulatu botoia).
     * - Aurreikusiak programazioaren arabera
     * - "emandako" balio batzuk kalkulu automatikoarekin (orduak) eta beste batzuk 0-ra (UDak)
     */
    @Transactional
    public void berkalkulatuEstatistika(Koadernoa koadernoa, Long estatistikaId) {
        if (koadernoa == null || koadernoa.getId() == null || estatistikaId == null) {
            return;
        }

        EstatistikaEbaluazioan est = estatRepo.findById(estatistikaId).orElse(null);
        if (est == null || est.getKoadernoa() == null ||
            !Objects.equals(est.getKoadernoa().getId(), koadernoa.getId())) {
            return; // ez dator bat koaderno honekin
        }

        Egutegia egutegia = koadernoa.getEgutegia();
        Programazioa programazioa = programazioaRepository
                .findByKoadernoaId(koadernoa.getId())
                .orElse(null);

        EbaluazioMomentua em = est.getEbaluazioMomentua();
        DateRange tartea = tarteaEbaluazioMomentua(egutegia, em);

        // ---------- UNITATEAK ----------
        if (programazioa != null) {
            int aurreikusUd = kalkulatuUnitateakAurreikusiak(programazioa, tartea);
            est.setUnitateakAurreikusiak(aurreikusUd);

            // Oraingoz: emandako UD = 0 (irakasleak eskuz jarriko du pantailan)
            int emandakoUd = kalkulatuUnitateakEmanda(programazioa, tartea);
            est.setUnitateakEmanda(emandakoUd);
        } else {
            est.setUnitateakAurreikusiak(0);
            est.setUnitateakEmanda(0);
        }

        // ---------- ORDUAK ----------
        int aurreikusOrdu = (programazioa != null)
                ? kalkulatuOrduakAurreikusiak(programazioa, tartea)
                : 0;
        est.setOrduakAurreikusiak(aurreikusOrdu);

        int emandakoOrdu = kalkulatuOrduakEmanda(koadernoa, tartea);
        est.setOrduakEmanda(emandakoOrdu);

        // ---------- IKASLEAK ----------
        int ebaluatuak = kalkulatuEbaluatuak(koadernoa);
        est.setEbaluatuak(ebaluatuak);

        int aprobatuak = kalkulatuAprobatuak(koadernoa, tartea);
        est.setAprobatuak(aprobatuak);

        // ---------- HUTSEGITE ORDUAK ----------
        int hutsOrdu = kalkulatuHutsegiteOrduak(koadernoa, tartea);
        est.setHutsegiteOrduak(hutsOrdu);

        estatRepo.save(est);
    }

    // ============================================================
    //  TARTEAK: EbaluazioMomentua -> DateRange
    // ============================================================

    private DateRange tarteaEbaluazioMomentua(Egutegia eg, EbaluazioMomentua em) {
        if (eg == null || em == null) {
            return new DateRange(LocalDate.MIN, LocalDate.MAX);
        }

        LocalDate ikasHas = eg.getHasieraData();
        LocalDate ikasAma = eg.getBukaeraData();
        LocalDate lehenBuk = eg.getLehenEbalBukaera();
        LocalDate bigarrenBuk = eg.getBigarrenEbalBukaera();

        if (ikasHas == null || ikasAma == null) {
            return new DateRange(LocalDate.MIN, LocalDate.MAX);
        }

        // Checkbox berria: "urte osoko datuak?"
        if (Boolean.TRUE.equals(em.getUrteOsoa())) {
            return new DateRange(ikasHas, ikasAma);
        }

        String kodea = em.getKodea();

        if ("1_EBAL".equalsIgnoreCase(kodea)) {
            LocalDate to = (lehenBuk != null ? lehenBuk : ikasAma);
            return new DateRange(ikasHas, to);
        }

        if ("2_EBAL".equalsIgnoreCase(kodea)) {
            LocalDate from = (lehenBuk != null ? lehenBuk.plusDays(1) : ikasHas);
            LocalDate to = (bigarrenBuk != null ? bigarrenBuk : ikasAma);
            return new DateRange(from, to);
        }

        if ("3_EBAL".equalsIgnoreCase(kodea)) {
            LocalDate from;
            if (bigarrenBuk != null) {
                from = bigarrenBuk.plusDays(1);
            } else if (lehenBuk != null) {
                from = lehenBuk.plusDays(1);
            } else {
                from = ikasHas;
            }
            return new DateRange(from, ikasAma);
        }

        // Default: ikasturte osoa
        return new DateRange(ikasHas, ikasAma);
    }

    /** Ebaluaketa baten tartea DateRangearekin gainjartzen den ala ez. */
    private boolean ebTarteanDago(Ebaluaketa eb, DateRange tartea) {
        if (eb == null || tartea == null) return false;

        LocalDate start = eb.getHasieraData();
        LocalDate end   = eb.getBukaeraData();

        if (start == null && end == null) return true;

        if (start == null) start = tartea.from();
        if (end   == null) end   = tartea.to();

        return !end.isBefore(tartea.from()) && !start.isAfter(tartea.to());
    }

    // ============================================================
    //  UNITATEAK
    // ============================================================

    /** Programazioan aurreikusitako UD kopurua tarte honetan. */
    private int kalkulatuUnitateakAurreikusiak(Programazioa p, DateRange tartea) {
        if (p == null || tartea == null || p.getEbaluaketak() == null) return 0;

        return (int) p.getEbaluaketak().stream()
                .filter(eb -> ebTarteanDago(eb, tartea))
                .flatMap(eb -> eb.getUnitateak().stream())
                .map(UnitateDidaktikoa::getId) // ID bidez distinct
                .distinct()
                .count();
    }

    /**
     * Unitate “emanda” kopurua.
     *
     * Oraingo bertsio sinplean:
     *  - 0 jarri → irakasleak eskuz jarriko du pantailan.
     *  - Horrela, ez dugu kontraesanik: 0 ordu emanda → 0 UD emanda.
     */
    private int kalkulatuUnitateakEmanda(Programazioa p, DateRange tartea) {
        return 0;
    }

    // ============================================================
    //  ORDUAK
    // ============================================================

    /**
     * Orduak aurreikusiak:
     *  - Programaziotik (UD-en orduen batura) tarte horretan.
     *  - `urteOsoa = true` bada → programazio osoaren orduen batura.
     */
    private int kalkulatuOrduakAurreikusiak(Programazioa p, DateRange tartea) {
        if (p == null || tartea == null || p.getEbaluaketak() == null) return 0;

        return p.getEbaluaketak().stream()
                .filter(eb -> ebTarteanDago(eb, tartea))
                .flatMap(eb -> eb.getUnitateak().stream())
                .mapToInt(ud -> {
                    Integer orduak = ud.getOrduak(); // Integer dela suposatuz
                    return (orduak != null ? orduak : 0);
                })
                .sum();
    }

    /**
     * Orduak emanda:
     *  - Jardueretatik hartzen dira, tarte horretan,
     *  - `mota != 'planifikatua'` (edo null) diren jardueren orduen batura.
     */
    private int kalkulatuOrduakEmanda(Koadernoa k, DateRange tartea) {
        if (k == null || k.getId() == null || tartea == null) return 0;

        double total = jardueraRepository
                .findByKoadernoaIdAndDataBetweenOrderByDataAscIdAsc(
                        k.getId(), tartea.from(), tartea.to())
                .stream()
                .filter(j -> j.getMota() == null ||
                             !"planifikatua".equalsIgnoreCase(j.getMota()))
                .mapToDouble(Jarduera::getOrduak)
                .sum();

        return (int) Math.round(total);
    }

    // ============================================================
    //  IKASLE KOPURUAK
    // ============================================================

    /** Ebaluatuak = koaderno honetako MATRIKULATUA egoeran dauden matrikulak. */
    public int kalkulatuEbaluatuak(Koadernoa k) {
        if (k == null || k.getId() == null) {
            return 0;
        }
        long cnt = matrikulaRepository.countByKoadernoa_IdAndEgoera(
                k.getId(),
                MatrikulaEgoera.MATRIKULATUA
        );
        return (int) cnt;
    }

    /**
     * Aprobatu kopurua:
     *  - Koaderno honetako MATRIKULATUA egoeran dauden matrikulak hartu.
     *  - Ikaslea "aprobatua" da bere noten artean >= 5.0 duen NOTA ZENBAKIZKOREN bat badu.
     *  - Oraingoz ez dugu tartea zorrotz erabiltzen; nahi baduzu gero filtro hori findu daiteke.
     */
    private int kalkulatuAprobatuak(Koadernoa k, DateRange tartea) {
        if (k == null || k.getId() == null) return 0;

        List<Matrikula> matrikulak = matrikulaRepository
                .findByKoadernoa_IdAndEgoera(k.getId(), MatrikulaEgoera.MATRIKULATUA);

        int aprobatuak = 0;

        for (Matrikula m : matrikulak) {
            boolean ikasleAprobatua = m.getNotak().stream()
                    .map(EbaluazioNota::getNota) // Number / BigDecimal / Float...
                    .filter(Objects::nonNull)
                    .anyMatch(n -> n.doubleValue() >= 5.0);

            if (ikasleAprobatua) {
                aprobatuak++;
            }
        }
        return aprobatuak;
    }

    // ============================================================
    //  HUTSEGITE ORDUAK
    // ============================================================

    /**
     * Hutsegite orduak:
     *  - Tarte horretako saio AKTIBO guztietan,
     *  - egoera HUTS edo JUSTIFIKATUA duten asistentziak,
     *  - Saio bakoitzean: iraupenaSlot * huts asistentzia kopurua.
     */
    private int kalkulatuHutsegiteOrduak(Koadernoa k, DateRange tartea) {
        if (k == null || k.getId() == null || tartea == null) return 0;

        List<Saioa> saioak = saioaRepository
                .findByKoadernoa_IdAndDataBetweenAndEgoera(
                        k.getId(),
                        tartea.from(),
                        tartea.to(),
                        SaioEgoera.AKTIBOA
                );

        if (saioak.isEmpty()) return 0;

        Map<Long, Integer> iraupenaBySaioaId = saioak.stream()
                .collect(Collectors.toMap(
                        Saioa::getId,
                        Saioa::getIraupenaSlot
                ));

        List<Long> saioaIds = saioak.stream()
                .map(Saioa::getId)
                .toList();

        List<Asistentzia> hutsak = asistentziaRepository
                .findBySaioa_IdInAndEgoeraIn(
                        saioaIds,
                        List.of(AsistentziaEgoera.HUTS, AsistentziaEgoera.JUSTIFIKATUA)
                );

        int totalHutsegiteOrduak = 0;
        for (Asistentzia a : hutsak) {
            Long saioaId = a.getSaioa().getId();
            Integer iraupena = iraupenaBySaioaId.get(saioaId);
            if (iraupena != null) {
                totalHutsegiteOrduak += iraupena;
            }
        }

        return totalHutsegiteOrduak;
    }
    
	 // Koaderno honetako estatistika-lerro GUZTIAK lortzeko metodo sinplea.
	 // Controllerrek erabiltzen du bai GET pantailarako, bai POST eguneratzeko.
	 public List<EstatistikaEbaluazioan> lortuKoadernoarenEstatistikak(Koadernoa koadernoa) {
	     if (koadernoa == null || koadernoa.getId() == null) {
	         return List.of();
	     }
	     return estatRepo.findByKoadernoaIdOrderByEbaluazioMomentua_OrdenaAscIdAsc(koadernoa.getId());
	 }
}
