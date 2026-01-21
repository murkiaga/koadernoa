package com.koadernoa.app.objektuak.koadernoak.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Ebaluaketa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Jarduera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.JardueraPlanifikatua;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Programazioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.ProgramazioTxantiloi;
import com.koadernoa.app.objektuak.koadernoak.entitateak.ProgramazioTxantiloiJarduera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.UnitateDidaktikoa;
import com.koadernoa.app.objektuak.koadernoak.repository.EbaluaketaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.JardueraPlanifikatuaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.JardueraRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.ProgramazioaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.ProgramazioTxantiloiRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.UnitateDidaktikoaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProgramazioTxantiloiService {

    private final ProgramazioTxantiloiRepository txantiloiRepository;
    private final JardueraRepository jardueraRepository;
    private final ProgramazioaRepository programazioaRepository;
    private final EbaluaketaRepository ebaluaketaRepository;
    private final UnitateDidaktikoaRepository udRepository;
    private final JardueraPlanifikatuaRepository jpRepository;

    @Transactional
    public ProgramazioTxantiloi sortuTxantiloiDenboralizaziotik(Koadernoa koadernoa, Irakaslea irakaslea, String izenaOptional) {
        if (koadernoa == null || koadernoa.getId() == null) {
            throw new IllegalArgumentException("Koaderno aktiborik ez.");
        }
        if (irakaslea == null || irakaslea.getId() == null) {
            throw new IllegalArgumentException("Irakaslea ezin da hutsik izan.");
        }
        if (koadernoa.getModuloa() == null || koadernoa.getModuloa().getId() == null) {
            throw new IllegalArgumentException("Koadernoak ez du modulorik.");
        }

        List<Jarduera> jarduerak = jardueraRepository.findByKoadernoaIdOrderByDataAscIdAsc(koadernoa.getId());

        ProgramazioTxantiloi txantiloi = new ProgramazioTxantiloi();
        txantiloi.setIrakaslea(irakaslea);
        txantiloi.setModuloa(koadernoa.getModuloa());
        txantiloi.setIturburuKoadernoa(koadernoa);
        txantiloi.setSortzeData(LocalDateTime.now());
        txantiloi.setIzena(erabakiIzena(koadernoa, izenaOptional));

        int ordena = 1;
        for (Jarduera j : jarduerak) {
            ProgramazioTxantiloiJarduera tJ = new ProgramazioTxantiloiJarduera();
            tJ.setTxantiloi(txantiloi);
            tJ.setIzenburua(j.getTitulua() == null || j.getTitulua().isBlank() ? "Jarduera" : j.getTitulua());
            tJ.setDeskribapena(j.getDeskribapena());
            tJ.setIraupenaMin(kalkulatuIraupenaMin(j));
            tJ.setOharrak(j.getMota());
            tJ.setOrdena(ordena++);
            txantiloi.getJarduerak().add(tJ);
        }

        return txantiloiRepository.save(txantiloi);
    }

    @Transactional(readOnly = true)
    public List<ProgramazioTxantiloi> zerrendatuIrakaslearenTxantiloiak(Irakaslea irakaslea, Koadernoa koadernoa) {
        if (irakaslea == null || irakaslea.getId() == null) return List.of();
        if (koadernoa == null || koadernoa.getModuloa() == null || koadernoa.getModuloa().getId() == null) {
            return List.of();
        }
        return txantiloiRepository.findByIrakasleaIdAndModuloaIdOrderBySortzeDataDesc(
            irakaslea.getId(), koadernoa.getModuloa().getId()
        );
    }

    @Transactional
    public int aplikatuTxantiloiKoadernoan(Long txantiloiId, Koadernoa koadernoa, Irakaslea irakaslea) {
        if (koadernoa == null || koadernoa.getId() == null) {
            throw new IllegalArgumentException("Koaderno aktiborik ez.");
        }
        if (irakaslea == null || irakaslea.getId() == null) {
            throw new IllegalArgumentException("Irakaslea ezin da hutsik izan.");
        }
        ProgramazioTxantiloi txantiloi = txantiloiRepository.findByIdAndIrakasleaId(txantiloiId, irakaslea.getId())
            .orElseThrow(() -> new IllegalArgumentException("Txantiloia ez da aurkitu."));

        if (txantiloi.getModuloa() == null || koadernoa.getModuloa() == null ||
            !Objects.equals(txantiloi.getModuloa().getId(), koadernoa.getModuloa().getId())) {
            throw new IllegalArgumentException("Txantiloia ez dator bat koaderno honetako moduloarekin.");
        }

        Programazioa programazioa = programazioaRepository.findByKoadernoa(koadernoa)
            .orElseGet(() -> programazioaRepository.save(new Programazioa(koadernoa, koadernoa.getIzena())));

        Ebaluaketa ebaluaketa = programazioa.getEbaluaketak().stream()
            .sorted(Comparator.comparing(e -> e.getOrdena() == null ? 0 : e.getOrdena()))
            .findFirst()
            .orElseGet(() -> sortuEbaluaketaLehenengoa(programazioa));

        UnitateDidaktikoa ud = new UnitateDidaktikoa();
        ud.setProgramazioa(programazioa);
        ud.setEbaluaketa(ebaluaketa);
        ud.setKodea(sortuTxantiloiKodea(ebaluaketa));
        ud.setIzenburua("Txantiloia");
        ud.setOrduak(0);
        ud.setPosizioa(ebaluaketa.getUnitateak() == null ? 0 : ebaluaketa.getUnitateak().size());
        udRepository.save(ud);

        int gehitutakoak = 0;
        List<ProgramazioTxantiloiJarduera> jarduerak = txantiloi.getJarduerak() == null ? List.of() : txantiloi.getJarduerak();
        for (ProgramazioTxantiloiJarduera j : jarduerak) {
            JardueraPlanifikatua jp = new JardueraPlanifikatua();
            jp.setUnitatea(ud);
            jp.setIzenburua(j.getIzenburua());
            jp.setOrduak(bihurtuMinutuakOrduetara(j.getIraupenaMin()));
            jp.setPosizioa(gehitutakoak);
            jpRepository.save(jp);
            gehitutakoak++;
        }

        return gehitutakoak;
    }

    @Transactional
    public void ezabatuTxantiloi(Long txantiloiId, Irakaslea irakaslea) {
        if (irakaslea == null || irakaslea.getId() == null) {
            throw new IllegalArgumentException("Irakaslea ezin da hutsik izan.");
        }
        ProgramazioTxantiloi txantiloi = txantiloiRepository.findByIdAndIrakasleaId(txantiloiId, irakaslea.getId())
            .orElseThrow(() -> new IllegalArgumentException("Txantiloia ez da aurkitu."));
        txantiloiRepository.delete(txantiloi);
    }

    private String erabakiIzena(Koadernoa koadernoa, String izenaOptional) {
        if (izenaOptional != null && !izenaOptional.isBlank()) {
            return izenaOptional.trim();
        }
        String ikasturtea = koadernoa.getEgutegia() != null && koadernoa.getEgutegia().getIkasturtea() != null
            ? koadernoa.getEgutegia().getIkasturtea().getIzena()
            : "";
        String oinarrizkoa = ikasturtea == null || ikasturtea.isBlank() ? "Denboralizazioa" : ikasturtea + " denboralizazioa";
        return oinarrizkoa;
    }

    private Integer kalkulatuIraupenaMin(Jarduera jarduera) {
        if (jarduera == null) return 0;
        float orduak = jarduera.getOrduak();
        if (Float.isNaN(orduak) || orduak <= 0) return 0;
        return Math.round(orduak * 60);
    }

    private int bihurtuMinutuakOrduetara(Integer minutuak) {
        if (minutuak == null || minutuak <= 0) return 0;
        return (int) Math.round(minutuak / 60.0);
    }

    private Ebaluaketa sortuEbaluaketaLehenengoa(Programazioa programazioa) {
        Ebaluaketa e = new Ebaluaketa();
        e.setProgramazioa(programazioa);
        e.setIzena("1. Ebaluaketa");
        e.setOrdena(1);
        ebaluaketaRepository.save(e);
        programazioa.getEbaluaketak().add(e);
        return e;
    }

    private String sortuTxantiloiKodea(Ebaluaketa ebaluaketa) {
        String base = "TX";
        if (ebaluaketa.getUnitateak() == null || ebaluaketa.getUnitateak().isEmpty()) {
            return base;
        }
        List<String> kodeak = ebaluaketa.getUnitateak().stream()
            .map(UnitateDidaktikoa::getKodea)
            .filter(Objects::nonNull)
            .toList();
        if (!kodeak.contains(base)) {
            return base;
        }
        int i = 1;
        while (kodeak.contains(base + i)) {
            i++;
        }
        return base + i;
    }
}
