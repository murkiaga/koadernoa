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
import com.koadernoa.app.objektuak.koadernoak.entitateak.Jarduera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.JardueraPlanifikatua;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoOrdutegiBlokea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Programazioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.UnitateDidaktikoa;
import com.koadernoa.app.objektuak.koadernoak.repository.EbaluaketaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.JardueraPlanifikatuaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
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
    private final KoadernoaRepository koadernoaRepository;
    private final DenboralizazioGeneratorService denboralizazioGeneratorService;

    // ========= Programazioa =========
    @Transactional
    public Programazioa getOrCreateForKoadernoa(Koadernoa koadernoa) {
        return programazioaRepository.findByKoadernoa(koadernoa)
            .orElseGet(() -> programazioaRepository.save(
                new Programazioa(koadernoa, "Programazioa " + koadernoa.getIzena())
            ));
    }
    
    // Laguntzaile pribatua kontroladorean bertan (edo service batean, nahi baduzu)
    public boolean isProgramazioaHutsik(Programazioa programazioa) {
        if (programazioa == null || programazioa.getEbaluaketak() == null) {
            // Ez dago programaziorik edo ebaluaketarik → hutsik
            return true;
        }

        // EB guztiek 0 UD badute → hutsik
        boolean badagoGutxienezUdBat = programazioa.getEbaluaketak().stream()
                .anyMatch(eb -> eb.getUnitateak() != null && !eb.getUnitateak().isEmpty());

        // Gutxienez UD bat egon ezean, hutsik
        return !badagoGutxienezUdBat;
    }
    
    /**
     * EB bakar batetik sortu denboralizazioa.
     * Aukerak:
     *  - Servicean metodo berria sortu (generateFromEbaluaketa)
     *  - Edo Programazioa "subprogramazio" bat eraiki EB bakar horrekin eta lehengo metodoa deitu.
     */
    public List<DenboralizazioGeneratorService.PreviewItem> bulkatuEbaluaketaBakarra(
            Koadernoa k,
            Programazioa p,
            Long ebaluaketaId,
            boolean replaceExisting
    ) {
        Ebaluaketa eb = p.getEbaluaketak().stream()
                .filter(e -> e.getId().equals(ebaluaketaId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ebaluaketa ez da aurkitu koaderno honetan."));

        // Programazio "txiki" bat muntatu EB bakarrarekin
        Programazioa sub = new Programazioa();
        sub.setKoadernoa(p.getKoadernoa());
        sub.setIzenburua(p.getIzenburua());
        sub.setAzalpena(p.getAzalpena());
        sub.setEbaluaketak(java.util.List.of(eb));

        return denboralizazioGeneratorService.generateFromProgramazioa(
                k, sub, /*preview*/ false, replaceExisting
        );
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
        balidatuUdOrduakEzDirelaGainditzen(udId, orduak, null, 0);
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
        balidatuJardueraOrduakEzDirelaGainditzen(udId, null, orduak);
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
        balidatuJardueraOrduakEzDirelaGainditzen(jp.getUnitatea().getId(), jpId, orduak);
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
    public void moveOrReorderUd(Long koadernoId, Long udId, Long toEbaluaketaId, int newIndex) {
        var ud = udRepository.findById(udId).orElseThrow();
        var toEbal = ebaluaketaRepository.findById(toEbaluaketaId).orElseThrow();

        // Segurtasuna: ebaluaketa helburua koaderno honetakoa dela ziurtatu
        if (!Objects.equals(
                toEbal.getProgramazioa().getKoadernoa().getId(),
                koadernoId
        )) {
            throw new IllegalArgumentException("Ebaluaketa ez dator koadernoarekin bat");
        }

        // 1) UDa target ebaluaketara mugitu (beharrezkoa bada)
        ud.setEbaluaketa(toEbal);

        // 2) Ebaluazio horretako UD zerrenda berria eraiki
        //    (UD hau barne, newIndex posizioan txertatuta)
        List<UnitateDidaktikoa> list = new ArrayList<>(toEbal.getUnitateak());
        // kendu aurreko posiziotik
        list.removeIf(x -> Objects.equals(x.getId(), ud.getId()));

        // indizea mugen barruan:
        newIndex = Math.max(0, Math.min(newIndex, list.size()));
        list.add(newIndex, ud);

        // 3) Zerrenda horretatik ID zerrenda atera eta reorderUd erabili
        List<Long> udIds = list.stream()
                .map(UnitateDidaktikoa::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        reorderUd(koadernoId, udIds);
    }
    
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
        balidatuJardueraOrduakEzDirelaGainditzen(toUdId, jpId, jp.getOrduak());

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
    
 // ========= Programazioa inportatu ========
       
    public Programazioa getProgramazioaForKoaderno(Koadernoa koadernoa) {
        return programazioaRepository.findByKoadernoa(koadernoa)
                                     .orElse(null);
    }
    
    @Transactional
    public void inportatuProgramazioa(Koadernoa iturburua, Koadernoa helburua) {

        // 1) EEI kodeak egiaztatu, badaudenean
        String srcEei = iturburua.getModuloa() != null ? iturburua.getModuloa().getEeiKodea() : null;
        String dstEei = helburua.getModuloa() != null ? helburua.getModuloa().getEeiKodea() : null;

        if (srcEei != null && dstEei != null && !srcEei.equals(dstEei)) {
            throw new IllegalStateException("EEI kodeak ez datoz bat; ezin da programazioa inportatu.");
        }

        // 2) Iturburuko eta helburuko programazioak lortu
        Programazioa src = getProgramazioaForKoaderno(iturburua);
        Programazioa dest = getProgramazioaForKoaderno(helburua);

        if (src == null || src.getEbaluaketak() == null || src.getEbaluaketak().isEmpty()) {
            throw new IllegalStateException("Iturburuko koadernoak ez du programaziorik (edo hutsik dago).");
        }

        // 3) Helburuko programazioa garbitu (ebaluaketak, UDak, jarduerak...)
        if (dest.getEbaluaketak() != null) {
            dest.getEbaluaketak().clear(); // orphanRemoval=true bada, hauek ezabatuko dira
        } else {
            dest.setEbaluaketak(new ArrayList<>());
        }

        //dest.setIzenburua(src.getIzenburua());
        //dest.setAzalpena(src.getAzalpena());

        Egutegia egutegia = helburua.getEgutegia();

        int ebIndex = 0;
        for (Ebaluaketa ebSrc : src.getEbaluaketak()) {
            if (ebSrc == null) continue;

            Ebaluaketa ebNew = new Ebaluaketa();
            ebNew.setProgramazioa(dest);
            ebNew.setIzena(ebSrc.getIzena());
            // ordena gordetzen dugu, ez badago, indizearekin
            ebNew.setOrdena(ebSrc.getOrdena() != null ? ebSrc.getOrdena() : ++ebIndex);

            // Ebaluaketa datak egutegira egokituta
            egokituEbaluaketaDatak(ebNew, ebNew.getOrdena(), egutegia);

            ebNew.setUnitateak(new ArrayList<>());

            // Hemen erabili zure UD klasearen izena
            // adibidez: UnitateDidaktikoa edo UnitateDidaktikoaProgramazioa...
            for (var udSrc : ebSrc.getUnitateak()) {
                if (udSrc == null) continue;

                UnitateDidaktikoa udNew = new UnitateDidaktikoa();
                
                udNew.setProgramazioa(dest);
                udNew.setEbaluaketa(ebNew);
                udNew.setPosizioa(udSrc.getPosizioa());
                udNew.setKodea(udSrc.getKodea());
                udNew.setIzenburua(udSrc.getIzenburua());
                udNew.setOrduak(udSrc.getOrduak());
                udNew.setAzpiJarduerak(new ArrayList<>());

                for (JardueraPlanifikatua jpSrc : udSrc.getAzpiJarduerak()) {
                    if (jpSrc == null) continue;

                    JardueraPlanifikatua jpNew = new JardueraPlanifikatua();
                    
                    jpNew.setUnitatea(udNew);
                    jpNew.setPosizioa(jpSrc.getPosizioa());
                    jpNew.setIzenburua(jpSrc.getIzenburua());
                    jpNew.setOrduak(jpSrc.getOrduak());

                    udNew.getAzpiJarduerak().add(jpNew);
                }

                ebNew.getUnitateak().add(udNew);
            }

            dest.getEbaluaketak().add(ebNew);
        }

        // dest jada "managed" dago (getProgramazioaForKoaderno-k DBtik ekarri du),
        // beraz normalean ez litzateke save behar, baina segurantzaz:
        programazioaRepository.save(dest);
    }

    private void egokituEbaluaketaDatak(Ebaluaketa eb,
                                        int ordena,
                                        Egutegia egutegia) {
        if (egutegia == null) return;

        var has = egutegia.getHasieraData();
        var buk = egutegia.getBukaeraData();
        var e1  = egutegia.getLehenEbalBukaera();
        var e2  = egutegia.getBigarrenEbalBukaera();

        if (has == null || buk == null) {
            return;
        }

        switch (ordena) {
            case 1 -> {
                eb.setHasieraData(has);
                eb.setBukaeraData(e1 != null ? e1 : buk);
            }
            case 2 -> {
                if (e1 == null) {
                    eb.setHasieraData(has);
                    eb.setBukaeraData(e2 != null ? e2 : buk);
                } else {
                    eb.setHasieraData(e1.plusDays(1));
                    eb.setBukaeraData(e2 != null ? e2 : buk);
                }
            }
            default -> {
                if (e2 == null) {
                    eb.setHasieraData(e1 != null ? e1.plusDays(1) : has);
                    eb.setBukaeraData(buk);
                } else {
                    eb.setHasieraData(e2.plusDays(1));
                    eb.setBukaeraData(buk);
                }
            }
        }
    }

 // ========= Laguntzailea =========


    private void balidatuJardueraOrduakEzDirelaGainditzen(Long udId, Long jpId, int orduakBerriak) {
        var ud = udRepository.findById(udId).orElseThrow();
        int oraingoBatura = planifikatutakoOrduak(udId);
        int jardueraZaharra = 0;
        if (jpId != null) {
            jardueraZaharra = jpRepository.findById(jpId)
                    .filter(jp -> jp.getUnitatea() != null && java.util.Objects.equals(jp.getUnitatea().getId(), udId))
                    .map(JardueraPlanifikatua::getOrduak)
                    .orElse(0);
        }
        int baturaBerria = oraingoBatura - jardueraZaharra + Math.max(0, orduakBerriak);
        balidatuUdOrduakEzDirelaGainditzen(udId, ud.getOrduak(), jpId, orduakBerriak);
        if (baturaBerria > ud.getOrduak()) {
            throw new IllegalArgumentException("jardueretan ordu gehiegi dituzu");
        }
    }

    private void balidatuUdOrduakEzDirelaGainditzen(Long udId, int udOrduakBerriak, Long jpId, int jpOrduakBerriak) {
        int oraingoBatura = planifikatutakoOrduak(udId);
        if (jpId != null) {
            int jardueraZaharra = jpRepository.findById(jpId)
                    .filter(jp -> jp.getUnitatea() != null && java.util.Objects.equals(jp.getUnitatea().getId(), udId))
                    .map(JardueraPlanifikatua::getOrduak)
                    .orElse(0);
            oraingoBatura = oraingoBatura - jardueraZaharra + Math.max(0, jpOrduakBerriak);
        }
        if (oraingoBatura > Math.max(0, udOrduakBerriak)) {
            throw new IllegalArgumentException("jardueretan ordu gehiegi dituzu");
        }
    }

    @Transactional
    private void syncUdOrduak(Long udId) {
        // UD.orduak erabiltzaileak ezarritako guztizko muga da; ez sinkronizatu automatikoki jardueren baturarekin.
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
        if (p.getEbaluaketak() == null) {
            p.setEbaluaketak(new ArrayList<>());
        } else {
            p.getEbaluaketak().clear();
        }
        p.getEbaluaketak().addAll(ebals);

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
                if (e.getUnitateak() == null) {
                    e.setUnitateak(new ArrayList<>());
                } else {
                    e.getUnitateak().clear();
                }
                e.getUnitateak().addAll(replaced);
            }
        }
        return Optional.of(p);
    }

    @Transactional
    public void syncDualUdForKoaderno(Long koadernoId) {
        if (koadernoId == null) return;
        Koadernoa k = koadernoaRepository.findByIdWithOrdutegiaAndEgutegia(koadernoId).orElse(null);
        if (k == null) return;
        Programazioa p = loadWithEbaluaketakUdetajpByKoadernoId(koadernoId)
                .orElseGet(() -> createWithDefaultEbaluaketak(k));
        syncDualUdForProgramazioa(k, p);
    }

    @Transactional
    public void syncDualUdForProgramazioa(Koadernoa koadernoa, Programazioa programazioa) {
        if (koadernoa == null || programazioa == null || programazioa.getEbaluaketak() == null) return;

        int dualOrduak = koadernoa.getModuloa() != null && koadernoa.getModuloa().getDualOrduak() != null
                ? koadernoa.getModuloa().getDualOrduak() : 0;

        List<LocalDate> dualHasierak = java.util.Optional.ofNullable(koadernoa.getOrdutegiak()).orElse(List.of()).stream()
                .filter(KoadernoOrdutegiBlokea::isDualOrdutegia)
                .map(b -> b.getHasieraData() != null ? b.getHasieraData()
                        : (koadernoa.getEgutegia() != null ? koadernoa.getEgutegia().getHasieraData() : null))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        List<LocalDate> ordutegiHasierak = java.util.Optional.ofNullable(koadernoa.getOrdutegiak()).orElse(List.of()).stream()
                .map(b -> b.getHasieraData() != null ? b.getHasieraData()
                        : (koadernoa.getEgutegia() != null ? koadernoa.getEgutegia().getHasieraData() : null))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        LocalDate ikastBukaera = koadernoa.getEgutegia() != null ? koadernoa.getEgutegia().getBukaeraData() : null;

        java.util.Set<String> activeCodes = new java.util.HashSet<>();
        for (LocalDate hasiera : dualHasierak) {
            LocalDate dualBuk = dualRangeEnd(hasiera, ordutegiHasierak, ikastBukaera);
            Ebaluaketa targetEbal = findBestEbaluaketaForRange(programazioa.getEbaluaketak(), hasiera, dualBuk);
            if (targetEbal == null) continue;

            String code = "DUAL-" + hasiera.toString().replace("-", "");
            activeCodes.add(code);
            if (targetEbal.getUnitateak() == null) {
                targetEbal.setUnitateak(new ArrayList<>());
            }

            UnitateDidaktikoa existing = findUdByKodeaInProgramazioa(programazioa, code);
            if (dualOrduak <= 0) continue;
            if (existing == null) {
                UnitateDidaktikoa ud = new UnitateDidaktikoa();
                ud.setProgramazioa(programazioa);
                ud.setEbaluaketa(targetEbal);
                ud.setKodea(code);
                ud.setIzenburua("DUALA");
                ud.setOrduak(dualOrduak);
                int maxPos = targetEbal.getUnitateak().stream().mapToInt(UnitateDidaktikoa::getPosizioa).max().orElse(0);
                ud.setPosizioa(maxPos + 1);
                targetEbal.getUnitateak().add(ud);
            } else {
                existing.setIzenburua("DUALA");
                existing.setOrduak(dualOrduak);
                if (!isSameEbaluaketa(existing.getEbaluaketa(), targetEbal)) {
                    existing.setEbaluaketa(targetEbal);
                    int maxPos = targetEbal.getUnitateak().stream()
                            .filter(u -> !Objects.equals(u.getId(), existing.getId()))
                            .mapToInt(UnitateDidaktikoa::getPosizioa)
                            .max().orElse(0);
                    existing.setPosizioa(maxPos + 1);
                }
            }
        }

        for (Ebaluaketa e : programazioa.getEbaluaketak()) {
            e.getUnitateak().removeIf(u -> {
                boolean dualKodea = u.getKodea() != null && u.getKodea().startsWith("DUAL-");
                return dualKodea && !activeCodes.contains(u.getKodea());
            });
        }
        programazioaRepository.save(programazioa);
    }
    
	
    private boolean isSameEbaluaketa(Ebaluaketa current, Ebaluaketa target) {
        if (current == target) return true;
        if (current == null || target == null || current.getId() == null || target.getId() == null) return false;
        return Objects.equals(current.getId(), target.getId());
    }

    private UnitateDidaktikoa findUdByKodeaInProgramazioa(Programazioa programazioa, String kodea) {
        if (programazioa == null || kodea == null || programazioa.getEbaluaketak() == null) {
            return null;
        }
        return programazioa.getEbaluaketak().stream()
                .filter(Objects::nonNull)
                .flatMap(e -> e.getUnitateak() == null ? java.util.stream.Stream.<UnitateDidaktikoa>empty() : e.getUnitateak().stream())
                .filter(u -> kodea.equals(u.getKodea()))
                .findFirst()
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> kalkulatuEbaluazioOrduak(Programazioa programazioa, Koadernoa koadernoa) {
        if (programazioa == null || koadernoa == null) {
            return Map.of();
        }

        Koadernoa ordutegidunKoadernoa = koadernoa;
        if (koadernoa.getId() != null) {
            ordutegidunKoadernoa = koadernoaRepository.findWithOrdutegiaById(koadernoa.getId())
                    .orElse(koadernoa);
        }

        return ebalOrduErabilgarriakBlokeekin(
                programazioa,
                ordutegidunKoadernoa.getEgutegia(),
                ordutegidunKoadernoa.getOrdutegiak());
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> ebalOrduErabilgarriakBlokeekin(
            Programazioa programazioa,
            Egutegia egutegia,
            List<KoadernoOrdutegiBlokea> blokeak) {

        if (programazioa == null || egutegia == null || blokeak == null) return Map.of();

        LocalDate ikastHas = egutegia.getHasieraData();
        java.util.NavigableMap<LocalDate, Map<Astegunak, Integer>> ordutegiaka = new java.util.TreeMap<>();
        java.util.List<LocalDate> ordutegiHasierak = new java.util.ArrayList<>();

        java.util.Set<LocalDate> dualHasierak = new java.util.HashSet<>();
        int dualOrduak = programazioa.getKoadernoa() != null
                && programazioa.getKoadernoa().getModuloa() != null
                && programazioa.getKoadernoa().getModuloa().getDualOrduak() != null
                ? programazioa.getKoadernoa().getModuloa().getDualOrduak() : 0;

        for (KoadernoOrdutegiBlokea b : blokeak) {
            LocalDate has = b.getHasieraData() != null ? b.getHasieraData() : ikastHas;
            ordutegiHasierak.add(has);
            if (b.isTarteHutsa()) {
                ordutegiaka.putIfAbsent(has, new java.util.EnumMap<>(Astegunak.class));
                continue;
            }
            if (b.getIraupenaSlot() <= 0 && !b.isDualOrdutegia()) {
                // Marker normala: ez du eguneroko ordu-kalkuluan eragin behar
                continue;
            }
            if (b.isDualOrdutegia()) {
                dualHasierak.add(has);
                continue;
            }
            if (b.getAsteguna() == null) continue;
            ordutegiaka.computeIfAbsent(has, __ -> new java.util.EnumMap<>(Astegunak.class))
                    .merge(b.getAsteguna(), b.getIraupenaSlot(), Integer::sum);
        }

        Map<Long, Integer> out = ebalOrduErabilgarriakCore(programazioa, egutegia, ordutegiaka);
        if (dualOrduak > 0 && !dualHasierak.isEmpty()) {
            List<LocalDate> hasieraOrdenatuak = ordutegiHasierak.stream().distinct().sorted().toList();
            Map<LocalDate, EgunBerezi> egunBereziakMap = java.util.Optional.ofNullable(egutegia.getEgunBereziak())
                    .orElse(java.util.List.of()).stream()
                    .filter(eb -> eb.getData() != null)
                    .collect(java.util.stream.Collectors.toMap(
                            EgunBerezi::getData,
                            eb -> eb,
                            (a, b) -> a
                    ));
            for (LocalDate dualHas : dualHasierak) {
                LocalDate dualBuk = dualRangeEnd(dualHas, hasieraOrdenatuak, egutegia.getBukaeraData());
                // 1) Dual tartean ordutegi arruntez kontatu diren orduak kendu
                Map<Long, Integer> kenketak = normalOrduakByEvalInRange(
                        programazioa, egutegia, ordutegiaka, egunBereziakMap, dualHas, dualBuk);
                kenketak.forEach((ebalId, ordu) -> out.merge(ebalId, -ordu, Integer::sum));

                // 2) Dagokion ebaluazioari DUAL ordu finkoak gehitu
                Ebaluaketa ebal = findBestEbaluaketaForRange(programazioa.getEbaluaketak(), dualHas, dualBuk);
                if (ebal == null) continue;
                out.merge(ebal.getId(), dualOrduak, Integer::sum);
            }
        }
        return out;
    }
	
	/* ============ Core kalkulua (ORDEZKATUA konponduta) ============ */

    private Map<Long, Integer> ebalOrduErabilgarriakCore(
            Programazioa programazioa,
            Egutegia egutegia,
            java.util.NavigableMap<LocalDate, Map<Astegunak, Integer>> ordutegiaka) {
        Map<LocalDate, EgunBerezi> egunBereziakMap = java.util.Optional.ofNullable(egutegia.getEgunBereziak())
            .orElse(java.util.List.of()).stream()
            .filter(eb -> eb.getData() != null)
            .collect(java.util.stream.Collectors.toMap(
                EgunBerezi::getData,
                eb -> eb,
                (a, b) -> a
            ));

        Map<LocalDate, Integer> egunekoOrduakIkasturtean = kalkulatuEgunekoOrduakIkasturtean(
            egutegia,
            ordutegiaka,
            egunBereziakMap
        );

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
                Astegunak astegunEraginkorra = astegunEraginkorra(d, egunBereziakMap);
                if (astegunEraginkorra == null) continue;

                sum += egunekoOrduakIkasturtean.getOrDefault(d, 0);
            }
            out.put(e.getId(), sum);
        }
        return out;
    }

    private Map<LocalDate, Integer> kalkulatuEgunekoOrduakIkasturtean(
            Egutegia egutegia,
            java.util.NavigableMap<LocalDate, Map<Astegunak, Integer>> ordutegiaka,
            Map<LocalDate, EgunBerezi> egunBereziakMap) {

        Map<LocalDate, Integer> emaitza = new java.util.LinkedHashMap<>();
        LocalDate ikastHasiera = egutegia.getHasieraData();
        LocalDate ikastBukaera = egutegia.getBukaeraData();

        if (ikastHasiera == null || ikastBukaera == null) {
            return emaitza;
        }

        for (LocalDate d = ikastHasiera; !d.isAfter(ikastBukaera); d = d.plusDays(1)) {
            Astegunak ag = astegunEraginkorra(d, egunBereziakMap);
            if (ag == null) continue;

            var egunekoOrdutegia = ordutegiaka.floorEntry(d) != null ? ordutegiaka.floorEntry(d).getValue() : Map.<Astegunak,Integer>of();
            int ordu = egunekoOrdutegia.getOrDefault(ag, 0);
            if (ordu > 0) {
                emaitza.put(d, ordu);
            }
        }
        return emaitza;
    }

    private Astegunak astegunEraginkorra(LocalDate data, Map<LocalDate, EgunBerezi> egunBereziakMap) {
        EgunBerezi eb = egunBereziakMap.get(data);
        if (eb != null) {
            EgunMota mota = eb.getMota();
            if (mota == EgunMota.JAIEGUNA || mota == EgunMota.EZ_LEKTIBOA) {
                return null;
            }
            if (mota == EgunMota.ORDEZKATUA && eb.getOrdezkatua() != null) {
                return eb.getOrdezkatua();
            }
        }

        DayOfWeek dow = data.getDayOfWeek();
        return switch (dow) {
            case MONDAY -> Astegunak.ASTELEHENA;
            case TUESDAY -> Astegunak.ASTEARTEA;
            case WEDNESDAY -> Astegunak.ASTEAZKENA;
            case THURSDAY -> Astegunak.OSTEGUNA;
            case FRIDAY -> Astegunak.OSTIRALA;
            default -> null;
        };
    }
    
    
    /* ==================== LAGUNTZAILEAK ==================== */

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

    private LocalDate dualRangeEnd(LocalDate dualHas, List<LocalDate> scheduleStarts, LocalDate ikastBukaera) {
        if (dualHas == null) return ikastBukaera;
        LocalDate next = scheduleStarts.stream()
                .filter(d -> d != null && d.isAfter(dualHas))
                .sorted()
                .findFirst()
                .orElse(null);
        if (next != null) return next.minusDays(1);
        return ikastBukaera != null ? ikastBukaera : dualHas;
    }

    private Ebaluaketa findBestEbaluaketaForRange(List<Ebaluaketa> ebaluaketak, LocalDate has, LocalDate buk) {
        if (ebaluaketak == null || has == null || buk == null) return null;
        long best = -1;
        Ebaluaketa bestEbal = null;
        for (Ebaluaketa e : ebaluaketak) {
            if (e.getHasieraData() == null || e.getBukaeraData() == null) continue;
            LocalDate s = has.isAfter(e.getHasieraData()) ? has : e.getHasieraData();
            LocalDate t = buk.isBefore(e.getBukaeraData()) ? buk : e.getBukaeraData();
            long overlap = s.isAfter(t) ? 0 : java.time.temporal.ChronoUnit.DAYS.between(s, t) + 1;
            if (overlap > best) {
                best = overlap;
                bestEbal = e;
            }
        }
        return best > 0 ? bestEbal : null;
    }

    private Map<Long, Integer> normalOrduakByEvalInRange(
            Programazioa programazioa,
            Egutegia egutegia,
            java.util.NavigableMap<LocalDate, Map<Astegunak, Integer>> ordutegiaka,
            Map<LocalDate, EgunBerezi> egunBereziakMap,
            LocalDate rangeHas,
            LocalDate rangeBuk) {

        Map<Long, Integer> out = new java.util.HashMap<>();
        if (rangeHas == null || rangeBuk == null || rangeHas.isAfter(rangeBuk)) return out;

        LocalDate ikastHas = egutegia.getHasieraData();
        LocalDate ikastBuk = egutegia.getBukaeraData();
        LocalDate has = max(rangeHas, ikastHas);
        LocalDate buk = min(rangeBuk, ikastBuk);
        if (has == null || buk == null || has.isAfter(buk)) return out;

        for (LocalDate d = has; !d.isAfter(buk); d = d.plusDays(1)) {
            Astegunak ag = astegunEraginkorra(d, egunBereziakMap);
            if (ag == null) continue;
            var ordutegia = ordutegiaka.floorEntry(d) != null ? ordutegiaka.floorEntry(d).getValue() : Map.<Astegunak, Integer>of();
            int ordu = ordutegia.getOrDefault(ag, 0);
            if (ordu <= 0) continue;

            Ebaluaketa ebal = null;
            for (Ebaluaketa e : java.util.Optional.ofNullable(programazioa.getEbaluaketak()).orElse(java.util.List.of())) {
                if (e.getHasieraData() == null || e.getBukaeraData() == null) continue;
                if (!d.isBefore(e.getHasieraData()) && !d.isAfter(e.getBukaeraData())) {
                    ebal = e;
                    break;
                }
            }
            if (ebal == null) continue;
            out.merge(ebal.getId(), ordu, Integer::sum);
        }
        return out;
    }
  
}
