package com.koadernoa.app.ethazi.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import com.koadernoa.app.ethazi.entitateak.EthaziDenboralizazioJarduera;
import com.koadernoa.app.ethazi.entitateak.EthaziKoadernoa;
import com.koadernoa.app.ethazi.entitateak.kronograma.KronogramaJardueraMota;
import com.koadernoa.app.ethazi.repository.EthaziDenboralizazioJardueraRepository;
import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.service.EgutegiaService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoOrdutegiBlokeaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.SaioaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.AsistentziaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EthaziDenboralizazioaService {
    private static final Locale EU_LOCALE = Locale.forLanguageTag("eu-ES");

    private final EthaziKoadernoaService ethaziKoadernoaService;
    private final ModuloaRepository moduloaRepository;
    private final KoadernoaRepository koadernoaRepository;
    private final KoadernoOrdutegiBlokeaRepository blokeRepository;
    private final SaioaRepository saioaRepository;
    private final AsistentziaService asistentziaService;
    private final EgutegiaService egutegiaService;
    private final EthaziDenboralizazioJardueraRepository denboralizazioJarduerak;
    private final KronogramaService kronogramaService;

    @Transactional
    public int bolkatu(EthaziKoadernoa ethaziKoadernoa, Egutegia egutegia,
            Irakaslea irakaslea, Long erronkaId, Authentication auth) {
        boolean koadernokoa = ethaziKoadernoaService
                .programazioaIkuspegia(ethaziKoadernoa, egutegia, irakaslea).erronkak().stream()
                .anyMatch(e -> e.id().equals(erronkaId));
        if (!koadernokoa) {
            throw new IllegalArgumentException("Erronka ez da Ethazi koaderno honetakoa.");
        }
        KronogramaService.DenboralizazioBanaketa banaketa =
                kronogramaService.denboralizazioBanaketa(erronkaId, auth);
        denboralizazioJarduerak.deleteByEthaziKoadernoaIdAndErronkaId(
                ethaziKoadernoa.getId(), erronkaId);
        List<EthaziDenboralizazioJarduera> berriak = banaketa.jarduerak().stream().map(egunekoa -> {
            var jatorria = egunekoa.jarduera();
            EthaziDenboralizazioJarduera berria = new EthaziDenboralizazioJarduera();
            berria.setEthaziKoadernoa(ethaziKoadernoa);
            berria.setErronka(banaketa.erronka());
            berria.setErronkaIzena(banaketa.erronka().getIzena());
            berria.setJatorrizkoJarduera(jatorria);
            berria.setData(egunekoa.data());
            berria.setIzenburua(jatorria.getIzenburua());
            berria.setDeskribapena(jatorria.getDeskribapena());
            berria.setMota(jatorria.getMota());
            berria.setModuloa(jatorria.getModuloa());
            berria.setAurreikusitakoModuloa(jatorria.getAurreikusitakoModuloa());
            berria.setOrduak(egunekoa.orduak());
            return berria;
        }).toList();
        denboralizazioJarduerak.saveAll(berriak);
        return berriak.size();
    }

    @Transactional(readOnly = true)
    public DenboralizazioaIkuspegia prestatu(EthaziKoadernoa ethaziKoadernoa, Egutegia egutegia,
            Irakaslea irakaslea, Long aukeratutakoModuloaId, int urtea, int hilabetea) {
        YearMonth hilabeteaData = YearMonth.of(urtea, hilabetea);
        List<EthaziKoadernoaService.ModuluAukera> moduluAukerak = irakaslea == null
                || irakaslea.getId() == null || ethaziKoadernoa.getTaldea() == null ? List.of()
                : moduloaRepository.findAktiboakByTaldeaIdAndIrakasleaId(
                        ethaziKoadernoa.getTaldea().getId(), irakaslea.getId()).stream()
                        .map(m -> new EthaziKoadernoaService.ModuluAukera(m.getId(), moduloEtiketa(m)))
                        .toList();
        Set<Long> irakaslearenModuloIds = moduluAukerak.stream()
                .map(EthaziKoadernoaService.ModuluAukera::id).collect(Collectors.toSet());
        Long moduloaId = irakaslearenModuloIds.contains(aukeratutakoModuloaId)
                ? aukeratutakoModuloaId : null;

        List<EthaziDenboralizazioJarduera> gordetakoak = denboralizazioJarduerak
                .findByEthaziKoadernoaIdAndDataBetweenOrderByDataAscIdAsc(
                        ethaziKoadernoa.getId(), hilabeteaData.atDay(1), hilabeteaData.atEndOfMonth());
        Map<LocalDate, List<EgunekoJarduera>> jarduerakEgunka = new LinkedHashMap<>();
        gordetakoak.stream()
                .filter(j -> j.getMota() == KronogramaJardueraMota.KOMUNA
                        || moduloaId != null && j.getModuloa() != null
                                && moduloaId.equals(j.getModuloa().getId())
                        || moduloaId == null && j.getModuloa() != null
                                && irakaslearenModuloIds.contains(j.getModuloa().getId()))
                .map(this::egunekoJarduera)
                .sorted(Comparator.comparing(EgunekoJarduera::data)
                        .thenComparing(EgunekoJarduera::izenburua, String.CASE_INSENSITIVE_ORDER))
                .forEach(j -> jarduerakEgunka.computeIfAbsent(j.data(), __ -> new ArrayList<>()).add(j));

        List<AsistentziaModulua> asistentziaModuluak = prestatuAsistentziaModuluak(
                moduluAukerak, moduloaId, egutegia, hilabeteaData);
        Map<String, String> egunKlaseak = egutegia == null ? Map.of()
                : egutegiaService.kalkulatuKlaseak(egutegia);
        List<List<Eguna>> asteak = prestatuAsteak(hilabeteaData, jarduerakEgunka,
                asistentziaModuluak, egunKlaseak);

        String hilabeteEtiketa = hilabeteaData.atDay(1)
                .format(DateTimeFormatter.ofPattern("LLLL yyyy", EU_LOCALE));
        String ikasturtea = egutegia.getIkasturtea() == null ? "" : egutegia.getIkasturtea().getIzena();
        return new DenboralizazioaIkuspegia(ikasturtea, hilabeteEtiketa,
                urtea, hilabetea, moduloaId, moduluAukerak, asteak);
    }

    @Transactional(readOnly = true)
    public Koadernoa lortuAsistentziaKoadernoa(EthaziKoadernoa ethaziKoadernoa,
            Irakaslea irakaslea, Long moduloaId) {
        if (ethaziKoadernoa == null || ethaziKoadernoa.getTaldea() == null || irakaslea == null
                || irakaslea.getId() == null || moduloaId == null) return null;
        boolean baimenduta = moduloaRepository
                .findAktiboakByTaldeaIdAndIrakasleaId(
                        ethaziKoadernoa.getTaldea().getId(), irakaslea.getId()).stream()
                .map(Moduloa::getId).anyMatch(moduloaId::equals);
        if (!baimenduta) return null;
        return koadernoaRepository.findByModuloaIdInAktiboIkasturtea(moduloaId).stream()
                .findFirst().orElse(null);
    }

    private List<AsistentziaModulua> prestatuAsistentziaModuluak(
            List<EthaziKoadernoaService.ModuluAukera> aukerak, Long moduloaId,
            Egutegia egutegia, YearMonth hilabetea) {
        if (egutegia == null) return List.of();
        return aukerak.stream()
                .filter(aukera -> moduloaId == null || moduloaId.equals(aukera.id()))
                .map(aukera -> asistentziaModulua(aukera, egutegia, hilabetea))
                .toList();
    }

    private AsistentziaModulua asistentziaModulua(EthaziKoadernoaService.ModuluAukera aukera,
            Egutegia egutegia, YearMonth hilabetea) {
        Koadernoa koadernoa = koadernoaRepository.findByModuloaIdInAktiboIkasturtea(aukera.id())
                .stream().findFirst().orElse(null);
        if (koadernoa == null) return new AsistentziaModulua(aukera.id(), aukera.etiketa(), Set.of(), Set.of());

        NavigableMap<LocalDate, Set<Astegunak>> astegunakDatarenArabera = new TreeMap<>();
        LocalDate ikasturteHasiera = egutegia == null ? hilabetea.atDay(1) : egutegia.getHasieraData();
        for (var blokea : blokeRepository.findByKoadernoa_Id(koadernoa.getId())) {
            LocalDate hasiera = blokea.getHasieraData() == null ? ikasturteHasiera : blokea.getHasieraData();
            if (hasiera == null) hasiera = hilabetea.atDay(1);
            if (blokea.isTarteHutsa()) {
                astegunakDatarenArabera.putIfAbsent(hasiera, new HashSet<>());
            } else if (!blokea.isDualOrdutegia() && blokea.getAsteguna() != null
                    && blokea.getIraupenaSlot() > 0) {
                astegunakDatarenArabera.computeIfAbsent(hasiera, __ -> new HashSet<>())
                        .add(blokea.getAsteguna());
            }
        }
        Set<LocalDate> egunak = asistentziaService.kalkulatuAsistentziaEgunak(
                astegunakDatarenArabera, egutegia, hilabetea.getYear(), hilabetea.getMonthValue())
                .entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey)
                .map(LocalDate::parse).collect(Collectors.toSet());
        Set<LocalDate> irekita = saioaRepository
                .findByKoadernoaIdAndDataBetweenOrderByDataAscHasieraSlotAsc(
                        koadernoa.getId(), hilabetea.atDay(1), hilabetea.atEndOfMonth()).stream()
                .map(s -> s.getData()).collect(Collectors.toSet());
        return new AsistentziaModulua(aukera.id(), aukera.etiketa(), egunak, irekita);
    }

    private List<List<Eguna>> prestatuAsteak(YearMonth hilabetea,
            Map<LocalDate, List<EgunekoJarduera>> jarduerakEgunka,
            List<AsistentziaModulua> asistentziaModuluak, Map<String, String> egunKlaseak) {
        LocalDate hasiera = hilabetea.atDay(1).with(DayOfWeek.MONDAY);
        LocalDate bukaera = hilabetea.atEndOfMonth().with(DayOfWeek.FRIDAY);
        List<List<Eguna>> asteak = new ArrayList<>();
        for (LocalDate asteHasiera = hasiera; !asteHasiera.isAfter(bukaera); asteHasiera = asteHasiera.plusWeeks(1)) {
            List<Eguna> astea = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                LocalDate data = asteHasiera.plusDays(i);
                List<AsistentziaAukera> asistentziak = asistentziaModuluak.stream()
                        .filter(modulua -> modulua.egunak().contains(data))
                        .map(modulua -> new AsistentziaAukera(modulua.moduloaId(), modulua.etiketa(),
                                modulua.irekita().contains(data)))
                        .toList();
                astea.add(new Eguna(data, data.getMonthValue() == hilabetea.getMonthValue(),
                        egunKlaseak.getOrDefault(data.toString(), ""),
                        jarduerakEgunka.getOrDefault(data, List.of()), asistentziak));
            }
            asteak.add(astea);
        }
        return asteak;
    }

    private record AsistentziaModulua(Long moduloaId, String etiketa,
            Set<LocalDate> egunak, Set<LocalDate> irekita) {}

    private EgunekoJarduera egunekoJarduera(EthaziDenboralizazioJarduera jarduera) {
        boolean orokorra = jarduera.getMota() == KronogramaJardueraMota.KOMUNA;
        Moduloa moduloa = jarduera.getModuloa();
        String badgea = orokorra ? "Orokorra" : moduloEtiketa(moduloa);
        return new EgunekoJarduera(jarduera.getId(), jarduera.getErronka().getId(),
                jarduera.getErronkaIzena(), jarduera.getIzenburua(),
                jarduera.getDeskribapena(), badgea, orokorra, jarduera.getData(),
                jarduera.getOrduak());
    }

    private String moduloEtiketa(Moduloa moduloa) {
        if (moduloa == null) return "Modulua";
        return moduloa.getKodea() == null || moduloa.getKodea().isBlank()
                ? moduloa.getIzena() : moduloa.getKodea();
    }

    public record EgunekoJarduera(Long id, Long erronkaId, String erronka, String izenburua,
            String deskribapena, String badgea, boolean orokorra,
            LocalDate data, BigDecimal orduak) {}
    public record AsistentziaAukera(Long moduloaId, String etiketa, boolean irekita) {}
    public record Eguna(LocalDate data, boolean hilabeteAktibokoa, String klasea,
            List<EgunekoJarduera> jarduerak, List<AsistentziaAukera> asistentziak) {}
    public record DenboralizazioaIkuspegia(String ikasturtea, String hilabeteEtiketa,
            int urtea, int hilabetea, Long aukeratutakoModuloaId,
            List<EthaziKoadernoaService.ModuluAukera> moduluAukerak,
            List<List<Eguna>> asteak) {}
}
