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
    public void addJardueraPlanifikatua(Long udId, String izenburua, int orduak) {
        var ud = udRepository.findById(udId).orElseThrow();

        var jp = new JardueraPlanifikatua();
        jp.setUnitatea(ud);
        jp.setIzenburua(izenburua);
        jp.setOrduak(orduak);
        jp.setPosizioa(ud.getAzpiJarduerak().size()); // edo kalkulu hobea
        jpRepository.save(jp);

        syncUdOrduak(udId);
    }

    @Transactional
    public void updateJardueraPlanifikatua(Long jpId, String izenburua, int orduak) {
        var jp = jpRepository.findById(jpId).orElseThrow();
        jp.setIzenburua(izenburua);
        jp.setOrduak(orduak);
        jpRepository.save(jp);

        syncUdOrduak(jp.getUnitatea().getId());
    }

    @Transactional
    public void deleteJardueraPlanifikatua(Long jpId) {
        var jp = jpRepository.findById(jpId).orElseThrow();
        Long udId = jp.getUnitatea().getId();
        jpRepository.delete(jp);

        syncUdOrduak(udId); // azkena bada, ez du orduak ukituko (eskuzko balioa mantentzen da)
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
    
    @Transactional
    public void reorderUd(Long koadernoId, List<Long> udIds) {
        if (udIds == null) return;
        // ziurtatu UD guztiak koaderno honetakoak direla
        for (int i=0;i<udIds.size();i++){
            Long udId = udIds.get(i);
            if (!udDagokioKoadernoari(udId, koadernoId)) continue;
            var ud = udRepository.findById(udId).orElseThrow();
            ud.setPosizioa(i); // edo i*10
            udRepository.save(ud);
        }
    }

    @Transactional
    public void moveOrReorderJarduera(Long jpId, Long toUdId, int newIndex) {
        var jp = jpRepository.findById(jpId).orElseThrow();
        Long fromUdId = jp.getUnitatea().getId();

        var toUd = udRepository.findById(toUdId).orElseThrow();

        // kendu ZAHARRETIK (memorian)
        var fromList = new java.util.ArrayList<>(jp.getUnitatea().getAzpiJarduerak());
        fromList.removeIf(x -> x.getId().equals(jpId));

        // sartu BERRIAN posizio berrian
        var toList = new java.util.ArrayList<>(toUd.getAzpiJarduerak());
        toList.removeIf(x -> x.getId().equals(jpId));
        newIndex = Math.max(0, Math.min(newIndex, toList.size()));
        jp.setUnitatea(toUd); // gurasoa aldatu
        toList.add(newIndex, jp);

        // birkalkulatu posizioak toUd-n
        for (int i = 0; i < toList.size(); i++) {
            toList.get(i).setPosizioa(i);
            jpRepository.save(toList.get(i));
        }
        // (Aukeran) fromUd-en posizioak trinkotu
        for (int i = 0; i < fromList.size(); i++) {
            fromList.get(i).setPosizioa(i);
            jpRepository.save(fromList.get(i));
        }

        // orduak sinkronizatu bi UD-etan
        syncUdOrduak(fromUdId);
        syncUdOrduak(toUdId);
    }

    @Transactional(readOnly = true)
    public UnitateDidaktikoa findUd(Long id){ return udRepository.findById(id).orElseThrow(); }

    @Transactional(readOnly = true)
    public JardueraPlanifikatua findJarduera(Long id){ return jpRepository.findById(id).orElseThrow(); }

    @Transactional
    public void updateUd(Long udId, String kodea, String izenburua, int orduak, Integer posizioa) {
        var ud = udRepository.findById(udId).orElseThrow();
        ud.setKodea(kodea);
        ud.setIzenburua(izenburua);
        if (posizioa != null) ud.setPosizioa(posizioa);

        long count = jpRepository.countByUnitatea_Id(udId);
        if (count > 0) {
            // azpi-jarduerak daude -> orduak DERIBATUAK dira
            int suma = jpRepository.sumOrduakByUdId(udId);
            ud.setOrduak(suma);
        } else {
            // ez dago azpi-jarduerarik -> eskuzkoa onartu
            ud.setOrduak(orduak);
        }
        udRepository.save(ud);
    }
    
    @Transactional
    private void syncUdOrduak(Long udId) {
        long count = jpRepository.countByUnitatea_Id(udId);
        if (count > 0) {
            int suma = jpRepository.sumOrduakByUdId(udId); // beti != null (coalesce)
            var ud = udRepository.findById(udId).orElseThrow();
            ud.setOrduak(suma);
            udRepository.save(ud);
        }
        // count == 0 -> EZ ukitu UD.orduak; eskuz kudeatzen jarraitzen da
    }
}
