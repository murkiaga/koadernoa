package com.koadernoa.app.objektuak.koadernoak.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
        List<Ebaluaketa> ebaluaketak = programazioaRepository.findByKoadernoaIdFetchEbaluaketak(koadernoa.getId())
            .map(Programazioa::getEbaluaketak)
            .orElse(List.of());

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
            tJ.setEbaluaketaOrdena(ebaluaketaOrdena(j, ebaluaketak));
            UdInfo udInfo = parseUdInfo(j);
            tJ.setUdKodea(udInfo.kodea());
            tJ.setUdIzenburua(udInfo.izenburua());
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

        int gehitutakoak = 0;
        List<ProgramazioTxantiloiJarduera> jarduerak = txantiloi.getJarduerak() == null ? List.of() : txantiloi.getJarduerak();
        Map<Long, Ebaluaketa> ebaluaketaMap = resolveEbaluaketaMap(programazioa);
        Map<UdKey, UnitateDidaktikoa> udMap = new HashMap<>();
        for (ProgramazioTxantiloiJarduera j : jarduerak) {
            Ebaluaketa ebaluaketa = resolveEbaluaketaForJarduera(j, ebaluaketaMap);
            UnitateDidaktikoa ud = resolveUdForJarduera(programazioa, ebaluaketa, udMap, j);

            JardueraPlanifikatua jp = new JardueraPlanifikatua();
            jp.setUnitatea(ud);
            jp.setIzenburua(j.getIzenburua());
            jp.setOrduak(bihurtuMinutuakOrduetara(j.getIraupenaMin()));
            jp.setPosizioa(ud.getAzpiJarduerak() == null ? 0 : ud.getAzpiJarduerak().size());
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

    private Integer ebaluaketaOrdena(Jarduera jarduera, List<Ebaluaketa> ebaluaketak) {
        if (jarduera == null || jarduera.getData() == null || ebaluaketak == null) return null;
        return ebaluaketak.stream()
            .filter(e -> e.getHasieraData() != null && e.getBukaeraData() != null)
            .filter(e -> !jarduera.getData().isBefore(e.getHasieraData())
                && !jarduera.getData().isAfter(e.getBukaeraData()))
            .map(Ebaluaketa::getOrdena)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private UdInfo parseUdInfo(Jarduera jarduera) {
        if (jarduera == null) return new UdInfo(null, null);
        String deskribapena = jarduera.getDeskribapena();
        if (deskribapena == null || !deskribapena.contains("—")) {
            return new UdInfo(null, null);
        }
        String[] parts = deskribapena.split("—", 2);
        String kodea = parts[0].trim();
        String izenburua = parts.length > 1 ? parts[1].trim() : null;
        if (izenburua != null && izenburua.contains("(UD orokorra)")) {
            izenburua = Optional.ofNullable(jarduera.getTitulua()).orElse(izenburua).trim();
        }
        if (kodea.isBlank()) kodea = null;
        if (izenburua != null && izenburua.isBlank()) izenburua = null;
        return new UdInfo(kodea, izenburua);
    }

    private record UdInfo(String kodea, String izenburua) {}

    private record UdKey(Long ebaluaketaId, String kodea, String izenburua) {}

    private Map<Long, Ebaluaketa> resolveEbaluaketaMap(Programazioa programazioa) {
        if (programazioa.getEbaluaketak() == null || programazioa.getEbaluaketak().isEmpty()) {
            Ebaluaketa berria = sortuEbaluaketaLehenengoa(programazioa);
            return Map.of(1L, berria);
        }
        return programazioa.getEbaluaketak().stream()
            .filter(e -> e.getOrdena() != null)
            .collect(java.util.stream.Collectors.toMap(
                e -> e.getOrdena().longValue(),
                e -> e,
                (a, b) -> a
            ));
    }

    private Ebaluaketa resolveEbaluaketaForJarduera(ProgramazioTxantiloiJarduera jarduera,
                                                    Map<Long, Ebaluaketa> ebaluaketaMap) {
        if (jarduera.getEbaluaketaOrdena() != null) {
            Ebaluaketa match = ebaluaketaMap.get(jarduera.getEbaluaketaOrdena().longValue());
            if (match != null) return match;
        }
        return ebaluaketaMap.values().stream()
            .sorted(Comparator.comparing(e -> Optional.ofNullable(e.getOrdena()).orElse(0)))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Ez dago ebaluaketarik."));
    }

    private UnitateDidaktikoa resolveUdForJarduera(Programazioa programazioa,
                                                   Ebaluaketa ebaluaketa,
                                                   Map<UdKey, UnitateDidaktikoa> udMap,
                                                   ProgramazioTxantiloiJarduera jarduera) {
        String kodea = jarduera.getUdKodea();
        String izenburua = jarduera.getUdIzenburua();

        if (kodea == null || kodea.isBlank()) {
            kodea = sortuTxantiloiKodea(ebaluaketa);
        }
        if (izenburua == null || izenburua.isBlank()) {
            izenburua = "Txantiloia";
        }

        UdKey key = new UdKey(ebaluaketa.getId(), kodea, izenburua);
        if (udMap.containsKey(key)) return udMap.get(key);

        UnitateDidaktikoa existing = ebaluaketa.getUnitateak() == null ? null
            : ebaluaketa.getUnitateak().stream()
                .filter(ud -> kodea.equals(ud.getKodea()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            udMap.put(key, existing);
            return existing;
        }

        UnitateDidaktikoa ud = new UnitateDidaktikoa();
        ud.setProgramazioa(programazioa);
        ud.setEbaluaketa(ebaluaketa);
        ud.setKodea(kodea);
        ud.setIzenburua(izenburua);
        ud.setOrduak(0);
        ud.setPosizioa(ebaluaketa.getUnitateak() == null ? 0 : ebaluaketa.getUnitateak().size());
        udRepository.save(ud);
        if (ebaluaketa.getUnitateak() != null) {
            ebaluaketa.getUnitateak().add(ud);
        }
        udMap.put(key, ud);
        return ud;
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
