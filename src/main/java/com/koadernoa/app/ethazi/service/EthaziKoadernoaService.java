package com.koadernoa.app.ethazi.service;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.ethazi.entitateak.Erronka;
import com.koadernoa.app.ethazi.entitateak.EthaziKoadernoa;
import com.koadernoa.app.ethazi.entitateak.kronograma.ErronkaJardueraMultzoa;
import com.koadernoa.app.ethazi.entitateak.kronograma.ErronkaKronogramaJarduera;
import com.koadernoa.app.ethazi.entitateak.kronograma.KronogramaJardueraMota;
import com.koadernoa.app.ethazi.repository.ErronkaRepository;
import com.koadernoa.app.ethazi.repository.ErronkaJardueraMultzoaRepository;
import com.koadernoa.app.ethazi.repository.ErronkaKronogramaJardueraRepository;
import com.koadernoa.app.ethazi.repository.EthaziKoadernoaRepository;
import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunMota;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.TaldeOrdutegia;
import com.koadernoa.app.objektuak.ordutegiak.repository.TaldeOrdutegiaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EthaziKoadernoaService {
    private static final List<Astegunak> ASTE_ORDENA = List.of(
            Astegunak.ASTELEHENA, Astegunak.ASTEARTEA, Astegunak.ASTEAZKENA,
            Astegunak.OSTEGUNA, Astegunak.OSTIRALA);
    private static final List<String> MODULU_KOLOREAK = List.of(
            "#dbeafe", "#dcfce7", "#fef3c7", "#fce7f3", "#ede9fe",
            "#cffafe", "#ffedd5", "#e2e8f0", "#f3e8ff", "#ccfbf1");

    private final EthaziKoadernoaRepository ethaziKoadernoak;
    private final ModuloaRepository moduluak;
    private final IkasturteaRepository ikasturteak;
    private final TaldeOrdutegiaRepository taldeOrdutegiak;
    private final ErronkaRepository erronkak;
    private final ErronkaKronogramaJardueraRepository kronogramaJarduerak;
    private final ErronkaJardueraMultzoaRepository jardueraMultzoak;

    @Transactional
    public List<EthaziKoadernoa> lortuIrakaslearenKoadernoak(Irakaslea irakaslea) {
        if (irakaslea == null || irakaslea.getId() == null) return List.of();
        List<Taldea> taldeak = moduluak.findDistinctTaldeakByIrakasleaId(irakaslea.getId());
        for (Taldea taldea : taldeak) {
            ziurtatuKoadernoa(taldea);
        }
        List<Long> taldeIds = taldeak.stream().map(Taldea::getId).toList();
        return taldeIds.isEmpty() ? List.of()
                : ethaziKoadernoak.findByTaldeaIdInOrderByTaldeaIzenaAsc(taldeIds);
    }

    @Transactional(readOnly = true)
    public EthaziKoadernoa findById(Long id) {
        return ethaziKoadernoak.findWithTaldeaById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean irakasleakBadaukaSarbidea(Irakaslea irakaslea, EthaziKoadernoa koadernoa) {
        return irakaslea != null && irakaslea.getId() != null && koadernoa != null
                && koadernoa.getTaldea() != null
                && moduluak.existsByTaldeaIdAndIrakasleakIdAndAktiboTrue(
                        koadernoa.getTaldea().getId(), irakaslea.getId());
    }

    @Transactional(readOnly = true)
    public HasieraIkuspegia hasieraIkuspegia(EthaziKoadernoa koadernoa) {
        Ikasturtea ikasturtea = ikasturteak.findFirstByAktiboaTrueOrderByIdDesc().orElse(null);
        if (koadernoa == null || koadernoa.getTaldea() == null || ikasturtea == null) {
            return HasieraIkuspegia.hutsa();
        }
        Long taldeaId = koadernoa.getTaldea().getId();
        List<TaldeOrdutegia> bertsioak = taldeOrdutegiak
                .findByTaldea_IdAndIkasturtea_IdOrderByHasieraDataAsc(taldeaId, ikasturtea.getId());
        TaldeOrdutegia ordutegia = aukeratuOrdutegia(bertsioak, LocalDate.now());

        List<Moduloa> taldekoModuluak = moduluak.findByTaldeaId(taldeaId).stream()
                .sorted(Comparator.comparing(m -> etiketa(m).toLowerCase()))
                .toList();
        Set<Long> erronkakoModuluIds = erronkakoModuluIds(koadernoa.getTaldea(), ikasturtea, taldekoModuluak);
        Map<Long, String> koloreak = new LinkedHashMap<>();
        int koloreIndizea = 0;
        for (Moduloa modulua : taldekoModuluak) {
            if (erronkakoModuluIds.contains(modulua.getId())) {
                koloreak.put(modulua.getId(), MODULU_KOLOREAK.get(koloreIndizea++ % MODULU_KOLOREAK.size()));
            }
        }

        Map<String, OrdutegiGelaxka> gelaxkak = new LinkedHashMap<>();
        if (ordutegia != null) {
            ordutegia.getLerroak().forEach(lerroa -> {
                Moduloa modulua = lerroa.getModuloa();
                gelaxkak.put(gakoa(lerroa.getAsteguna(), lerroa.getOrduZenbakia()),
                        new OrdutegiGelaxka(etiketa(modulua), koloreak.get(modulua.getId()),
                                erronkakoModuluIds.contains(modulua.getId())));
            });
        }
        List<ModuluLegenda> legenda = taldekoModuluak.stream()
                .filter(m -> erronkakoModuluIds.contains(m.getId()))
                .map(m -> new ModuluLegenda(etiketa(m), koloreak.get(m.getId())))
                .toList();
        return new HasieraIkuspegia(ikasturtea.getIzena(), ASTE_ORDENA,
                java.util.stream.IntStream.rangeClosed(1, 12).boxed().toList(), gelaxkak, legenda,
                ordutegia == null ? null : ordutegia.getHasieraData());
    }

    @Transactional(readOnly = true)
    public ProgramazioaIkuspegia programazioaIkuspegia(EthaziKoadernoa koadernoa, Egutegia egutegia,
            Irakaslea irakaslea) {
        Ikasturtea ikasturtea = ikasturteak.findFirstByAktiboaTrueOrderByIdDesc().orElse(null);
        if (koadernoa == null || koadernoa.getTaldea() == null || koadernoa.getTaldea().getZikloa() == null
                || ikasturtea == null) {
            return new ProgramazioaIkuspegia("", List.of(), List.of(), List.of());
        }
        Long taldeaId = koadernoa.getTaldea().getId();
        List<Moduloa> taldekoModuluak = moduluak.findByTaldeaId(taldeaId).stream()
                .filter(Moduloa::isAktibo)
                .toList();
        Set<Long> taldekoModuloIds = taldekoModuluak.stream().map(Moduloa::getId)
                .collect(java.util.stream.Collectors.toSet());
        Long mailaId = taldekoModuluak.stream().map(Moduloa::getMaila).filter(java.util.Objects::nonNull)
                .map(m -> m.getId()).findFirst().orElse(null);

        List<Erronka> hautagaiak = erronkak.findWithModuluakByZikloaIdAndIkasturteaId(
                koadernoa.getTaldea().getZikloa().getId(), ikasturtea.getId()).stream()
                .filter(e -> mailaId == null || e.getMaila() == null || mailaId.equals(e.getMaila().getId()))
                .filter(e -> e.getModuluak().stream().map(Moduloa::getId).anyMatch(taldekoModuloIds::contains))
                .sorted(Comparator.comparing(Erronka::getHasieraData, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Erronka::getId))
                .toList();

        Map<String, Erronka> bertsioBakarra = new LinkedHashMap<>();
        for (Erronka erronka : hautagaiak) {
            String gakoa = erronka.getBertsioTaldea() == null ? "id:" + erronka.getId()
                    : "bertsioa:" + erronka.getBertsioTaldea();
            bertsioBakarra.merge(gakoa, erronka, (aurrekoa, berria) ->
                    aurrekoa.getHizkuntza() != com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza.EUSKARA
                            && berria.getHizkuntza() == com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza.EUSKARA
                                    ? berria : aurrekoa);
        }

        List<Erronka> aukeratutakoErronkak = bertsioBakarra.values().stream()
                .sorted(Comparator.comparing(Erronka::getHasieraData, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Erronka::getId)).toList();
        Map<Long, List<ErronkaKronogramaJarduera>> jarduerakErronkaka = aukeratutakoErronkak.isEmpty()
                ? Map.of()
                : kronogramaJarduerak.findByErronkaIdInOrderByHasieraDataAscBukaeraDataAscIdAsc(
                        aukeratutakoErronkak.stream().map(Erronka::getId).toList()).stream()
                        .collect(java.util.stream.Collectors.groupingBy(j -> j.getErronka().getId(),
                                LinkedHashMap::new, java.util.stream.Collectors.toList()));
        Map<Long, List<ErronkaJardueraMultzoa>> multzoakErronkaka = aukeratutakoErronkak.isEmpty()
                ? Map.of()
                : jardueraMultzoak.findByErronkaIdInOrderByOrdenaAscIdAsc(
                        aukeratutakoErronkak.stream().map(Erronka::getId).toList()).stream()
                        .collect(java.util.stream.Collectors.groupingBy(m -> m.getErronka().getId(),
                                LinkedHashMap::new, java.util.stream.Collectors.toList()));

        Set<Long> erronketakoModuloIds = aukeratutakoErronkak.stream()
                .flatMap(e -> e.getModuluak().stream()).map(Moduloa::getId)
                .collect(java.util.stream.Collectors.toSet());
        List<ModuluAukera> moduluAukerak = irakaslea == null || irakaslea.getId() == null ? List.of()
                : moduluak.findAktiboakByTaldeaIdAndIrakasleaId(taldeaId, irakaslea.getId()).stream()
                        .filter(m -> erronketakoModuloIds.contains(m.getId()))
                        .map(m -> new ModuluAukera(m.getId(), etiketa(m)))
                        .toList();
        Set<Long> irakaslearenModuloIds = moduluAukerak.stream().map(ModuluAukera::id)
                .collect(java.util.stream.Collectors.toSet());
        List<TaldeOrdutegia> ordutegiBertsioak = taldeOrdutegiak
                .findByTaldea_IdAndIkasturtea_IdOrderByHasieraDataAsc(taldeaId, ikasturtea.getId());

        List<ErronkaTxartela> txartelak = new java.util.ArrayList<>();
        int indizea = 1;
        for (Erronka erronka : aukeratutakoErronkak) {
            List<String> moduloEtiketak = erronka.getModuluak().stream()
                    .filter(m -> taldekoModuloIds.contains(m.getId()))
                    .sorted(Comparator.comparing(m -> etiketa(m).toLowerCase()))
                    .map(this::etiketa)
                    .toList();
            List<KronogramaJardueraTxartela> jardueraTxartelak = jarduerakErronkaka
                    .getOrDefault(erronka.getId(), List.of()).stream()
                    .map(j -> jardueraTxartela(j, irakaslearenModuloIds))
                    .toList();
            List<JardueraMultzoTxartela> multzoTxartelak = multzoakErronkaka
                    .getOrDefault(erronka.getId(), List.of()).stream()
                    .map(m -> {
                        List<KronogramaJardueraTxartela> multzokoak = jardueraTxartelak.stream()
                                .filter(j -> m.getId().equals(j.multzoaId())).toList();
                        LocalDate hasiera = multzokoak.stream().map(KronogramaJardueraTxartela::hasieraData)
                                .filter(java.util.Objects::nonNull).min(LocalDate::compareTo).orElse(null);
                        LocalDate bukaera = multzokoak.stream().map(KronogramaJardueraTxartela::bukaeraData)
                                .filter(java.util.Objects::nonNull).max(LocalDate::compareTo).orElse(null);
                        return new JardueraMultzoTxartela(m.getId(), m.getIzena(), m.getOrdena(),
                                hasiera, bukaera, multzokoak);
                    })
                    .toList();
            List<KronogramaJardueraTxartela> multzokatuGabekoak = jardueraTxartelak.stream()
                    .filter(j -> j.multzoaId() == null).toList();
            List<ModuluAukera> erronkakoModuluAukerak = erronka.getModuluak().stream()
                    .filter(m -> irakaslearenModuloIds.contains(m.getId()))
                    .sorted(Comparator.comparing(m -> etiketa(m).toLowerCase()))
                    .map(m -> new ModuluAukera(m.getId(), etiketa(m)))
                    .toList();
            List<ModuluOrduEgoera> moduluOrduak = erronka.getModuluak().stream()
                    .filter(m -> irakaslearenModuloIds.contains(m.getId()))
                    .map(m -> new ModuluOrduEgoera(m.getId(),
                            kalkulatuOrduErabilgarriak(erronka, m, egutegia, ordutegiBertsioak),
                            kalkulatuOrduProgramatuak(m.getId(),
                                    jarduerakErronkaka.getOrDefault(erronka.getId(), List.of()))))
                    .toList();
            txartelak.add(new ErronkaTxartela(erronka.getId(), indizea++, erronka.getIzena(),
                    erronka.getDeskribapena(), erronka.getHasieraData(), erronka.getBukaeraData(),
                    moduloEtiketak, jardueraTxartelak, erronkakoModuluAukerak,
                    multzoTxartelak, multzokatuGabekoak, moduluOrduak));
        }
        return new ProgramazioaIkuspegia(ikasturtea.getIzena(), txartelak,
                kalkulatuEbaluazioTarteak(txartelak, egutegia), moduluAukerak);
    }

    private KronogramaJardueraTxartela jardueraTxartela(ErronkaKronogramaJarduera jarduera,
            Set<Long> irakaslearenModuloIds) {
        boolean orokorra = jarduera.getMota() == KronogramaJardueraMota.KOMUNA;
        Moduloa jabea = jarduera.getModuloa();
        Moduloa aurreikusitakoa = jarduera.getAurreikusitakoModuloa();
        String badgea = orokorra ? "Orokorra" : etiketa(jabea);
        return new KronogramaJardueraTxartela(jarduera.getId(), jarduera.getIzenburua(),
                jarduera.getDeskribapena(), jarduera.getHasieraData(), jarduera.getBukaeraData(),
                jarduera.getIraupenOrduak(), orokorra, jabea == null ? null : jabea.getId(), badgea,
                aurreikusitakoa == null ? null : aurreikusitakoa.getId(),
                aurreikusitakoa == null ? null : etiketa(aurreikusitakoa),
                jarduera.getOrdena() == null ? 0 : jarduera.getOrdena(),
                !orokorra && jabea != null && irakaslearenModuloIds.contains(jabea.getId()),
                jarduera.getMultzoa() == null ? null : jarduera.getMultzoa().getId());
    }

    private List<EbaluazioTartea> kalkulatuEbaluazioTarteak(List<ErronkaTxartela> erronkak, Egutegia egutegia) {
        if (erronkak.isEmpty() || egutegia == null || egutegia.getHasieraData() == null
                || egutegia.getBukaeraData() == null) return List.of();
        List<DataTartea> ebaluazioak = new java.util.ArrayList<>();
        LocalDate hasiera = egutegia.getHasieraData();
        if (egutegia.getLehenEbalBukaera() != null) {
            ebaluazioak.add(new DataTartea("1. ebal", "ebal1", hasiera, egutegia.getLehenEbalBukaera()));
            hasiera = egutegia.getLehenEbalBukaera().plusDays(1);
        }
        if (egutegia.getBigarrenEbalBukaera() != null) {
            ebaluazioak.add(new DataTartea(ebaluazioak.isEmpty() ? "1. ebal" : "2. ebal",
                    ebaluazioak.isEmpty() ? "ebal1" : "ebal2", hasiera, egutegia.getBigarrenEbalBukaera()));
            hasiera = egutegia.getBigarrenEbalBukaera().plusDays(1);
        }
        String azkenIzena = (ebaluazioak.size() + 1) + ". ebal";
        String azkenKolorea = "ebal" + Math.min(3, ebaluazioak.size() + 1);
        ebaluazioak.add(new DataTartea(azkenIzena, azkenKolorea, hasiera, egutegia.getBukaeraData()));

        List<EbaluazioTartea> emaitza = new java.util.ArrayList<>();
        for (DataTartea ebal : ebaluazioak) {
            int lehen = -1, azken = -1;
            for (int i = 0; i < erronkak.size(); i++) {
                ErronkaTxartela erronka = erronkak.get(i);
                if (erronka.hasieraData() == null || erronka.bukaeraData() == null) continue;
                if (!erronka.bukaeraData().isBefore(ebal.hasiera()) && !erronka.hasieraData().isAfter(ebal.bukaera())) {
                    if (lehen < 0) lehen = i;
                    azken = i;
                }
            }
            if (lehen < 0) continue;
            ErronkaTxartela lehenErronka = erronkak.get(lehen);
            ErronkaTxartela azkenErronka = erronkak.get(azken);
            double hasieraRatioa = ratioa(lehenErronka, ebal.hasiera(), false);
            double bukaeraRatioa = ratioa(azkenErronka, ebal.bukaera(), true);
            emaitza.add(new EbaluazioTartea(ebal.izena(), ebal.kolorea(), lehenErronka.indizea(),
                    hasieraRatioa, azkenErronka.indizea(), bukaeraRatioa));
        }
        return emaitza;
    }

    private double ratioa(ErronkaTxartela erronka, LocalDate data, boolean amaiera) {
        long egunGuztiak = Math.max(1, ChronoUnit.DAYS.between(erronka.hasieraData(), erronka.bukaeraData()) + 1);
        long kokapena = ChronoUnit.DAYS.between(erronka.hasieraData(), data) + (amaiera ? 1 : 0);
        return Math.max(0d, Math.min(1d, (double) kokapena / egunGuztiak));
    }

    private int kalkulatuOrduErabilgarriak(Erronka erronka, Moduloa modulua, Egutegia egutegia,
            List<TaldeOrdutegia> bertsioak) {
        if (erronka.getHasieraData() == null || erronka.getBukaeraData() == null) return 0;
        Map<LocalDate, EgunBerezi> egunBereziak = egutegia == null ? Map.of()
                : Optional.ofNullable(egutegia.getEgunBereziak()).orElse(List.of()).stream()
                        .filter(e -> e != null && e.getData() != null)
                        .collect(java.util.stream.Collectors.toMap(EgunBerezi::getData, e -> e, (a, b) -> a));
        int guztira = 0;
        for (LocalDate data = erronka.getHasieraData(); !data.isAfter(erronka.getBukaeraData());
                data = data.plusDays(1)) {
            if (!egutegiBarruan(data, egutegia)) continue;
            Astegunak asteguna = astegunEraginkorra(data, egunBereziak);
            if (asteguna == null) continue;
            LocalDate unekoData = data;
            TaldeOrdutegia ordutegia = bertsioak.stream()
                    .filter(o -> o.getHasieraData() != null && !o.getHasieraData().isAfter(unekoData))
                    .max(Comparator.comparing(TaldeOrdutegia::getHasieraData)).orElse(null);
            if (ordutegia == null) continue;
            guztira += (int) Optional.ofNullable(ordutegia.getLerroak()).orElse(List.of()).stream()
                    .filter(l -> l.getModuloa() != null && modulua.getId().equals(l.getModuloa().getId())
                            && asteguna == l.getAsteguna())
                    .count();
        }
        return guztira;
    }

    private BigDecimal kalkulatuOrduProgramatuak(Long moduloaId,
            List<ErronkaKronogramaJarduera> jarduerak) {
        return jarduerak.stream()
                .filter(j -> j.getIraupenOrduak() != null)
                .filter(j -> j.getMota() == KronogramaJardueraMota.MODULUA && j.getModuloa() != null
                            && moduloaId.equals(j.getModuloa().getId())
                        || j.getMota() == KronogramaJardueraMota.KOMUNA
                            && j.getAurreikusitakoModuloa() != null
                            && moduloaId.equals(j.getAurreikusitakoModuloa().getId()))
                .map(ErronkaKronogramaJarduera::getIraupenOrduak)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean egutegiBarruan(LocalDate data, Egutegia egutegia) {
        return egutegia == null
                || (egutegia.getHasieraData() == null || !data.isBefore(egutegia.getHasieraData()))
                && (egutegia.getBukaeraData() == null || !data.isAfter(egutegia.getBukaeraData()));
    }

    private Astegunak astegunEraginkorra(LocalDate data, Map<LocalDate, EgunBerezi> egunBereziak) {
        EgunBerezi berezia = egunBereziak.get(data);
        if (berezia != null && berezia.getMota() != null) {
            if (berezia.getMota() == EgunMota.EZ_LEKTIBOA || berezia.getMota() == EgunMota.JAIEGUNA) return null;
            if (berezia.getMota() == EgunMota.ORDEZKATUA) return berezia.getOrdezkatua();
        }
        return switch (data.getDayOfWeek()) {
            case MONDAY -> Astegunak.ASTELEHENA;
            case TUESDAY -> Astegunak.ASTEARTEA;
            case WEDNESDAY -> Astegunak.ASTEAZKENA;
            case THURSDAY -> Astegunak.OSTEGUNA;
            case FRIDAY -> Astegunak.OSTIRALA;
            case SATURDAY -> Astegunak.LARUNBATA;
            case SUNDAY -> Astegunak.IGANDEA;
        };
    }

    private void ziurtatuKoadernoa(Taldea taldea) {
        if (ethaziKoadernoak.findByTaldeaId(taldea.getId()).isPresent()) return;
        EthaziKoadernoa berria = new EthaziKoadernoa();
        berria.setTaldea(taldea);
        ethaziKoadernoak.saveAndFlush(berria);
    }

    private TaldeOrdutegia aukeratuOrdutegia(List<TaldeOrdutegia> bertsioak, LocalDate data) {
        return bertsioak.stream().filter(o -> !o.getHasieraData().isAfter(data))
                .max(Comparator.comparing(TaldeOrdutegia::getHasieraData))
                .orElseGet(() -> bertsioak.stream().findFirst().orElse(null));
    }

    private Set<Long> erronkakoModuluIds(Taldea taldea, Ikasturtea ikasturtea, List<Moduloa> taldekoModuluak) {
        if (taldea.getZikloa() == null) return Set.of();
        Set<Long> taldekoIds = taldekoModuluak.stream().map(Moduloa::getId)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> emaitza = new LinkedHashSet<>();
        for (Erronka erronka : erronkak.findWithModuluakByZikloaIdAndIkasturteaId(
                taldea.getZikloa().getId(), ikasturtea.getId())) {
            erronka.getModuluak().stream().map(Moduloa::getId).filter(taldekoIds::contains).forEach(emaitza::add);
        }
        return emaitza;
    }

    private String etiketa(Moduloa modulua) {
        if (modulua == null) return "—";
        if (modulua.getKodea() != null && !modulua.getKodea().isBlank()) return modulua.getKodea();
        return modulua.getIzena() != null && !modulua.getIzena().isBlank() ? modulua.getIzena() : "—";
    }

    private String gakoa(Astegunak eguna, Integer ordua) {
        return eguna.name() + ":" + ordua;
    }

    public record OrdutegiGelaxka(String modulua, String kolorea, boolean erronkan) {}
    public record ModuluLegenda(String modulua, String kolorea) {}
    public record ModuluAukera(Long id, String etiketa) {}
    public record KronogramaJardueraTxartela(Long id, String izenburua, String deskribapena,
            LocalDate hasieraData, LocalDate bukaeraData, BigDecimal iraupenOrduak, boolean orokorra,
            Long moduloaId, String badgea, Long aurreikusitakoModuloaId,
            String aurreikusitakoModuloa, int ordena,
            boolean editagarria, Long multzoaId) {}
    public record JardueraMultzoTxartela(Long id, String izena, int ordena,
            LocalDate hasieraData, LocalDate bukaeraData,
            List<KronogramaJardueraTxartela> jarduerak) {}
    public record ErronkaTxartela(Long id, int indizea, String izena, String deskribapena,
            LocalDate hasieraData, LocalDate bukaeraData, List<String> moduluak,
            List<KronogramaJardueraTxartela> jarduerak, List<ModuluAukera> moduluAukerak,
            List<JardueraMultzoTxartela> multzoak,
            List<KronogramaJardueraTxartela> multzokatuGabekoJarduerak,
            List<ModuluOrduEgoera> moduluOrduak) {}
    public record ModuluOrduEgoera(Long moduloaId, int orduErabilgarriak,
            BigDecimal orduProgramatuak) {}
    public record EbaluazioTartea(String izena, String kolorea, int hasieraErronka,
            double hasieraRatioa, int bukaeraErronka, double bukaeraRatioa) {}
    public record ProgramazioaIkuspegia(String ikasturtea, List<ErronkaTxartela> erronkak,
            List<EbaluazioTartea> ebaluazioTarteak, List<ModuluAukera> moduluAukerak) {}
    private record DataTartea(String izena, String kolorea, LocalDate hasiera, LocalDate bukaera) {}
    public record HasieraIkuspegia(String ikasturtea, List<Astegunak> egunak, List<Integer> orduak,
            Map<String, OrdutegiGelaxka> gelaxkak, List<ModuluLegenda> legenda, LocalDate ordutegiHasiera) {
        static HasieraIkuspegia hutsa() {
            return new HasieraIkuspegia("", ASTE_ORDENA,
                    java.util.stream.IntStream.rangeClosed(1, 12).boxed().toList(), Map.of(), List.of(), null);
        }
    }
}
