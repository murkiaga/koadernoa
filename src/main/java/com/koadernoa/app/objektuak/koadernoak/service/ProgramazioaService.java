package com.koadernoa.app.objektuak.koadernoak.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunMota;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Ebaluaketa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.JardueraPlanifikatua;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoOrdutegiBlokea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Programazioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.UnitateDidaktikoa;
import com.koadernoa.app.objektuak.koadernoak.repository.EbaluaketaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.JardueraPlanifikatuaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.ProgramazioaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.UnitateDidaktikoaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProgramazioaService {

    private final ProgramazioaRepository programazioaRepository;
    private final UnitateDidaktikoaRepository udRepository;
    private final JardueraPlanifikatuaRepository jpRepository;
    private final EbaluaketaRepository ebaluaketaRepository;

    // ========= Programazioa =========
    @Transactional
    public Programazioa getOrCreateForKoadernoa(Koadernoa koadernoa) {
        return programazioaRepository.findByKoadernoa(koadernoa)
            .orElseGet(() -> programazioaRepository.save(
                new Programazioa(koadernoa, "Programazioa " + koadernoa.getIzena())
            ));
    }
    
 // ======== EBALUAKETA ========

    @Transactional(readOnly = true)
    public Ebaluaketa findEbaluaketa(Long id) { return ebaluaketaRepository.findById(id).orElseThrow(); }

    @Transactional(readOnly = true)
    public boolean ebalDagokioKoadernoari(Long ebalId, Long koadernoId) {
        if (ebalId == null || koadernoId == null) return false;
        return ebaluaketaRepository.existsByIdAndProgramazioa_Koadernoa_Id(ebalId, koadernoId);
    }

    @Transactional
    public Ebaluaketa addEbaluaketaForKoadernoa(Long koadernoId, String izena, LocalDate hasiera, LocalDate bukaera) {
        Programazioa p = programazioaRepository.findByKoadernoaId(koadernoId)
            .orElseThrow(() -> new IllegalArgumentException("Koadernoak ez du programaziorik: " + koadernoId));

        Ebaluaketa e = new Ebaluaketa();
        e.setProgramazioa(p);
        e.setIzena(izena != null && !izena.isBlank() ? izena : "Ebaluaketa");
        e.setHasieraData(hasiera);
        e.setBukaeraData(bukaera);
        // ordena = azkenaren ondoren
        Integer maxOrdena = p.getEbaluaketak() == null ? null :
            p.getEbaluaketak().stream().map(Ebaluaketa::getOrdena).filter(Objects::nonNull).max(Integer::compareTo).orElse(null);
        e.setOrdena(maxOrdena == null ? 1 : maxOrdena + 1);

        p.getEbaluaketak().add(e);
        return ebaluaketaRepository.save(e);
    }

    @Transactional
    public void updateEbaluaketa(Long ebalId, String izena, LocalDate hasiera, LocalDate bukaera) {
        Ebaluaketa e = ebaluaketaRepository.findById(ebalId).orElseThrow();
        if (izena != null) e.setIzena(izena);
        e.setHasieraData(hasiera);
        e.setBukaeraData(bukaera);
        ebaluaketaRepository.save(e);
    }

    @Transactional
    public void deleteEbaluaketa(Long ebalId) {
        Ebaluaketa e = ebaluaketaRepository.findById(ebalId).orElseThrow();
        // orphanRemoval=true bada UD-ak ere ezabatuko dira (hala definituta baduzu)
        ebaluaketaRepository.delete(e);
    }

    // ========= UD =========
    @Transactional
    public UnitateDidaktikoa addUdToEbaluaketa(Long ebaluaketaId, String kodea, String izenburua, int orduak, int posizioa) {
        Ebaluaketa eb = ebaluaketaRepository.findById(ebaluaketaId)
            .orElseThrow(() -> new IllegalArgumentException("Ebaluaketa ez da existitzen: " + ebaluaketaId));

        UnitateDidaktikoa ud = new UnitateDidaktikoa();
        ud.setEbaluaketa(eb);
        ud.setProgramazioa(eb.getProgramazioa()); // trantsizio baterako, bada oraindik eremua
        ud.setKodea(kodea);
        ud.setIzenburua(izenburua);
        ud.setOrduak(orduak);
        ud.setPosizioa(posizioa);

        eb.getUnitateak().add(ud); // @OrderBy posizioa,id
        return udRepository.save(ud);
    }

    @Transactional
    public void updateUd(Long udId, String kodea, String izenburua, int orduak, Integer posizioa) {
        var ud = udRepository.findById(udId).orElseThrow();
        ud.setKodea(kodea);
        ud.setIzenburua(izenburua);
        if (posizioa != null) ud.setPosizioa(posizioa);
        ud.setOrduak(orduak);         
        udRepository.save(ud);
    }

    @Transactional
    public void deleteUd(Long udId) {
        UnitateDidaktikoa ud = udRepository.findById(udId)
            .orElseThrow(() -> new IllegalArgumentException("UD ez da existitzen: " + udId));
        // orphanRemoval + cascade direla eta, azpi-jarduerak ere ezabatuko dira
        udRepository.delete(ud);
    }

    @Transactional
    public void addJardueraPlanifikatua(Long udId, String izenburua, int orduak) {
        var ud = udRepository.findById(udId).orElseThrow();
        var jp = new JardueraPlanifikatua();
        jp.setUnitatea(ud);
        jp.setIzenburua(izenburua);
        jp.setOrduak(orduak);
        jp.setPosizioa(ud.getAzpiJarduerak().size());
        jpRepository.save(jp);
    }

    @Transactional
    public void updateJardueraPlanifikatua(Long jpId, String izenburua, int orduak) {
        var jp = jpRepository.findById(jpId).orElseThrow();
        jp.setIzenburua(izenburua);
        jp.setOrduak(orduak);
        jpRepository.save(jp);
    }

    @Transactional
    public void deleteJardueraPlanifikatua(Long jpId) {
        var jp = jpRepository.findById(jpId).orElseThrow();
        jpRepository.delete(jp);
    }

    // ========= Ordena =========
    @Transactional
    public void reorderUd(Long koadernoId, List<Long> udIds) {
        if (udIds == null) return;
        for (int i = 0; i < udIds.size(); i++) {
            Long udId = udIds.get(i);
            if (!udDagokioKoadernoari(udId, koadernoId)) continue;
            var ud = udRepository.findById(udId).orElseThrow();
            ud.setPosizioa(i);
            udRepository.save(ud);
        }
    }

    @Transactional
    public void moveOrReorderJarduera(Long jpId, Long toUdId, int newIndex) {
        var jp = jpRepository.findById(jpId).orElseThrow();
        Long fromUdId = jp.getUnitatea().getId();

        var toUd = udRepository.findById(toUdId).orElseThrow();

        var fromList = new ArrayList<>(jp.getUnitatea().getAzpiJarduerak());
        fromList.removeIf(x -> x.getId().equals(jpId));

        var toList = new ArrayList<>(toUd.getAzpiJarduerak());
        toList.removeIf(x -> x.getId().equals(jpId));
        newIndex = Math.max(0, Math.min(newIndex, toList.size()));
        jp.setUnitatea(toUd);
        toList.add(newIndex, jp);

        for (int i = 0; i < toList.size(); i++) {
            toList.get(i).setPosizioa(i);
            jpRepository.save(toList.get(i));
        }
        for (int i = 0; i < fromList.size(); i++) {
            fromList.get(i).setPosizioa(i);
            jpRepository.save(fromList.get(i));
        }

        syncUdOrduak(fromUdId);
        syncUdOrduak(toUdId);
    }

    // ========= Finder-ak =========
    @Transactional(readOnly = true)
    public UnitateDidaktikoa findUd(Long id) { return udRepository.findById(id).orElseThrow(); }

    @Transactional(readOnly = true)
    public JardueraPlanifikatua findJarduera(Long id) { return jpRepository.findById(id).orElseThrow(); }

    @Transactional(readOnly = true)
    public boolean udDagokioKoadernoari(Long udId, Long koadernoId) {
        if (udId == null || koadernoId == null) return false;
        return udRepository.existsByIdAndEbaluaketa_Programazioa_Koadernoa_Id(udId, koadernoId);
    }

    @Transactional(readOnly = true)
    public boolean jpDagokioKoadernoari(Long jpId, Long koadernoId) {
        if (jpId == null || koadernoId == null) return false;
        return jpRepository.existsByIdAndUnitatea_Ebaluaketa_Programazioa_Koadernoa_Id(jpId, koadernoId);
    }

 // ========= Laguntzailea =========

    @Transactional
    private void syncUdOrduak(Long udId) {
        long count = jpRepository.countByUnitatea_Id(udId);
        if (count > 0) {
            // sum Long izan daiteke; null ez bada (COALESCE 0), baina badaezpada
            Long sum = jpRepository.sumOrduakByUdId(udId);
            int suma = sum == null ? 0 : sum.intValue();

            var ud = udRepository.findById(udId).orElseThrow();
            ud.setOrduak(suma);
            udRepository.save(ud);
        }
        // count == 0 -> EZ ukitu UD.orduak; eskuz kudeatzen jarraitzen du
    }

    @Transactional(readOnly = true)
    public int planifikatutakoOrduak(Long udId) {
        Long sum = jpRepository.sumOrduakByUdId(udId);
        return sum == null ? 0 : sum.intValue();
    }

    @Transactional(readOnly = true)
    public java.util.Map<Long, Integer> planifikatutakoOrduakMap(java.util.List<UnitateDidaktikoa> uds) {
        var map = new java.util.HashMap<Long, Integer>();
        if (uds == null || uds.isEmpty()) return map;

        // Java 11-friendly
        java.util.List<Long> ids = uds.stream()
            .map(UnitateDidaktikoa::getId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .collect(java.util.stream.Collectors.toList());

        if (ids.isEmpty()) return map;

        for (Object[] row : jpRepository.sumOrduakByUdIds(ids)) {
            Long udId = (Long) row[0];
            Number n  = (Number) row[1]; // Long normalean (COALESCE)
            map.put(udId, n == null ? 0 : n.intValue());
        }

        // faltan daudenak 0
        for (Long id : ids) {
            map.putIfAbsent(id, 0);
        }
        return map;
    }

    
    
    
    /**
     * Koaderno baten Programazioa kargatu:
     *  1) Programazioa (koadernoId bidez)
     *  2) Ebaluaketak + UD (JOIN FETCH)
     *  3) UD + JP (JOIN FETCH batchean, ID zerrendarekin)
     */
    @Transactional(readOnly = true)
    public Optional<Programazioa> loadWithEbaluaketakUdetajpByKoadernoId(Long koadernoId) {
        Optional<Programazioa> opt = programazioaRepository.findByKoadernoaId(koadernoId);
        if (opt.isEmpty()) return Optional.empty();

        Programazioa p = opt.get();

        // (1) EBAL + UD
        List<Ebaluaketa> ebals = ebaluaketaRepository.findAllWithUdByProgramazioaId(p.getId());
        p.setEbaluaketak(ebals);

        // (2) UD id-ak bildu JP batch-fetch egiteko
        List<Long> udIds = ebals.stream()
            .flatMap(e -> e.getUnitateak().stream())
            .map(UnitateDidaktikoa::getId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        // (3) UD + JP (batch)
        if (!udIds.isEmpty()) {
            List<UnitateDidaktikoa> full = udRepository.fetchUdWithJpByIds(udIds);
            Map<Long, UnitateDidaktikoa> byId = full.stream()
                .collect(Collectors.toMap(UnitateDidaktikoa::getId, u -> u));

            for (Ebaluaketa e : ebals) {
                List<UnitateDidaktikoa> replaced = e.getUnitateak().stream()
                    .map(u -> byId.getOrDefault(u.getId(), u))
                    .collect(Collectors.toList());
                e.setUnitateak(replaced);
            }
        }
        return Optional.of(p);
    }
    
	
    @Transactional(readOnly = true)
    public Map<Long, Integer> ebalOrduErabilgarriakBlokeekin(
            Programazioa programazioa,
            Egutegia egutegia,
            List<KoadernoOrdutegiBlokea> blokeak) {

        if (programazioa == null || egutegia == null || blokeak == null) return Map.of();

        Map<DayOfWeek, Integer> baseWeekHours = blokeak.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                b -> toDow(b.getAsteguna()),
                java.util.stream.Collectors.summingInt(KoadernoOrdutegiBlokea::getIraupenaSlot)
            ));

        return ebalOrduErabilgarriakCore(programazioa, egutegia, baseWeekHours);
    }
	
	/* ============ Core kalkulua (ORDEZKATUA konponduta) ============ */

    private Map<Long, Integer> ebalOrduErabilgarriakCore(
            Programazioa programazioa,
            Egutegia egutegia,
            Map<DayOfWeek, Integer> baseWeekHours) {

        Map<DayOfWeek, Integer> weekHours = new java.util.EnumMap<>(DayOfWeek.class);
        weekHours.putAll(baseWeekHours);
        applyOrdezkatuWeeklyRules(weekHours, egutegia.getEgunBereziak(), baseWeekHours);

        // EZ_LEKTIBOA/JAIEGUNA bakarrik baztertu
        java.util.Set<java.time.LocalDate> ezLektiboak = java.util.Optional.ofNullable(egutegia.getEgunBereziak())
            .orElse(java.util.List.of()).stream()
            .filter(eb -> eb.getMota() == EgunMota.JAIEGUNA || eb.getMota() == EgunMota.EZ_LEKTIBOA)
            .map(EgunBerezi::getData)
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());

        // ORDEZKATUA: “toDate”ri gehitzeko orduak
        java.util.Map<java.time.LocalDate, Integer> mapDailyOverrides = new java.util.HashMap<>();
        buildDateSpecificOverrides_ADD_ONLY(mapDailyOverrides, baseWeekHours, egutegia.getEgunBereziak());

        java.time.LocalDate egHas = egutegia.getHasieraData();
        java.time.LocalDate egBuk = egutegia.getBukaeraData();

        java.util.Map<Long, Integer> out = new java.util.LinkedHashMap<>();

        for (Ebaluaketa e : java.util.Optional.ofNullable(programazioa.getEbaluaketak()).orElse(java.util.List.of())) {
            java.time.LocalDate eHas = e.getHasieraData(), eBuk = e.getBukaeraData();
            if (eHas == null || eBuk == null) { out.put(e.getId(), 0); continue; }

            java.time.LocalDate start = max(eHas, egHas), end = min(eBuk, egBuk);
            if (start == null || end == null || start.isAfter(end)) { out.put(e.getId(), 0); continue; }

            int sum = 0;
            for (java.time.LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                java.time.DayOfWeek dow = d.getDayOfWeek();
                if (dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY) continue;
                if (ezLektiboak.contains(d)) continue;

                // Lehenik override-ak (adib. asteartea -> astelehena modura)
                Integer override = mapDailyOverrides.get(d);
                int base = weekHours.getOrDefault(dow, 0);
                sum += base + (override == null ? 0 : override);
            }
            out.put(e.getId(), sum);
        }
        return out;
    }
    
    
    /* ==================== LAGUNTZAILEAK ==================== */
    
    /** DATA ZEHAZTUKO ORDEZKAPENA:
     *  toDate (adib. asteartea) -> gehitu fromDow (adib. ASTELEHENA) asteko orduak.
     *  Oharra: ez dugu “fromDate” kendu; zure arauan bi egunak dira lektibo.
     */
    private void buildDateSpecificOverrides_ADD_ONLY(
            java.util.Map<java.time.LocalDate, Integer> mapDailyOverrides,
            Map<DayOfWeek, Integer> baseWeekHours,
            java.util.List<EgunBerezi> bereziak) {

        if (bereziak == null) return;

        for (EgunBerezi eb : bereziak) {
            if (eb.getMota() != EgunMota.ORDEZKATUA) continue;
            if (eb.getData() == null || eb.getOrdezkatua() == null) continue;

            java.time.LocalDate toDate  = eb.getData();
            java.time.DayOfWeek fromDow = toDow(eb.getOrdezkatua());

            int moved = baseWeekHours.getOrDefault(fromDow, 0);
            if (moved <= 0) continue;

            // Gehitu — ez kendu jatorrizkoa
            mapDailyOverrides.merge(toDate, moved, Integer::sum);
        }
    }

    private static <T> List<T> safeList(List<T> in) { return in == null ? List.of() : in; }

    private static LocalDate max(LocalDate a, LocalDate b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }
    private static LocalDate min(LocalDate a, LocalDate b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isBefore(b) ? a : b;
    }
    
    /** ASTE MAILAKO ORDEZKATUA: soilik eb.getData()==null denean (erregelak errepikakorrak direnean). */
    private void applyOrdezkatuWeeklyRules(
            Map<DayOfWeek, Integer> weekHours,
            java.util.List<EgunBerezi> bereziak,
            Map<DayOfWeek, Integer> baseWeekHours) {
        if (bereziak == null) return;

        for (EgunBerezi eb : bereziak) {
            if (eb.getMota() != EgunMota.ORDEZKATUA) continue;
            // ⚠️ Aste-mailakoa bakarrik: datarik EZ dagoenean
            if (eb.getData() != null || eb.getOrdezkatua() == null) continue;

            // Hemen ez dugu "to" zehazten; behar izanez gero beste eremu bat behar zenuke (toDow).
            // Une honetan aste-mailakoa EZ dugu aldatzen (bestela bikoizketa arriskua dago).
            // weekHours utzi berdin.
        }
    }
    
    /** DATA ZEHAZTUKO ORDEZKAPENA: eb.getData()!=null; fromDow -> toDate. */
    private void buildDateSpecificOverrides(
            java.util.Map<java.time.LocalDate, Integer> mapDailyOverrides,
            java.util.Set<java.time.LocalDate> blockedDates,
            Map<DayOfWeek, Integer> baseWeekHours,
            java.util.List<EgunBerezi> bereziak) {

        if (bereziak == null) return;

        for (EgunBerezi eb : bereziak) {
            if (eb.getMota() != EgunMota.ORDEZKATUA) continue;
            if (eb.getData() == null || eb.getOrdezkatua() == null) continue;

            java.time.LocalDate toDate  = eb.getData();                   // ORDEZKATUA markatutako data
            java.time.DayOfWeek fromDow = toDow(eb.getOrdezkatua());      // Jatorrizko asteguna (adib. ASTELEHENA)
            java.time.LocalDate fromDate = toDate.with(fromDow);          // Aste bereko jatorrizko eguna

            if (fromDate.equals(toDate)) continue;

            int moved = baseWeekHours.getOrDefault(fromDow, 0);
            if (moved <= 0) continue;

            blockedDates.add(fromDate);                    // jatorrizkoa kendu
            mapDailyOverrides.merge(toDate, moved, Integer::sum); // eta toDate-ri gehitu
        }
    }
    
       
    @Transactional
    public Programazioa createWithDefaultEbaluaketak(Koadernoa koadernoa) {
        // Programazioa sortu
        Programazioa p = new Programazioa(koadernoa, koadernoa.getIzena() != null ? koadernoa.getIzena() : "Programazioa");
        programazioaRepository.save(p);

        var eg = koadernoa.getEgutegia();
        if (eg == null || eg.getHasieraData() == null || eg.getBukaeraData() == null) {
            // Ebaluaketarik gabe utzi (editatuko duzu gero)
            return p;
        }

        // 1. → hasiera .. lehenEbalBukaera
        if (eg.getLehenEbalBukaera() != null) {
            Ebaluaketa e1 = new Ebaluaketa();
            e1.setIzena("1. Ebaluaketa");
            e1.setOrdena(1);
            e1.setHasieraData(eg.getHasieraData());
            e1.setBukaeraData(eg.getLehenEbalBukaera());
            e1.setProgramazioa(p);
            p.getEbaluaketak().add(e1);
        }
        // 2. → lehenBukaera+1 .. bigarrenBukaera
        if (eg.getLehenEbalBukaera() != null && eg.getBigarrenEbalBukaera() != null) {
            Ebaluaketa e2 = new Ebaluaketa();
            e2.setIzena("2. Ebaluaketa");
            e2.setOrdena(2);
            e2.setHasieraData(eg.getLehenEbalBukaera().plusDays(1));
            e2.setBukaeraData(eg.getBigarrenEbalBukaera());
            e2.setProgramazioa(p);
            p.getEbaluaketak().add(e2);
        }
        // 3. → bigarrenBukaera+1 .. bukaera
        if (eg.getBigarrenEbalBukaera() != null) {
            Ebaluaketa e3 = new Ebaluaketa();
            e3.setIzena("3. Ebaluaketa");
            e3.setOrdena(3);
            e3.setHasieraData(eg.getBigarrenEbalBukaera().plusDays(1));
            e3.setBukaeraData(eg.getBukaeraData());
            e3.setProgramazioa(p);
            p.getEbaluaketak().add(e3);
        }

        // Edge-cases: balio faltak → gutxienez 1 ebal
        if (p.getEbaluaketak().isEmpty()) {
            Ebaluaketa e = new Ebaluaketa();
            e.setIzena("1. Ebaluaketa");
            e.setOrdena(1);
            e.setHasieraData(eg.getHasieraData());
            e.setBukaeraData(eg.getBukaeraData());
            e.setProgramazioa(p);
            p.getEbaluaketak().add(e);
        }

        return programazioaRepository.save(p);
    }
    
    /** Astegunak (euskaraz) -> DayOfWeek (Java) */
    private static DayOfWeek toDow(Astegunak a) {
        return switch (a) {
            case ASTELEHENA -> DayOfWeek.MONDAY;
            case ASTEARTEA  -> DayOfWeek.TUESDAY;
            case ASTEAZKENA -> DayOfWeek.WEDNESDAY;
            case OSTEGUNA   -> DayOfWeek.THURSDAY;
            case OSTIRALA   -> DayOfWeek.FRIDAY;
            case LARUNBATA  -> DayOfWeek.SATURDAY;
            case IGANDEA    -> DayOfWeek.SUNDAY;
        };
    }
    
    public Optional<Programazioa> loadWithEbaluaketakByKoadernoId(Long koadernoId) {
        return programazioaRepository.findByKoadernoaIdFetchEbaluaketak(koadernoId);
    }
  
}