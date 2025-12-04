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
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Asistentzia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Asistentzia.AsistentziaEgoera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Ebaluaketa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EstatistikaEbaluazioan;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Jarduera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Programazioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa.SaioEgoera;
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

 // Tarte txiki bat adierazteko record eroso bat
    private record DateRange(LocalDate from, LocalDate to) {}

    @Transactional(readOnly = true)
    public List<EstatistikaEbaluazioan> kalkulatuKoadernoarenEstatistikak(Koadernoa koadernoa) {
        if (koadernoa == null || koadernoa.getId() == null) {
            return List.of();
        }

        Egutegia egutegia = koadernoa.getEgutegia();
        Programazioa programazioa = programazioaRepository
                .findByKoadernoaId(koadernoa.getId())
                .orElse(null);

        // DB-n dauden estatistika-erregistroak (Koadernoa sortzean sortu zenituen)
        List<EstatistikaEbaluazioan> estatistikak =
                estatRepo.findByKoadernoaIdOrderByEbaluazioMomentua_OrdenaAscIdAsc(koadernoa.getId());

        for (EstatistikaEbaluazioan est : estatistikak) {

            EbaluazioMomentua momentua = est.getEbaluazioMomentua();

            // Ebaluazio momentuaren araberako data-tartea
            DateRange tartea = tarteaEstatistiketarako(egutegia, est.getEbaluazioMomentua());
            
            // ---- UNITATEAK ----
            if (programazioa != null) {
                est.setUnitateakAurreikusiak(
                        kalkulatuUnitateakAurreikusiak(programazioa, tartea)
                );
                est.setUnitateakEmanda(
                        kalkulatuUnitateakEmanda(koadernoa, tartea)
                );
            } else {
                est.setUnitateakAurreikusiak(0);
                est.setUnitateakEmanda(0);
            }

            // ---- ORDUAK ----
            est.setOrduakAurreikusiak(
                    kalkulatuOrduakAurreikusiak(koadernoa, tartea)
            );
            est.setOrduakEmanda(
                    kalkulatuOrduakEmanda(koadernoa, tartea)
            );

            // ---- IKASLEAK ----
            est.setEbaluatuak(kalkulatuEbaluatuak(koadernoa));

            // ---- APROBATUAK ----
            est.setAprobatuak(
                    kalkulatuAprobatuak(koadernoa, tartea)
            );

            // ---- HUTSEGITE ORDUAK ----
            est.setHutsegiteOrduak(
                kalkulatuHutsegiteOrduak(koadernoa, tartea)
            );
        }

        return estatistikak;
    }

    // ============================================================
    //  LAGUNTZAILEAK
    // ============================================================


    /** Ebaluaketa baten tartea Estatistika-tartearekin gainjartzen den ala ez. */
    private boolean ebTarteanDago(Ebaluaketa eb, DateRange tartea) {
        if (eb == null || tartea == null) return false;

        LocalDate start = eb.getHasieraData();
        LocalDate end   = eb.getBukaeraData();

        if (start == null && end == null) return true;
        if (start == null) start = tartea.from();
        if (end == null)   end   = tartea.to();

        return !end.isBefore(tartea.from()) && !start.isAfter(tartea.to());
    }

    /** Programazioan aurreikusitako UD kopurua EB tarte horretan. */
    private int kalkulatuUnitateakAurreikusiak(Programazioa p, DateRange tartea) {
        if (p == null || p.getEbaluaketak() == null) return 0;

        return (int) p.getEbaluaketak().stream()
                .filter(eb -> ebTarteanDago(eb, tartea))
                .flatMap(eb -> eb.getUnitateak().stream())
                .distinct()
                .count();
    }

    /**
     * Unitate “emanda” kopurua kalkulatu.
     *
     * Logika sinplea:
     *  - Tarte horretan Programazioan aurreikusitako UD kopurua hartzen dugu.
     *  - Tarte berean aurreikusitako orduak (saioak) eta benetan emandako orduak (jarduerak, planifikatua EZ direnak) hartzen ditugu.
     *  - emandakoPortzentaia = orduakEmanda / orduakAurreikusiak
     *  - unitateakEmanda = round(unitateakAurreikusiak * emandakoPortzentaia)
     *
     * Horrela, ez dugu UD bakoitza banaka jarraitu behar, baina emaitza nahiko
     * intuitiboa da: orduen zati bat emanda badago, unitateen zati proportzionala
     * "emanda" bezala zenbatzen dugu.
     */
    private int kalkulatuUnitateakEmanda(Koadernoa k, DateRange tartea) {
        if (k == null || k.getId() == null || tartea == null) return 0;

        // Programazioa berriz kargatu behar dugu, hemen ez baitugu param gisa
        Programazioa p = programazioaRepository
                .findByKoadernoaId(k.getId())
                .orElse(null);

        if (p == null) {
            return 0;
        }

        // Tarte horretan AURREIKUSITAKO UD kopurua
        int unitateakAurreikusiak = kalkulatuUnitateakAurreikusiak(p, tartea);
        if (unitateakAurreikusiak <= 0) {
            return 0;
        }

        // Tarte horretan AURREIKUSITAKO / EMANDAKO orduak
        int orduakAurreikusiak = kalkulatuOrduakAurreikusiak(k, tartea);
        int orduakEmanda       = kalkulatuOrduakEmanda(k, tartea);

        if (orduakAurreikusiak <= 0 || orduakEmanda <= 0) {
            return 0;
        }

        double ratio = (double) orduakEmanda / (double) orduakAurreikusiak;
        if (ratio <= 0) return 0;

        // Proportzionalki kalkulatu UD “emanda” kopurua
        double estim = unitateakAurreikusiak * ratio;
        int result = (int) Math.round(estim);

        // Ez dadila inoiz aurreikusitako kopurua baino handiagoa izan
        if (result > unitateakAurreikusiak) {
            result = unitateakAurreikusiak;
        }
        if (result < 0) {
            result = 0;
        }
        return result;
    }

    /** Ebaluazio tarte horretan aurreikusitako ordu kopurua (Saioak -> iraupenaSlot). */
    private int kalkulatuOrduakAurreikusiak(Koadernoa k, DateRange tartea) {
        if (k == null || k.getId() == null || tartea == null) return 0;

        return saioaRepository
                .findByKoadernoaIdAndDataBetweenOrderByDataAscHasieraSlotAsc(k.getId(), tartea.from(), tartea.to())
                .stream()
                .filter(s -> s.getEgoera() == null || s.getEgoera() == SaioEgoera.AKTIBOA)
                .mapToInt(Saioa::getIraupenaSlot)
                .sum();
    }

    /** Ebaluazio tarte horretan benetan emandako orduak (planifikatua EZ diren jarduerak). */
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

    /**
     * Ebaluatuak:
     *  - KOADERNO HONETAN MATRIKULATUTA dauden ikasle kopurua (MatrikulaEgoera.MATRIKULATUA)
     *  - tartea ez da erabiltzen (ebaluazio guztietan berdina agertuko da).
     */
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
     *  - Koaderno honetako MATRIKULATUA egoeran dauden matrikulak hartu
     *  - Ikasle bat "aprobatutzat" hartzen da bere noten artean
     *    gutxienez 5.0 (edo gehiago) duen nota bat badu.
     *  - Oraingoz tartea ez dugu erabiltzen.
     */
    private int kalkulatuAprobatuak(Koadernoa k, DateRange tartea) {
        if (k == null || k.getId() == null) return 0;

        List<Matrikula> matrikulak = matrikulaRepository
                .findByKoadernoa_IdAndEgoera(k.getId(), MatrikulaEgoera.MATRIKULATUA);

        int aprobatuak = 0;

        for (Matrikula m : matrikulak) {
            boolean ikasleAprobatua = m.getNotak().stream()
                    .map(EbaluazioNota::getNota)
                    .filter(Objects::nonNull)
                    .anyMatch(n -> n.doubleValue() >= 5.0);

            if (ikasleAprobatua) {
                aprobatuak++;
            }
        }
        return aprobatuak;
    }

    /**
     * Hutsegite orduak:
     *  - Tarte horretan dauden saio AKTIBO guztietan,
     *  - egoera HUTS edo JUSTIFIKATUA duten asistentzien orduak batu.
     *
     *  contribution = iraupenaSlot * HUTS/JUSTIFIKATUA kopurua saio horretan
     */
    private int kalkulatuHutsegiteOrduak(Koadernoa k, DateRange tartea) {
        if (k == null || k.getId() == null || tartea == null) return 0;

        // 1) Tarte horretako saio AKTIBO guztiak
        List<Saioa> saioak = saioaRepository
                .findByKoadernoaIdAndDataBetweenOrderByDataAscHasieraSlotAsc(k.getId(), tartea.from(), tartea.to())
                .stream()
                .filter(s -> s.getEgoera() == null || s.getEgoera() == SaioEgoera.AKTIBOA)
                .toList();

        if (saioak.isEmpty()) return 0;

        // 2) Saio-id -> iraupenaSlot
        Map<Long, Integer> iraupenaBySaioaId = saioak.stream()
                .collect(Collectors.toMap(
                        Saioa::getId,
                        Saioa::getIraupenaSlot
                ));

        List<Long> saioaIds = saioak.stream()
                .map(Saioa::getId)
                .toList();

        // 3) Saio horietako HUTS / JUSTIFIKATUA asistentziak
        List<Asistentzia> hutsak = asistentziaRepository
                .findBySaioa_IdInAndEgoeraIn(
                        saioaIds,
                        List.of(AsistentziaEgoera.HUTS, AsistentziaEgoera.JUSTIFIKATUA)
                );

        // 4) Guztizko hutsegite orduak
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
    
    private DateRange tarteaEstatistiketarako(Egutegia eg, EbaluazioMomentua em) {
        if (eg == null) {
            return new DateRange(LocalDate.MIN, LocalDate.MAX);
        }

        LocalDate has = eg.getHasieraData();
        LocalDate ama = eg.getBukaeraData();
        LocalDate lehen = eg.getLehenEbalBukaera();
        LocalDate bigarren = eg.getBigarrenEbalBukaera();

        // Segurtasun txiki bat (null kasuetarako fallback)
        has = (has != null) ? has : LocalDate.MIN;
        ama = (ama != null) ? ama : LocalDate.MAX;

        // 1) Checkbox-a markatuta badago → URTE OSOA
        if (em != null && Boolean.TRUE.equals(em.getUrteOsoa())) {
            return new DateRange(has, ama);
        }

        // 2) Bestela, kodearen arabera tartea
        String kodea = (em != null && em.getKodea() != null)
                ? em.getKodea().toUpperCase()
                : "";

        switch (kodea) {
            case "1_EBAL":
                return new DateRange(
                        has,
                        (lehen != null ? lehen : ama)
                );

            case "2_EBAL": {
                LocalDate from = (lehen != null ? lehen.plusDays(1) : has);
                LocalDate to   = (bigarren != null ? bigarren : ama);
                return new DateRange(from, to);
            }

            case "1_FINAL":
            case "2_FINAL": {
                // Finaletarako, checkbox-a markatu gabe badago ere:
                // bigarren ebaluaziotik bukaerara
                LocalDate from = (bigarren != null ? bigarren.plusDays(1) : has);
                return new DateRange(from, ama);
            }

            default:
                // Ezezaguna bada, defektuz ikasturte osoa
                return new DateRange(has, ama);
        }
    }
}
