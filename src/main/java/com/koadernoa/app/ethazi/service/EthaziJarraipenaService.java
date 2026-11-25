package com.koadernoa.app.ethazi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.ethazi.entitateak.Erronka;
import com.koadernoa.app.ethazi.entitateak.ErronkaTaldeKidea;
import com.koadernoa.app.ethazi.entitateak.EthaziJarraipenGelaxka;
import com.koadernoa.app.ethazi.entitateak.EthaziKoadernoa;
import com.koadernoa.app.ethazi.repository.ErronkaRepository;
import com.koadernoa.app.ethazi.repository.ErronkaTaldeKideaRepository;
import com.koadernoa.app.ethazi.repository.EthaziJarraipenGelaxkaRepository;
import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunMota;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.repository.EgutegiaRepository;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Asistentzia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa;
import com.koadernoa.app.objektuak.koadernoak.repository.AsistentziaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.SaioaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.repository.IkasleaRepository;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.TaldeOrdutegia;
import com.koadernoa.app.objektuak.ordutegiak.repository.TaldeOrdutegiaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EthaziJarraipenaService {
    private static final int OHAR_MAX_LUZERA = 2000;

    private final IkasturteaRepository ikasturteak;
    private final ErronkaRepository erronkak;
    private final ErronkaTaldeKideaRepository taldeKideak;
    private final EthaziJarraipenGelaxkaRepository gelaxkak;
    private final IkasleaRepository ikasleak;
    private final ModuloaRepository moduluak;
    private final TaldeOrdutegiaRepository taldeOrdutegiak;
    private final EgutegiaRepository egutegiak;
    private final SaioaRepository saioak;
    private final AsistentziaRepository asistentziak;

    @Transactional
    public void sinkronizatuAsistentzia(EthaziKoadernoa ethaziKoadernoa,
            Koadernoa koadernoa, Saioa saioa, Matrikula matrikula) {
        if (ethaziKoadernoa == null || ethaziKoadernoa.getTaldea() == null
                || koadernoa == null || koadernoa.getModuloa() == null || saioa == null
                || saioa.getData() == null || matrikula == null || matrikula.getIkaslea() == null) return;
        Moduloa modulua = koadernoa.getModuloa();
        LocalDate data = saioa.getData();
        Ikasturtea ikasturtea = koadernoa.getEgutegia() != null
                ? koadernoa.getEgutegia().getIkasturtea() : null;
        if (ikasturtea == null) {
            ikasturtea = ikasturteak.findFirstByAktiboaTrueOrderByIdDesc().orElse(null);
        }
        if (ikasturtea == null) return;

        List<Saioa> egunekoSaioak = saioak.findByKoadernoaIdAndData(koadernoa.getId(), data);
        BigDecimal faltaOrduak = asistentziak
                .findBySaioaInAndMatrikulaIn(egunekoSaioak, List.of(matrikula)).stream()
                .filter(a -> a.getEgoera() == Asistentzia.AsistentziaEgoera.HUTS
                        || a.getEgoera() == Asistentzia.AsistentziaEgoera.JUSTIFIKATUA)
                .map(a -> BigDecimal.valueOf(Math.max(0, a.getSaioa().getIraupenaSlot())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Set<Long> taldekoModuloIds = moduluak.findByTaldeaId(ethaziKoadernoa.getTaldea().getId())
                .stream().filter(Moduloa::isAktibo).map(Moduloa::getId).collect(Collectors.toSet());
        List<Erronka> egunekoErronkak = aukeratuErronkak(
                ethaziKoadernoa, ikasturtea, taldekoModuloIds).stream()
                .filter(e -> e.getHasieraData() != null && e.getBukaeraData() != null
                        && !data.isBefore(e.getHasieraData()) && !data.isAfter(e.getBukaeraData()))
                .filter(e -> e.getModuluak().stream().anyMatch(m -> modulua.getId().equals(m.getId())))
                .toList();
        for (Erronka erronka : egunekoErronkak) {
            Optional<EthaziJarraipenGelaxka> gordeta = gelaxkak
                    .findByEthaziKoadernoaIdAndErronkaIdAndIkasleaIdAndModuloaIdAndData(
                            ethaziKoadernoa.getId(), erronka.getId(), matrikula.getIkaslea().getId(),
                            modulua.getId(), data);
            if (faltaOrduak.signum() == 0
                    && gordeta.map(EthaziJarraipenGelaxka::getOharra)
                            .map(String::isBlank).orElse(true)) {
                gordeta.ifPresent(gelaxkak::delete);
                continue;
            }
            EthaziJarraipenGelaxka gelaxka = gordeta.orElseGet(EthaziJarraipenGelaxka::new);
            gelaxka.setEthaziKoadernoa(ethaziKoadernoa);
            gelaxka.setErronka(erronka);
            gelaxka.setIkaslea(matrikula.getIkaslea());
            gelaxka.setModuloa(modulua);
            gelaxka.setData(data);
            gelaxka.setFaltaOrduak(faltaOrduak);
            gelaxkak.save(gelaxka);
        }
    }

    @Transactional(readOnly = true)
    public JarraipenaIkuspegia prestatu(EthaziKoadernoa koadernoa, Egutegia egutegia,
            Irakaslea irakaslea, boolean administratzailea, Long eskatutakoModuloaId) {
        Ikasturtea ikasturtea = ikasturteak.findFirstByAktiboaTrueOrderByIdDesc().orElse(null);
        if (koadernoa == null || koadernoa.getTaldea() == null
                || koadernoa.getTaldea().getZikloa() == null || ikasturtea == null) {
            return JarraipenaIkuspegia.hutsa();
        }

        Long taldeaId = koadernoa.getTaldea().getId();
        List<Moduloa> taldekoModuluak = moduluak.findByTaldeaId(taldeaId).stream()
                .filter(Moduloa::isAktibo)
                .toList();
        Map<Long, Moduloa> taldekoModuluakIdz = taldekoModuluak.stream()
                .collect(Collectors.toMap(Moduloa::getId, Function.identity()));
        List<Erronka> aukeratutakoErronkak = aukeratuErronkak(koadernoa, ikasturtea, taldekoModuluakIdz.keySet());
        Set<Long> erronketakoModuloIds = aukeratutakoErronkak.stream()
                .flatMap(e -> e.getModuluak().stream()).map(Moduloa::getId)
                .filter(taldekoModuluakIdz::containsKey).collect(Collectors.toSet());

        Set<Long> editagarriIds = moduluEditagarriak(koadernoa, irakaslea, administratzailea).stream()
                .map(Moduloa::getId).filter(erronketakoModuloIds::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<ModuluAukera> moduluAukerak = editagarriIds.stream()
                .map(taldekoModuluakIdz::get).filter(Objects::nonNull)
                .sorted(Comparator.comparing(this::etiketa, String.CASE_INSENSITIVE_ORDER))
                .map(m -> new ModuluAukera(m.getId(), etiketa(m))).toList();
        Long moduloaId = editagarriIds.contains(eskatutakoModuloaId) ? eskatutakoModuloaId : null;
        if (moduloaId != null) {
            aukeratutakoErronkak = aukeratutakoErronkak.stream()
                    .filter(e -> e.getModuluak().stream().anyMatch(m -> moduloaId.equals(m.getId())))
                    .toList();
        }

        List<TaldeOrdutegia> ordutegiBertsioak = taldeOrdutegiak
                .findByTaldea_IdAndIkasturtea_IdOrderByHasieraDataAsc(taldeaId, ikasturtea.getId());
        List<ErronkaZutabeMultzoa> erronkaZutabeak = new ArrayList<>();
        Map<Long, ErronkaOrdutegia> ordutegiak = new LinkedHashMap<>();
        int indizea = 1;
        for (Erronka erronka : aukeratutakoErronkak) {
            ErronkaOrdutegia erronkaOrdutegia = kalkulatuErronkaOrdutegia(
                    erronka, egutegia, ordutegiBertsioak, taldekoModuluakIdz.keySet());
            ordutegiak.put(erronka.getId(), erronkaOrdutegia);
            Map<LocalDate, Integer> egunekoOrduak = moduloaId == null
                    ? erronkaOrdutegia.orokorra() : erronkaOrdutegia.modulua(moduloaId);
            List<EgunZutabea> egunak = egunekoOrduak.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> new EgunZutabea(e.getKey(), e.getValue()))
                    .toList();
            int programaOrduak = egunekoOrduak.values().stream().mapToInt(Integer::intValue).sum();
            erronkaZutabeak.add(new ErronkaZutabeMultzoa(erronka.getId(), indizea++, erronka.getIzena(),
                    erronka.getHasieraData(), erronka.getBukaeraData(),
                    erronka.getBukaeraData() != null && erronka.getBukaeraData().isBefore(LocalDate.now()),
                    egunak, programaOrduak));
        }

        List<Ikaslea> taldekoIkasleak = ikasleak
                .findByTaldea_IdOrderByAbizena1AscAbizena2AscIzenaAsc(taldeaId);
        List<Long> erronkaIds = erronkaZutabeak.stream().map(ErronkaZutabeMultzoa::id).toList();
        List<EthaziJarraipenGelaxka> gordetakoak = erronkaIds.isEmpty() ? List.of()
                : gelaxkak.findByEthaziKoadernoaIdAndErronkaIdIn(koadernoa.getId(), erronkaIds);
        Map<ErregistroGakoa, List<EthaziJarraipenGelaxka>> erregistroak = gordetakoak.stream()
                .collect(Collectors.groupingBy(g -> new ErregistroGakoa(
                        g.getIkaslea().getId(), g.getErronka().getId()), LinkedHashMap::new, Collectors.toList()));

        Map<Long, TaldeInfo> taldeak = taldeInfoa(erronkaZutabeak, taldekoIkasleak);
        List<IkasleOrdenatua> ordenatutakoak = taldekoIkasleak.stream()
                .map(i -> new IkasleOrdenatua(i, taldeak.getOrDefault(i.getId(), TaldeInfo.gabe())))
                .sorted(Comparator.comparingInt((IkasleOrdenatua i) -> i.taldea().ordena())
                        .thenComparing(i -> i.ikaslea().getIzenOsoa(), String.CASE_INSENSITIVE_ORDER))
                .toList();
        Map<Integer, Long> taldeKopuruak = ordenatutakoak.stream()
                .collect(Collectors.groupingBy(i -> i.taldea().ordena(), LinkedHashMap::new, Collectors.counting()));

        int muga = taldekoModuluak.stream().map(Moduloa::getMaila).filter(Objects::nonNull)
                .map(m -> m.getFaltenMugaPortzentaia()).filter(Objects::nonNull).findFirst().orElse(20);
        List<IkasleLerroa> ikasleLerroak = new ArrayList<>();
        Integer aurrekoTaldea = null;
        for (IkasleOrdenatua ordenatua : ordenatutakoak) {
            Ikaslea ikaslea = ordenatua.ikaslea();
            Map<Long, ErronkaIkasleLaburpena> laburpenak = new LinkedHashMap<>();
            Map<String, JarraipenGelaxka> egunGelaxkak = new HashMap<>();
            for (ErronkaZutabeMultzoa erronka : erronkaZutabeak) {
                List<EthaziJarraipenGelaxka> ikaslearenak = erregistroak.getOrDefault(
                        new ErregistroGakoa(ikaslea.getId(), erronka.id()), List.of()).stream()
                        .filter(g -> moduloaId == null || moduloaId.equals(g.getModuloa().getId()))
                        .toList();
                BigDecimal faltaOrduak = ikaslearenak.stream().map(this::faltaOrduak)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                double portzentaia = erronka.programaOrduak() == 0 ? 0d
                        : faltaOrduak.doubleValue() * 100d / erronka.programaOrduak();
                laburpenak.put(erronka.id(), new ErronkaIkasleLaburpena(
                        faltaOrduak, portzentaia, portzentaia >= muga,
                        moduloaId == null ? "Erronka suspendituta" : "Modulua suspendituta"));
                Map<LocalDate, List<EthaziJarraipenGelaxka>> egunarenArabera = ikaslearenak.stream()
                        .collect(Collectors.groupingBy(EthaziJarraipenGelaxka::getData));
                for (EgunZutabea eguna : erronka.egunak()) {
                    List<EthaziJarraipenGelaxka> egunekoak = egunarenArabera.getOrDefault(eguna.data(), List.of());
                    BigDecimal orduak = egunekoak.stream().map(this::faltaOrduak)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    boolean oharra = egunekoak.stream().anyMatch(g -> g.getOharra() != null && !g.getOharra().isBlank());
                    egunGelaxkak.put(gelaxkaGakoa(erronka.id(), eguna.data()),
                            new JarraipenGelaxka(orduak, oharra));
                }
            }
            boolean taldeHasiera = !Objects.equals(aurrekoTaldea, ordenatua.taldea().ordena());
            aurrekoTaldea = ordenatua.taldea().ordena();
            ikasleLerroak.add(new IkasleLerroa(ikaslea.getId(), ikaslea.getIzenOsoa(),
                    ordenatua.taldea().etiketa(), taldeHasiera,
                    taldeKopuruak.getOrDefault(ordenatua.taldea().ordena(), 1L).intValue(),
                    laburpenak, egunGelaxkak));
        }

        String filtroEtiketa = moduloaId == null ? "Orokorra"
                : moduluAukerak.stream().filter(m -> moduloaId.equals(m.id()))
                        .map(ModuluAukera::etiketa).findFirst().orElse("Orokorra");
        return new JarraipenaIkuspegia(ikasturtea.getIzena(), moduluAukerak, moduloaId,
                filtroEtiketa, muga, erronkaZutabeak, ikasleLerroak);
    }

    @Transactional(readOnly = true)
    public GelaxkaXehetasuna gelaxkaXehetasuna(EthaziKoadernoa koadernoa, Irakaslea irakaslea,
            boolean administratzailea, Long ikasleaId, Long erronkaId, LocalDate data, Long moduloaId) {
        Testuingurua t = balioztatuTestuingurua(koadernoa, ikasleaId, erronkaId, data);
        Set<Long> editagarriIds = moduluEditagarriak(koadernoa, irakaslea, administratzailea).stream()
                .map(Moduloa::getId).collect(Collectors.toSet());
        if (moduloaId != null && !editagarriIds.contains(moduloaId)) {
            throw new IllegalArgumentException("Ez duzu modulu horretako jarraipena editatzeko baimenik.");
        }
        Map<Long, EthaziJarraipenGelaxka> gordetakoak = gelaxkak
                .findByEthaziKoadernoaIdAndErronkaIdAndIkasleaIdAndData(
                        koadernoa.getId(), erronkaId, ikasleaId, data).stream()
                .collect(Collectors.toMap(g -> g.getModuloa().getId(), Function.identity()));
        List<ModuloGelaxka> modulukoGelaxkak = t.ordutegia().moduluka().entrySet().stream()
                .filter(e -> e.getValue().getOrDefault(data, 0) > 0)
                .filter(e -> moduloaId == null || moduloaId.equals(e.getKey()))
                .map(e -> {
                    Moduloa modulua = t.moduluak().get(e.getKey());
                    EthaziJarraipenGelaxka gordeta = gordetakoak.get(e.getKey());
                    return new ModuloGelaxka(e.getKey(), etiketa(modulua), e.getValue().get(data),
                            gordeta == null ? BigDecimal.ZERO : faltaOrduak(gordeta),
                            gordeta == null ? "" : Optional.ofNullable(gordeta.getOharra()).orElse(""),
                            moduloaId != null && editagarriIds.contains(e.getKey()));
                })
                .sorted(Comparator.comparing(ModuloGelaxka::modulua, String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (modulukoGelaxkak.isEmpty()) {
            throw new IllegalArgumentException("Egun hori ez da aukeratutako moduluaren klase-eguna.");
        }
        return new GelaxkaXehetasuna(ikasleaId, t.ikaslea().getIzenOsoa(), erronkaId,
                t.erronka().getIzena(), data, modulukoGelaxkak);
    }

    @Transactional
    public void gordeGelaxka(EthaziKoadernoa koadernoa, Irakaslea irakaslea,
            boolean administratzailea, GelaxkaGordeEskaera eskaera) {
        if (eskaera == null || eskaera.moduluak() == null || eskaera.moduluak().isEmpty()) {
            throw new IllegalArgumentException("Gutxienez modulu bateko datuak bidali behar dira.");
        }
        if (eskaera.aukeratutakoModuloaId() == null) {
            throw new IllegalArgumentException("Faltak edo oharrak sartzeko modulu konkretu bat aukeratu behar da.");
        }
        if (eskaera.moduluak().size() != 1
                || !eskaera.aukeratutakoModuloaId().equals(eskaera.moduluak().get(0).moduloaId())) {
            throw new IllegalArgumentException("Aukeratutako moduluaren datuak bakarrik gorde daitezke.");
        }
        Testuingurua t = balioztatuTestuingurua(
                koadernoa, eskaera.ikasleaId(), eskaera.erronkaId(), eskaera.data());
        Set<Long> editagarriIds = moduluEditagarriak(koadernoa, irakaslea, administratzailea).stream()
                .map(Moduloa::getId).collect(Collectors.toSet());
        Set<Long> errepikatuak = new HashSet<>();
        for (ModuloGelaxkaGorde sarrera : eskaera.moduluak()) {
            if (sarrera == null || sarrera.moduloaId() == null || !errepikatuak.add(sarrera.moduloaId())) {
                throw new IllegalArgumentException("Moduluen zerrenda ez da zuzena.");
            }
            if (!editagarriIds.contains(sarrera.moduloaId())) {
                throw new IllegalArgumentException("Ez duzu modulu horretako jarraipena editatzeko baimenik.");
            }
            Moduloa modulua = t.moduluak().get(sarrera.moduloaId());
            int gehienez = t.ordutegia().modulua(sarrera.moduloaId()).getOrDefault(eskaera.data(), 0);
            if (modulua == null || gehienez <= 0) {
                throw new IllegalArgumentException("Aukeratutako modulua ez dago erronkako egun horretan.");
            }
            BigDecimal orduak = sarrera.faltaOrduak() == null ? BigDecimal.ZERO
                    : sarrera.faltaOrduak().setScale(2, RoundingMode.HALF_UP);
            if (orduak.signum() < 0 || orduak.compareTo(BigDecimal.valueOf(gehienez)) > 0) {
                throw new IllegalArgumentException("Falta-orduak 0 eta " + gehienez + " artekoak izan behar dira.");
            }
            String oharra = sarrera.oharra() == null ? "" : sarrera.oharra().trim();
            if (oharra.length() > OHAR_MAX_LUZERA) {
                throw new IllegalArgumentException("Oharrak ezin ditu " + OHAR_MAX_LUZERA + " karaktere baino gehiago izan.");
            }
            Optional<EthaziJarraipenGelaxka> gordeta = gelaxkak
                    .findByEthaziKoadernoaIdAndErronkaIdAndIkasleaIdAndModuloaIdAndData(
                            koadernoa.getId(), eskaera.erronkaId(), eskaera.ikasleaId(),
                            sarrera.moduloaId(), eskaera.data());
            if (orduak.signum() == 0 && oharra.isBlank()) {
                gordeta.ifPresent(gelaxkak::delete);
                continue;
            }
            EthaziJarraipenGelaxka gelaxka = gordeta.orElseGet(EthaziJarraipenGelaxka::new);
            gelaxka.setEthaziKoadernoa(koadernoa);
            gelaxka.setErronka(t.erronka());
            gelaxka.setIkaslea(t.ikaslea());
            gelaxka.setModuloa(modulua);
            gelaxka.setData(eskaera.data());
            gelaxka.setFaltaOrduak(orduak);
            gelaxka.setOharra(oharra.isBlank() ? null : oharra);
            gelaxkak.save(gelaxka);
        }
    }

    private Testuingurua balioztatuTestuingurua(EthaziKoadernoa koadernoa, Long ikasleaId,
            Long erronkaId, LocalDate data) {
        if (koadernoa == null || koadernoa.getTaldea() == null || ikasleaId == null
                || erronkaId == null || data == null) {
            throw new IllegalArgumentException("Jarraipen-gelaxkaren datuak osatu gabe daude.");
        }
        Ikaslea ikaslea = ikasleak.findById(ikasleaId)
                .orElseThrow(() -> new IllegalArgumentException("Ikaslea ez da aurkitu."));
        if (ikaslea.getTaldea() == null || !koadernoa.getTaldea().getId().equals(ikaslea.getTaldea().getId())) {
            throw new IllegalArgumentException("Ikaslea ez da Ethazi koaderno honetakoa.");
        }
        Erronka erronka = erronkak.findWithModuluakById(erronkaId)
                .orElseThrow(() -> new IllegalArgumentException("Erronka ez da aurkitu."));
        if (data.isBefore(erronka.getHasieraData()) || data.isAfter(erronka.getBukaeraData())) {
            throw new IllegalArgumentException("Data ez dago erronkaren barruan.");
        }
        Long taldeaId = koadernoa.getTaldea().getId();
        Map<Long, Moduloa> taldekoModuluak = moduluak.findByTaldeaId(taldeaId).stream()
                .filter(Moduloa::isAktibo).collect(Collectors.toMap(Moduloa::getId, Function.identity()));
        boolean koadernoarena = koadernoa.getTaldea().getZikloa() != null && erronka.getZikloa() != null
                && Objects.equals(koadernoa.getTaldea().getZikloa().getId(), erronka.getZikloa().getId())
                && erronka.getModuluak().stream().map(Moduloa::getId).anyMatch(taldekoModuluak::containsKey);
        if (!koadernoarena || erronka.getIkasturtea() == null || erronka.getMaila() == null) {
            throw new IllegalArgumentException("Erronka ez da Ethazi koaderno honetakoa.");
        }
        Egutegia egutegia = egutegiak.findByIkasturtea_IdAndMaila_Id(
                erronka.getIkasturtea().getId(), erronka.getMaila().getId()).orElse(null);
        List<TaldeOrdutegia> bertsioak = taldeOrdutegiak
                .findByTaldea_IdAndIkasturtea_IdOrderByHasieraDataAsc(taldeaId, erronka.getIkasturtea().getId());
        ErronkaOrdutegia ordutegia = kalkulatuErronkaOrdutegia(
                erronka, egutegia, bertsioak, taldekoModuluak.keySet());
        return new Testuingurua(erronka, ikaslea, taldekoModuluak, ordutegia);
    }

    private List<Erronka> aukeratuErronkak(EthaziKoadernoa koadernoa, Ikasturtea ikasturtea,
            Set<Long> taldekoModuloIds) {
        Long mailaId = moduluak.findByTaldeaId(koadernoa.getTaldea().getId()).stream()
                .filter(Moduloa::isAktibo).map(Moduloa::getMaila).filter(Objects::nonNull)
                .map(m -> m.getId()).findFirst().orElse(null);
        List<Erronka> hautagaiak = erronkak.findWithModuluakByZikloaIdAndIkasturteaId(
                koadernoa.getTaldea().getZikloa().getId(), ikasturtea.getId()).stream()
                .filter(e -> mailaId == null || e.getMaila() == null || mailaId.equals(e.getMaila().getId()))
                .filter(e -> e.getModuluak().stream().map(Moduloa::getId).anyMatch(taldekoModuloIds::contains))
                .sorted(Comparator.comparing(Erronka::getHasieraData).thenComparing(Erronka::getId)).toList();
        Map<String, Erronka> bertsioBakarra = new LinkedHashMap<>();
        for (Erronka erronka : hautagaiak) {
            String gakoa = erronka.getBertsioTaldea() == null ? "id:" + erronka.getId()
                    : "bertsioa:" + erronka.getBertsioTaldea();
            bertsioBakarra.merge(gakoa, erronka, (aurrekoa, berria) ->
                    aurrekoa.getHizkuntza() != Hizkuntza.EUSKARA && berria.getHizkuntza() == Hizkuntza.EUSKARA
                            ? berria : aurrekoa);
        }
        return bertsioBakarra.values().stream()
                .sorted(Comparator.comparing(Erronka::getHasieraData).thenComparing(Erronka::getId)).toList();
    }

    private List<Moduloa> moduluEditagarriak(EthaziKoadernoa koadernoa, Irakaslea irakaslea,
            boolean administratzailea) {
        if (koadernoa == null || koadernoa.getTaldea() == null) return List.of();
        if (administratzailea) {
            return moduluak.findByTaldeaId(koadernoa.getTaldea().getId()).stream()
                    .filter(Moduloa::isAktibo).toList();
        }
        if (irakaslea == null || irakaslea.getId() == null) return List.of();
        return moduluak.findAktiboakByTaldeaIdAndIrakasleaId(
                koadernoa.getTaldea().getId(), irakaslea.getId());
    }

    private Map<Long, TaldeInfo> taldeInfoa(List<ErronkaZutabeMultzoa> erronkak,
            List<Ikaslea> taldekoIkasleak) {
        if (erronkak.isEmpty()) return Map.of();
        Set<Long> ikasleIds = taldekoIkasleak.stream().map(Ikaslea::getId).collect(Collectors.toSet());
        Map<Long, TaldeInfo> emaitza = new HashMap<>();
        for (ErronkaTaldeKidea kidea : taldeKideak.findByErronkaId(erronkak.get(0).id())) {
            if (kidea.getIkaslea() == null || !ikasleIds.contains(kidea.getIkaslea().getId())
                    || kidea.getTaldea() == null) continue;
            int ordena = kidea.getTaldea().getOrdena() == null ? Integer.MAX_VALUE - 1
                    : kidea.getTaldea().getOrdena();
            String etiketa = kidea.getTaldea().getIzena();
            if (etiketa == null || etiketa.isBlank()) etiketa = ordena + ". taldea";
            emaitza.put(kidea.getIkaslea().getId(), new TaldeInfo(ordena, etiketa));
        }
        return emaitza;
    }

    private ErronkaOrdutegia kalkulatuErronkaOrdutegia(Erronka erronka, Egutegia egutegia,
            List<TaldeOrdutegia> bertsioak, Set<Long> taldekoModuloIds) {
        Map<Long, NavigableMap<LocalDate, Integer>> moduluka = new LinkedHashMap<>();
        erronka.getModuluak().stream().filter(m -> taldekoModuloIds.contains(m.getId()))
                .forEach(m -> moduluka.put(m.getId(), new TreeMap<>()));
        NavigableMap<LocalDate, TaldeOrdutegia> ordutegiak = new TreeMap<>();
        Optional.ofNullable(bertsioak).orElse(List.of()).stream()
                .filter(o -> o.getHasieraData() != null).forEach(o -> ordutegiak.put(o.getHasieraData(), o));
        Map<LocalDate, EgunBerezi> egunBereziak = egutegia == null ? Map.of()
                : Optional.ofNullable(egutegia.getEgunBereziak()).orElse(List.of()).stream()
                        .filter(e -> e != null && e.getData() != null)
                        .collect(Collectors.toMap(EgunBerezi::getData, Function.identity(), (a, b) -> a));
        for (LocalDate data = erronka.getHasieraData(); !data.isAfter(erronka.getBukaeraData()); data = data.plusDays(1)) {
            if (!egutegiBarruan(data, egutegia)) continue;
            Astegunak asteguna = astegunEraginkorra(data, egunBereziak);
            Map.Entry<LocalDate, TaldeOrdutegia> bertsioa = ordutegiak.floorEntry(data);
            if (asteguna == null || bertsioa == null) continue;
            LocalDate unekoData = data;
            moduluka.forEach((moduloaId, egunak) -> {
                int orduak = (int) Optional.ofNullable(bertsioa.getValue().getLerroak()).orElse(List.of()).stream()
                        .filter(l -> asteguna == l.getAsteguna() && l.getModuloa() != null
                                && moduloaId.equals(l.getModuloa().getId())).count();
                if (orduak > 0) egunak.put(unekoData, orduak);
            });
        }
        Map<Long, Map<LocalDate, Integer>> emaitza = new LinkedHashMap<>();
        moduluka.forEach((id, egunak) -> emaitza.put(id, new LinkedHashMap<>(egunak)));
        return new ErronkaOrdutegia(emaitza);
    }

    private boolean egutegiBarruan(LocalDate data, Egutegia egutegia) {
        return egutegia == null
                || (egutegia.getHasieraData() == null || !data.isBefore(egutegia.getHasieraData()))
                && (egutegia.getBukaeraData() == null || !data.isAfter(egutegia.getBukaeraData()));
    }

    private Astegunak astegunEraginkorra(LocalDate data, Map<LocalDate, EgunBerezi> bereziak) {
        EgunBerezi berezia = bereziak.get(data);
        if (berezia != null && berezia.getMota() != null) {
            if (berezia.getMota() == EgunMota.EZ_LEKTIBOA || berezia.getMota() == EgunMota.JAIEGUNA) return null;
            if (berezia.getMota() == EgunMota.ORDEZKATUA) return berezia.getOrdezkatua();
        }
        return asteguna(data.getDayOfWeek());
    }

    private Astegunak asteguna(DayOfWeek eguna) {
        return switch (eguna) {
            case MONDAY -> Astegunak.ASTELEHENA;
            case TUESDAY -> Astegunak.ASTEARTEA;
            case WEDNESDAY -> Astegunak.ASTEAZKENA;
            case THURSDAY -> Astegunak.OSTEGUNA;
            case FRIDAY -> Astegunak.OSTIRALA;
            case SATURDAY -> Astegunak.LARUNBATA;
            case SUNDAY -> Astegunak.IGANDEA;
        };
    }

    private BigDecimal faltaOrduak(EthaziJarraipenGelaxka gelaxka) {
        return gelaxka.getFaltaOrduak() == null ? BigDecimal.ZERO : gelaxka.getFaltaOrduak();
    }

    private String etiketa(Moduloa modulua) {
        if (modulua == null) return "—";
        if (modulua.getKodea() != null && !modulua.getKodea().isBlank()) return modulua.getKodea();
        return modulua.getIzena() == null ? "—" : modulua.getIzena();
    }

    private static String gelaxkaGakoa(Long erronkaId, LocalDate data) {
        return erronkaId + "|" + data;
    }

    private record ErregistroGakoa(Long ikasleaId, Long erronkaId) {}
    private record IkasleOrdenatua(Ikaslea ikaslea, TaldeInfo taldea) {}
    private record TaldeInfo(int ordena, String etiketa) {
        static TaldeInfo gabe() { return new TaldeInfo(Integer.MAX_VALUE, "Talderik gabe"); }
    }
    private record Testuingurua(Erronka erronka, Ikaslea ikaslea, Map<Long, Moduloa> moduluak,
            ErronkaOrdutegia ordutegia) {}
    private record ErronkaOrdutegia(Map<Long, Map<LocalDate, Integer>> moduluka) {
        Map<LocalDate, Integer> modulua(Long moduloaId) {
            return moduluka.getOrDefault(moduloaId, Map.of());
        }
        Map<LocalDate, Integer> orokorra() {
            Map<LocalDate, Integer> emaitza = new TreeMap<>();
            moduluka.values().forEach(egunak -> egunak.forEach((data, orduak) -> emaitza.merge(data, orduak, Integer::sum)));
            return emaitza;
        }
    }

    public record ModuluAukera(Long id, String etiketa) {}
    public record EgunZutabea(LocalDate data, int orduak) {}
    public record ErronkaZutabeMultzoa(Long id, int indizea, String izena,
            LocalDate hasieraData, LocalDate bukaeraData, boolean lehenetsitaUzkurtuta,
            List<EgunZutabea> egunak, int programaOrduak) {
        public int zutabeKopurua() { return egunak.size() + 1; }
    }
    public record ErronkaIkasleLaburpena(BigDecimal faltaOrduak, double portzentaia,
            boolean suspendituta, String egoeraEtiketa) {}
    public record JarraipenGelaxka(BigDecimal faltaOrduak, boolean oharra) {}
    public record IkasleLerroa(Long id, String izenOsoa, String taldea, boolean taldeHasiera,
            int taldekoLerroKopurua, Map<Long, ErronkaIkasleLaburpena> laburpenak,
            Map<String, JarraipenGelaxka> gelaxkak) {
        public ErronkaIkasleLaburpena laburpena(Long erronkaId) { return laburpenak.get(erronkaId); }
        public JarraipenGelaxka gelaxka(Long erronkaId, LocalDate data) {
            return gelaxkak.getOrDefault(gelaxkaGakoa(erronkaId, data),
                    new JarraipenGelaxka(BigDecimal.ZERO, false));
        }
    }
    public record JarraipenaIkuspegia(String ikasturtea, List<ModuluAukera> moduluAukerak,
            Long aukeratutakoModuloaId, String filtroEtiketa, int faltenMugaPortzentaia,
            List<ErronkaZutabeMultzoa> erronkak, List<IkasleLerroa> ikasleak) {
        static JarraipenaIkuspegia hutsa() {
            return new JarraipenaIkuspegia("", List.of(), null, "Orokorra", 20, List.of(), List.of());
        }
    }
    public record ModuloGelaxka(Long moduloaId, String modulua, int gehienezkoOrduak,
            BigDecimal faltaOrduak, String oharra, boolean editagarria) {}
    public record GelaxkaXehetasuna(Long ikasleaId, String ikaslea, Long erronkaId,
            String erronka, LocalDate data, List<ModuloGelaxka> moduluak) {}
    public record ModuloGelaxkaGorde(Long moduloaId, BigDecimal faltaOrduak, String oharra) {}
    public record GelaxkaGordeEskaera(Long ikasleaId, Long erronkaId, LocalDate data,
            Long aukeratutakoModuloaId,
            List<ModuloGelaxkaGorde> moduluak) {}
}
