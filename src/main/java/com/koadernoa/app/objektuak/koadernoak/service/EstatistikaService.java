package com.koadernoa.app.objektuak.koadernoak.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    
    public long countGuztira(){ return estatRepo.countGuztira(); } //KENDU
    public long countIkasturteAktiboan(){ return estatRepo.countIkasturteAktiboan(); } //KENDU

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

    @Transactional(readOnly = true)
    public EstatistikaEbaluazioan lortuKoadernoarenEstatistika(Koadernoa koadernoa, Long estatistikaId) {
        if (koadernoa == null || koadernoa.getId() == null || estatistikaId == null) {
            return null;
        }
        EstatistikaEbaluazioan est = estatRepo.findById(estatistikaId).orElse(null);
        if (est == null || est.getKoadernoa() == null ||
                !Objects.equals(est.getKoadernoa().getId(), koadernoa.getId())) {
            return null;
        }
        return est;
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
        int ebaluatuak = kalkulatuEbaluatuak(koadernoa, em);
        est.setEbaluatuak(ebaluatuak);

        int aprobatuak = kalkulatuAprobatuak(koadernoa, em);
        est.setAprobatuak(aprobatuak);

        // ---------- HUTSEGITE ORDUAK ----------
        int hutsOrdu = kalkulatuHutsegiteOrduak(koadernoa, tartea);
        est.setHutsegiteOrduak(hutsOrdu);

        est.setKalkulatua(true);
        est.setAzkenKalkulua(LocalDateTime.now());
        
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
     * Hurbilpen berria (kontserbadoreagoa):
     *  - Tarte horretako jarduera NO-PLANIFIKATUAK hartzen ditugu.
     *  - Jarduera bakoitzean, deskribapenaren hasierako zatiak hartzen ditugu
     *    (lehen "—" arte): "2UD — (UD orokorra)" -> "2UD".
     *  - UD bat emanda kontsideratzen da bere kodea (ud.getKodea())
     *    jardueretatik ateratako kode horien multzoan badago.
     */
    private int kalkulatuUnitateakEmanda(Programazioa p, DateRange tartea) {
        if (p == null || tartea == null || p.getEbaluaketak() == null) {
            return 0;
        }

        Koadernoa k = p.getKoadernoa();
        if (k == null || k.getId() == null) {
            return 0;
        }

        // Tarte horretako jarduera NO-PLANIFIKATUAK
        List<Jarduera> jarduerakNoPlan = jardueraRepository
                .findByKoadernoaIdAndDataBetweenOrderByDataAscIdAsc(
                        k.getId(),
                        tartea.from(),
                        tartea.to()
                ).stream()
                .filter(j -> j.getMota() == null ||
                             !"planifikatua".equalsIgnoreCase(j.getMota()))
                .toList();

        if (jarduerakNoPlan.isEmpty()) {
            return 0;
        }

        // Jardueretatik ateratako "UD kode" multzoa (deskribapenaren lehenengo zatia, "—" arte)
        Set<String> jardueraUdKodeak = jarduerakNoPlan.stream()
                .map(j -> {
                    String base = j.getDeskribapena();
                    if (base == null || base.isBlank()) {
                        base = j.getTitulua();
                    }
                    if (base == null || base.isBlank()) {
                        return null;
                    }
                    String[] parts = base.split("—", 2);
                    String first = parts[0].trim();   // adib. "2UD"
                    if (first.isEmpty()) return null;
                    return first.toLowerCase();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (jardueraUdKodeak.isEmpty()) {
            return 0;
        }

        // Ebaluazio tarte honetan dauden UD-etatik, zenbat daude jarduera-kode multzo horretan
        Set<Long> emandakoUdIds = new java.util.HashSet<>();

        for (Ebaluaketa eb : p.getEbaluaketak()) {
            if (!ebTarteanDago(eb, tartea)) {
                continue;
            }

            for (UnitateDidaktikoa ud : eb.getUnitateak()) {
                if (ud == null || ud.getId() == null) continue;
                if (emandakoUdIds.contains(ud.getId())) continue;

                String kodea = ud.getKodea();
                if (kodea == null || kodea.isBlank()) {
                    continue; // kode barik -> ez dugu detektatzen heuristikan
                }

                String kodeLower = kodea.toLowerCase();

                if (jardueraUdKodeak.contains(kodeLower)) {
                    emandakoUdIds.add(ud.getId());
                }
            }
        }

        return emandakoUdIds.size();
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

    /**
     * Ebaluatuak kalkulatu:
     *
     *  - em.getUrteOsoa() == true:
     *      Koaderno honetako MATRIKULATUA egoeran dauden matrikulak (orain arte bezala).
     *
     *  - em.getUrteOsoa() == false:
     *      Ebaluazio momentu HONETAN nota zenbakizkoa (nota != null) duten matrikulak bakarrik.
     *      EZ dira zenbatzen "EZ_AURKEZTUA", "EZ_EBALUATUA_FALTAK" eta abar,
     *      normalean nota = null dutelako.
     */
    public int kalkulatuEbaluatuak(Koadernoa k, EbaluazioMomentua em) {
        if (k == null || k.getId() == null) {
            return 0;
        }

        // Ebaluazio momentua falta bada → fallback: guztira matrikulatuak
        if (em == null || em.getId() == null) {
            long cnt = matrikulaRepository.countByKoadernoa_IdAndEgoera(
                    k.getId(),
                    MatrikulaEgoera.MATRIKULATUA
            );
            return (int) cnt;
        }

        // URTE OSOKO momentua → logika zaharra: MATRIKULATUTA dauden guztiak
        if (Boolean.TRUE.equals(em.getUrteOsoa())) {
            long cnt = matrikulaRepository.countByKoadernoa_IdAndEgoera(
                    k.getId(),
                    MatrikulaEgoera.MATRIKULATUA
            );
            return (int) cnt;
        }

        // Bestela: ebaluazio momentu honetarako nota zenbakizkoa dutenak bakarrik
        List<Matrikula> matrikulak = matrikulaRepository
                .findByKoadernoa_IdAndEgoera(k.getId(), MatrikulaEgoera.MATRIKULATUA);

        int ebaluatuak = 0;

        for (Matrikula m : matrikulak) {
            if (m.getNotak() == null || m.getNotak().isEmpty()) {
                continue;
            }

            boolean duNotaZenbakizkoaMomentuHonetan = m.getNotak().stream()
                    .filter(n -> n.getEbaluazioMomentua() != null &&
                                 Objects.equals(n.getEbaluazioMomentua().getId(), em.getId()))
                    .map(EbaluazioNota::getNota)
                    .anyMatch(Objects::nonNull); // nota != null → zenbakizkoa

            if (duNotaZenbakizkoaMomentuHonetan) {
                ebaluatuak++;
            }
        }

        return ebaluatuak;
    }

    /**
     * Aprobatu kopurua ebaluazio momentu zehatz baterako.
     *
     * - Koaderno honetako MATRIKULATUA egoeran dauden matrikulak hartzen ditu.
     * - Ikasle bat "aprobatua" da EBALUAZIO MOMENTU HONETAN
     *   bere noten artean (momentu horretakoetan) >= 5.0 duen nota zenbakizkorik badu.
     * - Beste ebaluazio momentuetako notak EZ dira kontuan hartzen.
     */
    private int kalkulatuAprobatuak(Koadernoa k, EbaluazioMomentua em) {
        if (k == null || k.getId() == null || em == null || em.getId() == null) {
            return 0;
        }

        List<Matrikula> matrikulak = matrikulaRepository
                .findByKoadernoa_IdAndEgoera(k.getId(), MatrikulaEgoera.MATRIKULATUA);

        int aprobatuak = 0;

        for (Matrikula m : matrikulak) {
            boolean ikasleAprobatua = m.getNotak().stream()
                    // Ebaluazio momentu HAU bakarrik
                    .filter(n -> n.getEbaluazioMomentua() != null &&
                                 Objects.equals(n.getEbaluazioMomentua().getId(), em.getId()))
                    // Nota zenbakizkoak bakarrik (EZ_AURKEZTUA -> nota null, beraz ez da sartzen)
                    .map(EbaluazioNota::getNota)
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
