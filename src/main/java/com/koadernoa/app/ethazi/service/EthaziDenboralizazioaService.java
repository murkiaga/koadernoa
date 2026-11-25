package com.koadernoa.app.ethazi.service;

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

import com.koadernoa.app.ethazi.entitateak.EthaziKoadernoa;
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

    @Transactional(readOnly = true)
    public DenboralizazioaIkuspegia prestatu(EthaziKoadernoa ethaziKoadernoa, Egutegia egutegia,
            Irakaslea irakaslea, Long aukeratutakoModuloaId, int urtea, int hilabetea) {
        YearMonth hilabeteaData = YearMonth.of(urtea, hilabetea);
        var programazioa = ethaziKoadernoaService.programazioaIkuspegia(
                ethaziKoadernoa, egutegia, irakaslea);
        List<EthaziKoadernoaService.ModuluAukera> moduluAukerak = programazioa.moduluAukerak();
        Set<Long> irakaslearenModuloIds = moduluAukerak.stream()
                .map(EthaziKoadernoaService.ModuluAukera::id).collect(Collectors.toSet());
        Long moduloaId = irakaslearenModuloIds.contains(aukeratutakoModuloaId)
                ? aukeratutakoModuloaId : null;

        Map<Long, String> erronkaIzenak = programazioa.erronkak().stream()
                .collect(Collectors.toMap(EthaziKoadernoaService.ErronkaTxartela::id,
                        EthaziKoadernoaService.ErronkaTxartela::izena, (a, b) -> a,
                        LinkedHashMap::new));
        List<EgunekoJarduera> jarduerak = programazioa.erronkak().stream()
                .flatMap(erronka -> erronka.jarduerak().stream()
                        .filter(jarduera -> jarduera.orokorra()
                                || moduloaId != null && moduloaId.equals(jarduera.moduloaId())
                                || moduloaId == null && irakaslearenModuloIds.contains(jarduera.moduloaId()))
                        .map(jarduera -> new EgunekoJarduera(jarduera.id(), erronka.id(),
                                erronkaIzenak.get(erronka.id()), jarduera.izenburua(),
                                jarduera.deskribapena(), jarduera.badgea(), jarduera.orokorra(),
                                jarduera.hasieraData(), jarduera.bukaeraData())))
                .sorted(Comparator.comparing(EgunekoJarduera::hasieraData)
                        .thenComparing(EgunekoJarduera::izenburua, String.CASE_INSENSITIVE_ORDER))
                .toList();

        Map<LocalDate, List<EgunekoJarduera>> jarduerakEgunka = new LinkedHashMap<>();
        for (EgunekoJarduera jarduera : jarduerak) {
            LocalDate hasiera = jarduera.hasieraData().isBefore(hilabeteaData.atDay(1))
                    ? hilabeteaData.atDay(1) : jarduera.hasieraData();
            LocalDate bukaera = jarduera.bukaeraData().isAfter(hilabeteaData.atEndOfMonth())
                    ? hilabeteaData.atEndOfMonth() : jarduera.bukaeraData();
            for (LocalDate data = hasiera; !data.isAfter(bukaera); data = data.plusDays(1)) {
                if (data.getDayOfWeek() != DayOfWeek.SATURDAY
                        && data.getDayOfWeek() != DayOfWeek.SUNDAY) {
                    jarduerakEgunka.computeIfAbsent(data, __ -> new ArrayList<>()).add(jarduera);
                }
            }
        }

        List<AsistentziaModulua> asistentziaModuluak = prestatuAsistentziaModuluak(
                moduluAukerak, moduloaId, egutegia, hilabeteaData);
        Map<String, String> egunKlaseak = egutegia == null ? Map.of()
                : egutegiaService.kalkulatuKlaseak(egutegia);
        List<List<Eguna>> asteak = prestatuAsteak(hilabeteaData, jarduerakEgunka,
                asistentziaModuluak, egunKlaseak);

        String hilabeteEtiketa = hilabeteaData.atDay(1)
                .format(DateTimeFormatter.ofPattern("LLLL yyyy", EU_LOCALE));
        return new DenboralizazioaIkuspegia(programazioa.ikasturtea(), hilabeteEtiketa,
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

    public record EgunekoJarduera(Long id, Long erronkaId, String erronka, String izenburua,
            String deskribapena, String badgea, boolean orokorra,
            LocalDate hasieraData, LocalDate bukaeraData) {}
    public record AsistentziaAukera(Long moduloaId, String etiketa, boolean irekita) {}
    public record Eguna(LocalDate data, boolean hilabeteAktibokoa, String klasea,
            List<EgunekoJarduera> jarduerak, List<AsistentziaAukera> asistentziak) {}
    public record DenboralizazioaIkuspegia(String ikasturtea, String hilabeteEtiketa,
            int urtea, int hilabetea, Long aukeratutakoModuloaId,
            List<EthaziKoadernoaService.ModuluAukera> moduluAukerak,
            List<List<Eguna>> asteak) {}
}
