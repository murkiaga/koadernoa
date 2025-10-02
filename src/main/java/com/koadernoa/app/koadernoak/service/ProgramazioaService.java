package com.koadernoa.app.koadernoak.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.koadernoak.entitateak.JardueraPlanifikatua;
import com.koadernoa.app.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.koadernoak.entitateak.Programazioa;
import com.koadernoa.app.koadernoak.entitateak.UnitateDidaktikoa;
import com.koadernoa.app.koadernoak.repository.JardueraPlanifikatuaRepository;
import com.koadernoa.app.koadernoak.repository.ProgramazioaRepository;
import com.koadernoa.app.koadernoak.repository.UnitateDidaktikoaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProgramazioaService {

    private final ProgramazioaRepository programazioaRepository;
    private final UnitateDidaktikoaRepository udRepository;
    private final JardueraPlanifikatuaRepository jpRepository;

    // ========= Programazioa =========
    @Transactional
    public Programazioa getOrCreateForKoadernoa(Koadernoa koadernoa) {
        return programazioaRepository.findByKoadernoa(koadernoa)
            .orElseGet(() -> programazioaRepository.save(
                new Programazioa(koadernoa, "Programazioa " + koadernoa.getIzena())
            ));
    }

    // ========= UD =========
    @Transactional
    public UnitateDidaktikoa addUd(Long programazioId, String kodea, String izenburua, int orduak, int posizioa) {
        Programazioa p = programazioaRepository.findById(programazioId)
            .orElseThrow(() -> new IllegalArgumentException("Programazioa ez da existitzen: " + programazioId));
        UnitateDidaktikoa ud = new UnitateDidaktikoa(p, kodea, izenburua, orduak, posizioa);
        p.gehituUnitatea(ud);
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
        return udRepository.existsByIdAndProgramazioa_Koadernoa_Id(udId, koadernoId);
    }

    @Transactional(readOnly = true)
    public boolean jpDagokioKoadernoari(Long jpId, Long koadernoId) {
        if (jpId == null || koadernoId == null) return false;
        return jpRepository.existsByIdAndUnitatea_Programazioa_Koadernoa_Id(jpId, koadernoId);
    }

    // ========= Laguntzailea =========
    @Transactional
    private void syncUdOrduak(Long udId) {
        long count = jpRepository.countByUnitatea_Id(udId);
        if (count > 0) {
            int suma = jpRepository.sumOrduakByUdId(udId);
            var ud = udRepository.findById(udId).orElseThrow();
            ud.setOrduak(suma);
            udRepository.save(ud);
        }
        // count == 0 -> EZ ukitu UD.orduak; eskuz kudeatzen jarraitzen du
    }
    
    @Transactional(readOnly = true)
    public int planifikatutakoOrduak(Long udId) {
        return jpRepository.sumOrduakByUdId(udId); // null ez, coalesce 0
    }

    @Transactional(readOnly = true)
    public java.util.Map<Long,Integer> planifikatutakoOrduakMap(java.util.List<UnitateDidaktikoa> uds) {
        var ids = uds.stream().map(UnitateDidaktikoa::getId).toList();
        var map = new java.util.HashMap<Long,Integer>();
        if (ids.isEmpty()) return map;
        for (Object[] row : jpRepository.sumOrduakByUdIds(ids)) {
            map.put((Long)row[0], ((Number)row[1]).intValue());
        }
        // faltan daudenak 0
        ids.forEach(id -> map.putIfAbsent(id, 0));
        return map;
    }
}