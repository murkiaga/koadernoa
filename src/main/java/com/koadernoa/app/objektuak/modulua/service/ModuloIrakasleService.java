package com.koadernoa.app.objektuak.modulua.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.IrakasleOrdutegiLerroa;
import com.koadernoa.app.objektuak.ordutegiak.repository.IrakasleOrdutegiaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModuloIrakasleService {
    private static final List<Astegunak> ASTE_ORDENA = List.of(
            Astegunak.ASTELEHENA, Astegunak.ASTEARTEA, Astegunak.ASTEAZKENA,
            Astegunak.OSTEGUNA, Astegunak.OSTIRALA);

    private final ModuloaRepository moduloaRepository;
    private final IrakasleaRepository irakasleaRepository;
    private final IrakasleOrdutegiaRepository ordutegiaRepository;

    @Transactional
    public EsleipenEmaitza esleituOrdutegietatik() {
        Map<String, List<Moduloa>> moduluak = new LinkedHashMap<>();
        Map<String, Long> taldeakKodez = new LinkedHashMap<>();
        moduloaRepository.findAll().stream()
                .filter(m -> m.getTaldea() != null && ezHutsa(m.getKodea()))
                .forEach(m -> {
                    moduluak.computeIfAbsent(gakoa(m.getTaldea().getId(), m.getKodea()), k -> new ArrayList<>()).add(m);
                    if (ezHutsa(m.getTaldea().getIzena())) {
                        taldeakKodez.put(normalizatu(m.getTaldea().getIzena()), m.getTaldea().getId());
                    }
                });

        Set<String> prozesatutakoak = new LinkedHashSet<>();
        Set<Moduloa> gordetzekoak = new LinkedHashSet<>();
        int lotuGabe = 0;
        int anbiguo = 0;
        int lehendik = 0;
        int gehitu = 0;

        for (var ordutegia : ordutegiaRepository.findAllAktiboak()) {
            Irakaslea irakaslea = ordutegia.getIrakaslea();
            if (irakaslea == null) continue;
            for (IrakasleOrdutegiLerroa lerroa : ordutegia.getLerroak()) {
                Long taldeaId = lerrokoTaldeaId(lerroa, taldeakKodez);
                if (taldeaId == null || !ezHutsa(lerroa.getModuluKodea())) {
                    lotuGabe++;
                    continue;
                }
                String saioGakoa = irakaslea.getId() + ":" + gakoa(taldeaId, lerroa.getModuluKodea());
                if (!prozesatutakoak.add(saioGakoa)) continue;
                List<Moduloa> hautagaiak = moduluak.getOrDefault(gakoa(taldeaId, lerroa.getModuluKodea()), List.of());
                if (hautagaiak.isEmpty()) {
                    lotuGabe++;
                    continue;
                }
                if (hautagaiak.size() != 1) {
                    anbiguo++;
                    continue;
                }
                Moduloa moduloa = hautagaiak.get(0);
                if (moduloa.getIrakasleak().stream().anyMatch(i -> i.getId().equals(irakaslea.getId()))) {
                    lehendik++;
                    continue;
                }
                moduloa.getIrakasleak().add(irakaslea);
                gordetzekoak.add(moduloa);
                gehitu++;
            }
        }
        moduloaRepository.saveAll(gordetzekoak);
        return new EsleipenEmaitza(gehitu, lehendik, lotuGabe, anbiguo);
    }

    @Transactional(readOnly = true)
    public Moduloa moduluaIrakasleekin(Long moduloaId) {
        return moduloaRepository.findWithIrakasleakById(moduloaId)
                .orElseThrow(() -> new IllegalArgumentException("Moduloa ez da aurkitu: " + moduloaId));
    }

    @Transactional(readOnly = true)
    public List<Irakaslea> bilatuGehitzeko(Long moduloaId, String q) {
        if (q == null || q.trim().length() < 3) return List.of();
        Set<Long> esleituta = moduluaIrakasleekin(moduloaId).getIrakasleak().stream()
                .map(Irakaslea::getId).collect(java.util.stream.Collectors.toSet());
        return irakasleaRepository
                .findTop10ByIzenaContainingIgnoreCaseOrEmailaContainingIgnoreCaseOrderByIzenaAsc(q.trim(), q.trim())
                .stream().filter(i -> !esleituta.contains(i.getId())).toList();
    }

    @Transactional
    public Irakaslea gehitu(Long moduloaId, Long irakasleaId) {
        Moduloa moduloa = moduluaIrakasleekin(moduloaId);
        Irakaslea irakaslea = irakasleaRepository.findById(irakasleaId)
                .orElseThrow(() -> new IllegalArgumentException("Irakaslea ez da aurkitu: " + irakasleaId));
        if (moduloa.getIrakasleak().stream().noneMatch(i -> i.getId().equals(irakasleaId))) {
            moduloa.getIrakasleak().add(irakaslea);
            moduloaRepository.save(moduloa);
        }
        return irakaslea;
    }

    @Transactional
    public void kendu(Long moduloaId, Long irakasleaId) {
        Moduloa moduloa = moduluaIrakasleekin(moduloaId);
        moduloa.getIrakasleak().removeIf(i -> i.getId().equals(irakasleaId));
        moduloaRepository.save(moduloa);
    }

    @Transactional(readOnly = true)
    public TaldeOrdutegia taldeOrdutegia(Long taldeaId) {
        List<Moduloa> moduluak = moduloaRepository.findWithIrakasleakByTaldeaId(taldeaId);
        String taldeKodea = moduluak.stream().map(Moduloa::getTaldea)
                .filter(java.util.Objects::nonNull).map(t -> t.getIzena())
                .filter(this::ezHutsa).findFirst().orElse(null);
        Map<Long, Set<String>> kodeakIrakasleko = new LinkedHashMap<>();
        Map<String, Moduloa> moduluaIrakasleEtaKodez = new LinkedHashMap<>();
        for (Moduloa moduloa : moduluak) {
            if (!ezHutsa(moduloa.getKodea())) continue;
            for (Irakaslea irakaslea : moduloa.getIrakasleak()) {
                kodeakIrakasleko.computeIfAbsent(irakaslea.getId(), k -> new LinkedHashSet<>())
                        .add(normalizatu(moduloa.getKodea()));
                moduluaIrakasleEtaKodez.put(irakaslea.getId() + ":" + normalizatu(moduloa.getKodea()), moduloa);
            }
        }
        if (kodeakIrakasleko.isEmpty()) return hutsekoOrdutegia();

        Map<String, List<TaldeOrdutegiSarrera>> gelaxkak = new LinkedHashMap<>();
        int azkenOrdua = 12;
        for (var ordutegia : ordutegiaRepository.findAllAktiboakByIrakasleaIds(new ArrayList<>(kodeakIrakasleko.keySet()))) {
            Long irakasleaId = ordutegia.getIrakaslea().getId();
            for (IrakasleOrdutegiLerroa lerroa : ordutegia.getLerroak()) {
                if (!lerroaTaldekoaDa(lerroa, taldeaId, taldeKodea)
                        || lerroa.getAsteguna() == null || lerroa.getOrduZenbakia() == null
                        || !ASTE_ORDENA.contains(lerroa.getAsteguna())) continue;
                String kodea = normalizatu(lerroa.getModuluKodea());
                if (!kodeakIrakasleko.getOrDefault(irakasleaId, Set.of()).contains(kodea)) continue;
                Moduloa moduloa = moduluaIrakasleEtaKodez.get(irakasleaId + ":" + kodea);
                String gela = ezHutsa(lerroa.getGelaKodea()) ? lerroa.getGelaKodea() : lerroa.getGelaIzena();
                TaldeOrdutegiSarrera sarrera = new TaldeOrdutegiSarrera(lerroa.getId(),
                        moduloa != null ? moduloa.getKodea() : kodea,
                        moduloa != null ? moduloa.getIzena() : lerroa.getModuluIzena(),
                        ordutegia.getIrakaslea().getIzena(), gela);
                int saioak = lerroa.getSaioKopurua() != null && lerroa.getSaioKopurua() > 0 ? lerroa.getSaioKopurua() : 1;
                int bukaera = lerroa.getOrduZenbakia() + saioak - 1;
                azkenOrdua = Math.max(azkenOrdua, bukaera);
                for (int ordua = lerroa.getOrduZenbakia(); ordua <= bukaera; ordua++) {
                    gelaxkak.computeIfAbsent(gelaxkaGakoa(lerroa.getAsteguna(), ordua), k -> new ArrayList<>()).add(sarrera);
                }
            }
        }
        int maxOrdua = azkenOrdua;
        List<TaldeOrdutegiLerroa> lerroak = IntStream.rangeClosed(1, maxOrdua)
                .mapToObj(ordua -> new TaldeOrdutegiLerroa(ordua, ASTE_ORDENA.stream()
                        .map(eguna -> new TaldeOrdutegiGelaxka(eguna,
                                gelaxkak.getOrDefault(gelaxkaGakoa(eguna, ordua), List.of()).stream()
                                        .sorted(Comparator.comparing(TaldeOrdutegiSarrera::moduluKodea, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                                        .toList()))
                        .toList())).toList();
        return new TaldeOrdutegia(ASTE_ORDENA, lerroak);
    }

    private TaldeOrdutegia hutsekoOrdutegia() {
        return new TaldeOrdutegia(ASTE_ORDENA, IntStream.rangeClosed(1, 12)
                .mapToObj(ordua -> new TaldeOrdutegiLerroa(ordua, ASTE_ORDENA.stream()
                        .map(eguna -> new TaldeOrdutegiGelaxka(eguna, List.of())).toList())).toList());
    }

    private String gakoa(Long taldeaId, String kodea) { return taldeaId + ":" + normalizatu(kodea); }
    private Long lerrokoTaldeaId(IrakasleOrdutegiLerroa lerroa, Map<String, Long> taldeakKodez) {
        if (lerroa.getTaldea() != null) return lerroa.getTaldea().getId();
        return taldeakKodez.get(normalizatu(lerroa.getTaldeKodea()));
    }
    private boolean lerroaTaldekoaDa(IrakasleOrdutegiLerroa lerroa, Long taldeaId, String taldeKodea) {
        if (lerroa.getTaldea() != null) return taldeaId.equals(lerroa.getTaldea().getId());
        return ezHutsa(taldeKodea) && normalizatu(taldeKodea).equals(normalizatu(lerroa.getTaldeKodea()));
    }
    private String gelaxkaGakoa(Astegunak eguna, int ordua) { return eguna.name() + ":" + ordua; }
    private String normalizatu(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
    private boolean ezHutsa(String value) { return value != null && !value.isBlank(); }

    public record EsleipenEmaitza(int gehituak, int lehendikZeudenak, int lotuGabeak, int anbiguoak) {}
    public record TaldeOrdutegia(List<Astegunak> egunak, List<TaldeOrdutegiLerroa> lerroak) {}
    public record TaldeOrdutegiLerroa(int orduZenbakia, List<TaldeOrdutegiGelaxka> gelaxkak) {}
    public record TaldeOrdutegiGelaxka(Astegunak asteguna, List<TaldeOrdutegiSarrera> edukiak) {
        public boolean beteta() { return edukiak != null && !edukiak.isEmpty(); }
    }
    public record TaldeOrdutegiSarrera(Long lerroaId, String moduluKodea, String moduluIzena,
                                       String irakasleIzena, String gela) {}
}
