package com.koadernoa.app.objektuak.koadernoak.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Ebaluaketa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Jarduera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.JardueraKalifikazioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Programazioa;
import com.koadernoa.app.objektuak.koadernoak.repository.JardueraKalifikazioaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.JardueraRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.ProgramazioaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.entitateak.MatrikulaEgoera;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JardueraKalifikazioService {
    private static final BigDecimal EHUN = new BigDecimal("100");
    private static final BigDecimal HAMAR = new BigDecimal("10");

    private final JardueraRepository jardueraRepository;
    private final JardueraKalifikazioaRepository kalifikazioaRepository;
    private final MatrikulaRepository matrikulaRepository;
    private final ProgramazioaRepository programazioaRepository;

    public boolean badagoJardueraKalifikagarririk(Long koadernoId) {
        return koadernoId != null && jardueraRepository.existsByKoadernoaIdAndKalifikagarriaTrue(koadernoId);
    }

    @Transactional(readOnly = true)
    public PantailaData prestatu(Long koadernoId, Long hautatutakoEbaluaketaId, Long fokukoJardueraId) {
        Programazioa programazioa = programazioaRepository.findByKoadernoaIdFetchEbaluaketak(koadernoId)
                .orElse(null);
        List<Ebaluaketa> ebaluaketak = programazioa == null || programazioa.getEbaluaketak() == null
                ? List.of()
                : programazioa.getEbaluaketak().stream()
                        .sorted(Comparator.comparing(e -> e.getOrdena() == null ? Integer.MAX_VALUE : e.getOrdena()))
                        .toList();
        List<Jarduera> jarduerak = jardueraRepository
                .findByKoadernoaIdAndKalifikagarriaTrueOrderByDataAscIdAsc(koadernoId);

        Map<Long, List<Jarduera>> jarduerakEbaluaketaka = new LinkedHashMap<>();
        ebaluaketak.forEach(e -> jarduerakEbaluaketaka.put(e.getId(), new ArrayList<>()));
        for (Jarduera jarduera : jarduerak) {
            Ebaluaketa ebaluaketa = aurkituEbaluaketa(jarduera, ebaluaketak);
            if (ebaluaketa != null) jarduerakEbaluaketaka.get(ebaluaketa.getId()).add(jarduera);
        }

        Long aukeratua = hautatutakoEbaluaketaId;
        if (fokukoJardueraId != null) {
            aukeratua = jarduerakEbaluaketaka.entrySet().stream()
                    .filter(e -> e.getValue().stream().anyMatch(j -> fokukoJardueraId.equals(j.getId())))
                    .map(Map.Entry::getKey).findFirst().orElse(aukeratua);
        }
        if (aukeratua == null || !jarduerakEbaluaketaka.containsKey(aukeratua)) {
            aukeratua = ebaluaketak.isEmpty() ? null : ebaluaketak.get(0).getId();
        }

        List<Matrikula> matrikulak = matrikulaRepository
                .findByKoadernoaIdAndEgoeraFetchIkasleaOrderByIzena(koadernoId, MatrikulaEgoera.MATRIKULATUA);
        Map<String, BigDecimal> notak = new LinkedHashMap<>();
        for (JardueraKalifikazioa k : kalifikazioaRepository.findByKoadernoId(koadernoId)) {
            notak.put(gakoa(k.getMatrikula().getId(), k.getJarduera().getId()), k.getNota());
        }

        List<Jarduera> hautatutakoJarduerak = aukeratua == null
                ? List.of() : jarduerakEbaluaketaka.getOrDefault(aukeratua, List.of());
        Map<Long, BigDecimal> guztirakoak = new LinkedHashMap<>();
        for (Matrikula matrikula : matrikulak) {
            BigDecimal guztira = BigDecimal.ZERO;
            for (Jarduera jarduera : hautatutakoJarduerak) {
                BigDecimal nota = notak.get(gakoa(matrikula.getId(), jarduera.getId()));
                BigDecimal pisua = jarduera.getPisua();
                if (nota != null && pisua != null) {
                    guztira = guztira.add(nota.multiply(pisua).divide(EHUN, 4, RoundingMode.HALF_UP));
                }
            }
            guztirakoak.put(matrikula.getId(), guztira.min(HAMAR).setScale(2, RoundingMode.HALF_UP));
        }
        return new PantailaData(ebaluaketak, jarduerakEbaluaketaka, aukeratua, hautatutakoJarduerak,
                matrikulak, notak, guztirakoak, fokukoJardueraId);
    }

    @Transactional
    public void gorde(Long koadernoId, Long ebaluaketaId, Map<String, String[]> parametroak) {
        PantailaData data = prestatu(koadernoId, ebaluaketaId, null);
        Map<Long, Jarduera> jarduerak = new LinkedHashMap<>();
        data.hautatutakoJarduerak().forEach(j -> jarduerak.put(j.getId(), j));
        Map<Long, Matrikula> matrikulak = new LinkedHashMap<>();
        data.matrikulak().forEach(m -> matrikulak.put(m.getId(), m));

        Map<Long, BigDecimal> pisuak = new LinkedHashMap<>();
        Map<String, BigDecimal> notak = new LinkedHashMap<>();
        for (Jarduera jarduera : jarduerak.values()) {
            pisuak.put(jarduera.getId(), parseBalioa(parametroak, "pisua_" + jarduera.getId(), BigDecimal.ZERO, EHUN,
                    "Pisua 0 eta 100 artekoa izan behar da"));
            for (Matrikula matrikula : matrikulak.values()) {
                String key = gakoa(matrikula.getId(), jarduera.getId());
                notak.put(key, parseBalioa(parametroak, "nota_" + key, BigDecimal.ZERO, HAMAR,
                        "Kalifikazioa 0 eta 10 artekoa izan behar da"));
            }
        }

        pisuak.forEach((id, pisua) -> jarduerak.get(id).setPisua(pisua));
        jardueraRepository.saveAll(jarduerak.values());

        Map<String, JardueraKalifikazioa> daudenak = new LinkedHashMap<>();
        for (JardueraKalifikazioa k : kalifikazioaRepository.findByKoadernoId(koadernoId)) {
            daudenak.put(gakoa(k.getMatrikula().getId(), k.getJarduera().getId()), k);
        }
        for (Jarduera jarduera : jarduerak.values()) {
            for (Matrikula matrikula : matrikulak.values()) {
                String key = gakoa(matrikula.getId(), jarduera.getId());
                BigDecimal nota = notak.get(key);
                JardueraKalifikazioa dagoena = daudenak.get(key);
                if (nota == null) {
                    if (dagoena != null) kalifikazioaRepository.delete(dagoena);
                } else {
                    JardueraKalifikazioa kalifikazioa = dagoena == null ? new JardueraKalifikazioa() : dagoena;
                    kalifikazioa.setJarduera(jarduera);
                    kalifikazioa.setMatrikula(matrikula);
                    kalifikazioa.setNota(nota.setScale(2, RoundingMode.HALF_UP));
                    kalifikazioaRepository.save(kalifikazioa);
                }
            }
        }
    }

    private BigDecimal parseBalioa(Map<String, String[]> parametroak, String izena, BigDecimal min,
                                    BigDecimal max, String errorea) {
        String[] balioak = parametroak.get(izena);
        if (balioak == null || balioak.length == 0 || balioak[0] == null || balioak[0].isBlank()) return null;
        try {
            BigDecimal balioa = new BigDecimal(balioak[0].trim().replace(',', '.'));
            if (balioa.compareTo(min) < 0 || balioa.compareTo(max) > 0) throw new NumberFormatException();
            return balioa;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorea + ".");
        }
    }

    private Ebaluaketa aurkituEbaluaketa(Jarduera jarduera, List<Ebaluaketa> ebaluaketak) {
        if (jarduera.getData() != null) {
            for (Ebaluaketa e : ebaluaketak) {
                if (e.getHasieraData() != null && e.getBukaeraData() != null
                        && !jarduera.getData().isBefore(e.getHasieraData())
                        && !jarduera.getData().isAfter(e.getBukaeraData())) return e;
            }
        }
        if (jarduera.getUnitatea() != null && jarduera.getUnitatea().getEbaluaketa() != null) {
            Long id = jarduera.getUnitatea().getEbaluaketa().getId();
            return ebaluaketak.stream().filter(e -> e.getId().equals(id)).findFirst().orElse(null);
        }
        if (ebaluaketak.isEmpty()) return null;
        if (jarduera.getData() != null) {
            return ebaluaketak.stream()
                    .filter(e -> e.getHasieraData() != null && !e.getHasieraData().isAfter(jarduera.getData()))
                    .max(Comparator.comparing(Ebaluaketa::getHasieraData))
                    .orElse(ebaluaketak.get(0));
        }
        return ebaluaketak.get(0);
    }

    private String gakoa(Long matrikulaId, Long jardueraId) {
        return matrikulaId + "_" + jardueraId;
    }

    public record PantailaData(List<Ebaluaketa> ebaluaketak,
                               Map<Long, List<Jarduera>> jarduerakEbaluaketaka,
                               Long hautatutakoEbaluaketaId,
                               List<Jarduera> hautatutakoJarduerak,
                               List<Matrikula> matrikulak,
                               Map<String, BigDecimal> notak,
                               Map<Long, BigDecimal> guztirakoak,
                               Long fokukoJardueraId) {}
}
