package com.koadernoa.app.objektuak.mezuak.service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EstatistikaEbaluazioan;
import com.koadernoa.app.objektuak.mezuak.entitateak.Mezua;
import com.koadernoa.app.objektuak.mezuak.repository.MezuaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MezuaService {

    private final MezuaRepository mezuaRepository;

    @Transactional
    public int bidaliEstatistikaFiltrotik(Irakaslea bidaltzailea, List<EstatistikaEbaluazioan> estatistikak, String edukia) {
        Set<Long> hartzaileIds = new LinkedHashSet<>();
        for (EstatistikaEbaluazioan e : estatistikak) {
            if (e.getKoadernoa() == null || e.getKoadernoa().getIrakasleak() == null) continue;
            e.getKoadernoa().getIrakasleak().forEach(ir -> {
                if (ir != null && ir.getId() != null) {
                    hartzaileIds.add(ir.getId());
                }
            });
        }

        int bidaliak = 0;
        for (Long hartzaileId : hartzaileIds) {
            if (bidaltzailea.getId().equals(hartzaileId)) continue;
            Irakaslea hartzailea = new Irakaslea();
            hartzailea.setId(hartzaileId);

            Mezua mezua = new Mezua();
            mezua.setBidaltzailea(bidaltzailea);
            mezua.setHartzailea(hartzailea);
            mezua.setEdukia(edukia);
            mezua.setBidalketaData(LocalDateTime.now());
            mezuaRepository.save(mezua);
            bidaliak++;
        }
        return bidaliak;
    }

    @Transactional(readOnly = true)
    public List<Mezua> jasotakoak(Long irakasleId) {
        return mezuaRepository.findByHartzaileaIdOrderByBidalketaDataDesc(irakasleId);
    }

    @Transactional(readOnly = true)
    public List<Mezua> bidaliEtaJasotakoak(Long irakasleId) {
        return mezuaRepository.findByBidaltzaileaIdOrHartzaileaIdOrderByBidalketaDataDesc(irakasleId, irakasleId);
    }

    @Transactional(readOnly = true)
    public long irakurriGabekoKopurua(Long irakasleId) {
        return mezuaRepository.countByHartzaileaIdAndIrakurritaFalse(irakasleId);
    }

    @Transactional
    public Mezua markatuIrakurrita(Long mezuaId, Long hartzaileId) {
        Mezua mezua = mezuaRepository.findById(mezuaId)
                .orElseThrow(() -> new IllegalArgumentException("Mezua ez da aurkitu"));
        if (mezua.getHartzailea() == null || !hartzaileId.equals(mezua.getHartzailea().getId())) {
            throw new IllegalArgumentException("Mezu hau ezin duzu irakurri");
        }
        if (!mezua.isIrakurrita()) {
            mezua.setIrakurrita(true);
            mezua.setIrakurketaData(LocalDateTime.now());
            mezuaRepository.save(mezua);
        }
        return mezua;
    }
}
