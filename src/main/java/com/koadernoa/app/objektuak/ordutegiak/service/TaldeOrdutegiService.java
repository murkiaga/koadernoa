package com.koadernoa.app.objektuak.ordutegiak.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.repository.EgutegiaRepository;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.IrakasleOrdutegiLerroa;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.TaldeOrdutegia;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.TaldeOrdutegiLerroa;
import com.koadernoa.app.objektuak.ordutegiak.repository.IrakasleOrdutegiaRepository;
import com.koadernoa.app.objektuak.ordutegiak.repository.TaldeOrdutegiaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaldeOrdutegiService {
    public static final List<Astegunak> EGUNAK = List.of(Astegunak.ASTELEHENA, Astegunak.ASTEARTEA,
            Astegunak.ASTEAZKENA, Astegunak.OSTEGUNA, Astegunak.OSTIRALA);

    private final TaldeOrdutegiaRepository taldeOrdutegiak;
    private final IrakasleOrdutegiaRepository irakasleOrdutegiak;
    private final ModuloaRepository moduluak;
    private final TaldeaRepository taldeak;
    private final IkasturteaRepository ikasturteak;
    private final EgutegiaRepository egutegiak;

    @Transactional(readOnly = true)
    public Ikuspegia ikuspegia(Long taldeaId, Long hautatutakoOrdutegiaId) {
        Ikasturtea ikasturtea = ikasturteAktiboa();
        List<Moduloa> taldekoModuluak = moduluak.findWithIrakasleakByTaldeaId(taldeaId);
        List<TaldeOrdutegia> ordutegiak = taldeOrdutegiak
                .findByTaldea_IdAndIkasturtea_IdOrderByHasieraDataAsc(taldeaId, ikasturtea.getId());
        Map<Long, String> moduluEtiketak = new LinkedHashMap<>();
        taldekoModuluak.forEach(m -> {
            String kodea = m.getKodea() == null || m.getKodea().isBlank() ? m.getIzena() : m.getKodea();
            String irakasleIzenak = m.getIrakasleak().stream().map(i -> i.getIzena())
                    .filter(java.util.Objects::nonNull).filter(s -> !s.isBlank())
                    .collect(java.util.stream.Collectors.joining(", "));
            moduluEtiketak.put(m.getId(), kodea + (irakasleIzenak.isBlank() ? "" : "\n" + irakasleIzenak));
        });
        LocalDate gaur = LocalDate.now();
        Long aktiboaId = ordutegiak.stream().filter(o -> !o.getHasieraData().isAfter(gaur))
                .max(Comparator.comparing(TaldeOrdutegia::getHasieraData)).or(() -> ordutegiak.stream().findFirst())
                .map(TaldeOrdutegia::getId).orElse(null);
        Long hautatutakoaId = ordutegiak.stream().map(TaldeOrdutegia::getId)
                .filter(id -> id.equals(hautatutakoOrdutegiaId)).findFirst().orElse(aktiboaId);
        List<OrdutegiBista> bistak = new ArrayList<>();
        for (int i = 0; i < ordutegiak.size(); i++) {
            TaldeOrdutegia o = ordutegiak.get(i);
            LocalDate bukaera = i + 1 < ordutegiak.size() ? ordutegiak.get(i + 1).getHasieraData().minusDays(1) : null;
            Map<String, Long> gelaxkak = new LinkedHashMap<>();
            o.getLerroak().forEach(l -> gelaxkak.put(gakoa(l.getAsteguna(), l.getOrduZenbakia()), l.getModuloa().getId()));
            bistak.add(new OrdutegiBista(o.getId(), o.getHasieraData(), bukaera, o.getId().equals(aktiboaId), gelaxkak));
        }
        return new Ikuspegia(ikasturtea.getId(), EGUNAK, java.util.stream.IntStream.rangeClosed(1, 12).boxed().toList(),
                taldekoModuluak, moduluEtiketak, bistak, aktiboaId, hautatutakoaId);
    }

    @Transactional
    public TaldeOrdutegia sortu(Long taldeaId, LocalDate hasieraData) {
        if (hasieraData == null) throw new IllegalArgumentException("Hasiera data beharrezkoa da.");
        Ikasturtea ikasturtea = ikasturteAktiboa();
        Taldea taldea = taldeak.findById(taldeaId).orElseThrow(() -> new IllegalArgumentException("Taldea ez da aurkitu."));
        balioztatuData(taldeaId, ikasturtea, hasieraData);
        return taldeOrdutegiak.findByTaldea_IdAndIkasturtea_IdOrderByHasieraDataAsc(taldeaId, ikasturtea.getId()).stream()
                .filter(o -> o.getHasieraData().equals(hasieraData)).findFirst().orElseGet(() -> {
                    TaldeOrdutegia o = new TaldeOrdutegia(); o.setTaldea(taldea); o.setIkasturtea(ikasturtea);
                    o.setHasieraData(hasieraData); return taldeOrdutegiak.save(o);
                });
    }

    @Transactional
    public EguneratzeEmaitza eguneratuIrakasleetatik(Long taldeaId, Long ordutegiaId, LocalDate hasieraData) {
        TaldeOrdutegia helburua;
        if (ordutegiaId == null) {
            LocalDate data = hasieraData != null ? hasieraData : egutegiHasiera(taldeaId);
            helburua = sortu(taldeaId, data != null ? data : LocalDate.now());
        } else {
            helburua = ordutegia(ordutegiaId, taldeaId);
        }
        List<Moduloa> taldekoModuluak = moduluak.findWithIrakasleakByTaldeaId(taldeaId);
        Set<Long> irakasleIds = taldekoModuluak.stream().flatMap(m -> m.getIrakasleak().stream())
                .map(i -> i.getId()).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<String, Moduloa> moduluaIrakasleKodez = new LinkedHashMap<>();
        taldekoModuluak.forEach(m -> m.getIrakasleak().forEach(i ->
                moduluaIrakasleKodez.put(i.getId() + ":" + norm(m.getKodea()), m)));
        Map<String, Moduloa> gelaxkak = new LinkedHashMap<>();
        int gatazkak = 0;
        if (!irakasleIds.isEmpty()) {
            for (var io : irakasleOrdutegiak.findAllByIkasturteaIdAndIrakasleaIds(
                    helburua.getIkasturtea().getId(), new ArrayList<>(irakasleIds))) {
                for (IrakasleOrdutegiLerroa l : io.getLerroak()) {
                    if (!taldekoaDa(l, helburua.getTaldea()) || l.getAsteguna() == null || l.getOrduZenbakia() == null) continue;
                    Moduloa modulua = moduluaIrakasleKodez.get(io.getIrakaslea().getId() + ":" + norm(l.getModuluKodea()));
                    if (modulua == null) continue;
                    int saioak = l.getSaioKopurua() != null && l.getSaioKopurua() > 0 ? l.getSaioKopurua() : 1;
                    for (int i = 0; i < saioak; i++) {
                        String key = gakoa(l.getAsteguna(), l.getOrduZenbakia() + i);
                        Moduloa aurrekoa = gelaxkak.putIfAbsent(key, modulua);
                        if (aurrekoa != null && !aurrekoa.getId().equals(modulua.getId())) gatazkak++;
                    }
                }
            }
        }
        helburua.getLerroak().clear();
        gelaxkak.forEach((key, modulua) -> {
            String[] zatiak = key.split(":");
            TaldeOrdutegiLerroa l = new TaldeOrdutegiLerroa(); l.setTaldeOrdutegia(helburua);
            l.setAsteguna(Astegunak.valueOf(zatiak[0])); l.setOrduZenbakia(Integer.valueOf(zatiak[1]));
            l.setModuloa(modulua); helburua.getLerroak().add(l);
        });
        taldeOrdutegiak.save(helburua);
        return new EguneratzeEmaitza(helburua.getId(), gelaxkak.size(), gatazkak);
    }

    @Transactional
    public void gordeGelaxka(Long taldeaId, Long ordutegiaId, Astegunak asteguna, Integer ordua, Long moduloaId) {
        if (!EGUNAK.contains(asteguna) || ordua == null || ordua < 1 || ordua > 12)
            throw new IllegalArgumentException("Gelaxka baliogabea.");
        TaldeOrdutegia o = ordutegia(ordutegiaId, taldeaId);
        o.getLerroak().removeIf(l -> l.getAsteguna() == asteguna && ordua.equals(l.getOrduZenbakia()));
        if (moduloaId != null) {
            Moduloa m = moduluak.findById(moduloaId).orElseThrow(() -> new IllegalArgumentException("Moduloa ez da aurkitu."));
            if (m.getTaldea() == null || !taldeaId.equals(m.getTaldea().getId()))
                throw new IllegalArgumentException("Moduloa ez da talde honetakoa.");
            TaldeOrdutegiLerroa l = new TaldeOrdutegiLerroa(); l.setTaldeOrdutegia(o); l.setAsteguna(asteguna);
            l.setOrduZenbakia(ordua); l.setModuloa(m); o.getLerroak().add(l);
        }
        taldeOrdutegiak.save(o);
    }

    @Transactional
    public void ezabatu(Long taldeaId, Long ordutegiaId) {
        taldeOrdutegiak.delete(ordutegia(ordutegiaId, taldeaId));
    }

    private TaldeOrdutegia ordutegia(Long id, Long taldeaId) {
        TaldeOrdutegia o = taldeOrdutegiak.findWithLerroakById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ordutegia ez da aurkitu."));
        if (!taldeaId.equals(o.getTaldea().getId())) throw new IllegalArgumentException("Ordutegia ez da talde honetakoa.");
        return o;
    }

    private void balioztatuData(Long taldeaId, Ikasturtea ikasturtea, LocalDate data) {
        List<Moduloa> ms = moduluak.findByTaldeaId(taldeaId);
        if (!ms.isEmpty() && ms.get(0).getMaila() != null) {
            Egutegia e = egutegiak.findByIkasturtea_IdAndMaila_Id(ikasturtea.getId(), ms.get(0).getMaila().getId()).orElse(null);
            if (e != null && e.getHasieraData() != null && data.isBefore(e.getHasieraData()))
                throw new IllegalArgumentException("Hasiera data ikasturtea baino lehenagokoa da.");
            if (e != null && e.getBukaeraData() != null && data.isAfter(e.getBukaeraData()))
                throw new IllegalArgumentException("Hasiera data ikasturtea baino beranduagokoa da.");
        }
    }

    private LocalDate egutegiHasiera(Long taldeaId) {
        Ikasturtea i = ikasturteAktiboa();
        return moduluak.findByTaldeaId(taldeaId).stream().filter(m -> m.getMaila() != null).findFirst()
                .flatMap(m -> egutegiak.findByIkasturtea_IdAndMaila_Id(i.getId(), m.getMaila().getId()))
                .map(Egutegia::getHasieraData).orElse(null);
    }

    private Ikasturtea ikasturteAktiboa() {
        return ikasturteak.findFirstByAktiboaTrueOrderByIdDesc()
                .orElseThrow(() -> new IllegalArgumentException("Ez dago ikasturte aktiborik."));
    }
    private boolean taldekoaDa(IrakasleOrdutegiLerroa l, Taldea t) {
        return l.getTaldea() != null ? t.getId().equals(l.getTaldea().getId()) : norm(t.getIzena()).equals(norm(l.getTaldeKodea()));
    }
    private String norm(String s) { return s == null ? "" : s.trim().toUpperCase(Locale.ROOT); }
    private String gakoa(Astegunak a, Integer o) { return a.name() + ":" + o; }

    public record Ikuspegia(Long ikasturteaId, List<Astegunak> egunak, List<Integer> orduak,
                            List<Moduloa> moduluak, Map<Long, String> moduluEtiketak,
                            List<OrdutegiBista> ordutegiak, Long aktiboaId, Long hautatutakoaId) {}
    public record OrdutegiBista(Long id, LocalDate hasiera, LocalDate bukaera, boolean aktiboa,
                                Map<String, Long> gelaxkak) {}
    public record EguneratzeEmaitza(Long ordutegiaId, int gelaxkaKopurua, int gatazkaKopurua) {}
}
