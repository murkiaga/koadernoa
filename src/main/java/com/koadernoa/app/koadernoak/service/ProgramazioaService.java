package com.koadernoa.app.koadernoak.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.koadernoak.entitateak.JardueraPlanifikatua;
import com.koadernoa.app.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.koadernoak.entitateak.Programazioa;
import com.koadernoa.app.koadernoak.entitateak.UnitateDidaktikoa;
import com.koadernoa.app.koadernoak.repository.JardueraPlanifikatuaRepository;
import com.koadernoa.app.koadernoak.repository.OrdutegiaRepository;
import com.koadernoa.app.koadernoak.repository.ProgramazioaRepository;
import com.koadernoa.app.koadernoak.repository.UnitateDidaktikoaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProgramazioaService {

	private final ProgramazioaRepository programazioaRepository;
    private final UnitateDidaktikoaRepository udRepository;
    private final JardueraPlanifikatuaRepository jpRepository;


    @Transactional
    public Programazioa getOrCreateForKoadernoa(Koadernoa koadernoa) {
        return programazioaRepository.findByKoadernoa(koadernoa)
                .orElseGet(() -> {
                    Programazioa p = new Programazioa(koadernoa, "Programazioa " + koadernoa.getIzena());
                    return programazioaRepository.save(p);
                });
    }

    // ---------- UD ----------
    @Transactional
    public UnitateDidaktikoa addUd(Long programazioId, String kodea, String izenburua, int orduak, int posizioa) {
        Programazioa p = programazioaRepository.findById(programazioId)
                .orElseThrow(() -> new IllegalArgumentException("Programazioa ez da existitzen: " + programazioId));
        UnitateDidaktikoa ud = new UnitateDidaktikoa(p, kodea, izenburua, orduak, posizioa);
        p.gehituUnitatea(ud);
        // Ordena eguneratu nahi bada, hemen normalizatu daiteke
        return udRepository.save(ud);
    }

    @Transactional
    public void updateUd(Long udId, String kodea, String izenburua, int orduak, int posizioa) {
        UnitateDidaktikoa ud = udRepository.findById(udId)
                .orElseThrow(() -> new IllegalArgumentException("UD ez da existitzen: " + udId));
        ud.setKodea(kodea);
        ud.setIzenburua(izenburua);
        ud.setOrduak(orduak);
        ud.setPosizioa(posizioa);
        udRepository.save(ud);
    }

    @Transactional
    public void deleteUd(Long udId) {
        UnitateDidaktikoa ud = udRepository.findById(udId)
                .orElseThrow(() -> new IllegalArgumentException("UD ez da existitzen: " + udId));
        // cascade orphanRemoval dagoenez, bere jarduera planifikatuak ere ezabatuko dira
        udRepository.delete(ud);
    }

    // ---------- Jarduera Planifikatua ----------
    @Transactional
    public JardueraPlanifikatua addJardueraPlanifikatua(Long udId, String izenburua, int orduak) {
        UnitateDidaktikoa ud = udRepository.findById(udId)
                .orElseThrow(() -> new IllegalArgumentException("UD ez da existitzen: " + udId));
        JardueraPlanifikatua jp = new JardueraPlanifikatua(ud, izenburua, orduak);
        ud.gehituAzpiJarduera(jp); // entitateko helper-a aprobetxatu
        return jpRepository.save(jp);
    }

    @Transactional
    public void updateJardueraPlanifikatua(Long jpId, String izenburua, int orduak) {
        JardueraPlanifikatua jp = jpRepository.findById(jpId)
                .orElseThrow(() -> new IllegalArgumentException("Jarduera planifikatua ez da existitzen: " + jpId));
        jp.setIzenburua(izenburua);
        jp.setOrduak(orduak);
        jpRepository.save(jp);
    }

    @Transactional
    public void deleteJardueraPlanifikatua(Long jpId) {
        JardueraPlanifikatua jp = jpRepository.findById(jpId)
                .orElseThrow(() -> new IllegalArgumentException("Jarduera planifikatua ez da existitzen: " + jpId));
        jpRepository.delete(jp);
    }
    
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
}
