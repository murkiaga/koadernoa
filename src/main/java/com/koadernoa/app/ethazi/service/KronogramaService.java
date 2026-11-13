package com.koadernoa.app.ethazi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.ethazi.dto.KronogramaJardueraForm;
import com.koadernoa.app.ethazi.dto.JardueraMultzoForm;
import com.koadernoa.app.ethazi.entitateak.Erronka;
import com.koadernoa.app.ethazi.entitateak.kronograma.ErronkaJardueraMultzoa;
import com.koadernoa.app.ethazi.entitateak.kronograma.ErronkaKronogramaJarduera;
import com.koadernoa.app.ethazi.entitateak.kronograma.KronogramaJardueraMota;
import com.koadernoa.app.ethazi.repository.ErronkaJardueraMultzoaRepository;
import com.koadernoa.app.ethazi.repository.ErronkaKronogramaJardueraRepository;
import com.koadernoa.app.ethazi.repository.ErronkaRepository;
import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunMota;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.repository.EgutegiaRepository;
import com.koadernoa.app.objektuak.irakasleak.entitateak.IrakasleUserDetails;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.TaldeOrdutegia;
import com.koadernoa.app.objektuak.ordutegiak.repository.TaldeOrdutegiaRepository;
import com.koadernoa.app.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KronogramaService {
    private final ErronkaRepository erronkak;
    private final ErronkaKronogramaJardueraRepository jarduerak;
    private final ErronkaJardueraMultzoaRepository multzoak;
    private final ModuloaRepository moduloakRepository;
    private final IrakasleaRepository irakasleak;
    private final EgutegiaRepository egutegiak;
    private final TaldeOrdutegiaRepository taldeOrdutegiak;

    public KronogramaIkuspegia ikuspegia(Long erronkaId, Authentication auth) {
        Erronka erronka = erronka(erronkaId);
        List<Moduloa> moduluak = ordenatutakoModuluak(erronka);
        Set<Long> editaDaitezkeenModuloak = editaDaitezkeenModuloak(auth, erronka, moduluak);
        boolean koordinatzailea = koordinatzaileaDa(auth);
        if (!koordinatzailea && editaDaitezkeenModuloak.isEmpty()) {
            throw new AccessDeniedException("Ez duzu erronka honen kronograma ikusteko baimenik.");
        }
        List<ErronkaKronogramaJarduera> erronkakoJarduerak =
            jarduerak.findByErronkaIdOrderByHasieraDataAscBukaeraDataAscIdAsc(erronkaId);
        KronogramaOrdutegiTestuingurua ordutegiTestuingurua = prestatuOrdutegiTestuingurua(erronka, moduluak);
        Map<ErronkaKronogramaJarduera, Map<LocalDate, BigDecimal>> banaketak =
                banatuJardueraGuztiak(erronkakoJarduerak, ordutegiTestuingurua, erronka, false);
        List<KronogramaAstea> asteak = sortuAsteak(
            erronka, moduluak, erronkakoJarduerak, koordinatzailea, editaDaitezkeenModuloak,
            ordutegiTestuingurua, banaketak);
        long egunKopurua = java.time.temporal.ChronoUnit.DAYS.between(
            erronka.getHasieraData(), erronka.getBukaeraData()) + 1;
        return new KronogramaIkuspegia(erronka, moduluak, erronkakoJarduerak, asteak,
            koordinatzailea, editaDaitezkeenModuloak, egunKopurua);
    }

    @Transactional
    public Long gorde(Long erronkaId, KronogramaJardueraForm form, Authentication auth) {
        return gorde(erronkaId, form, auth, false);
    }

    @Transactional
    public Long gordeProgramaziotik(Long erronkaId, KronogramaJardueraForm form, Authentication auth) {
        return gorde(erronkaId, form, auth, true);
    }

    private Long gorde(Long erronkaId, KronogramaJardueraForm form, Authentication auth,
            boolean kokapenAutomatikoa) {
        Erronka erronka = erronka(erronkaId);
        balioztatuOinarrizkoa(erronka, form, kokapenAutomatikoa);
        ErronkaKronogramaJarduera jarduera;
        if (form.getId() == null) {
            jarduera = new ErronkaKronogramaJarduera();
            jarduera.setErronka(erronka);
        } else {
            jarduera = jarduerak.findById(form.getId())
                .orElseThrow(() -> new IllegalArgumentException("Jarduera ez da aurkitu."));
            require(jarduera.getErronka().getId().equals(erronkaId),
                "Jarduera ez da erronka honetakoa.");
            egiaztatuEditatzekoBaimena(auth, erronka, jarduera.getMota(),
                jarduera.getModuloa() == null ? null : jarduera.getModuloa().getId());
        }

        Moduloa moduloa = null;
        Moduloa aurreikusitakoModuloa = null;
        Long aurrekoModuloaId = jarduera.getModuloa() == null ? null : jarduera.getModuloa().getId();
        Long aurrekoMultzoaId = jarduera.getMultzoa() == null ? null : jarduera.getMultzoa().getId();
        if (form.getMota() == KronogramaJardueraMota.MODULUA) {
            moduloa = parteHartzenDuenModuloa(erronka, form.getModuloaId());
            egiaztatuEditatzekoBaimena(auth, erronka, form.getMota(), moduloa.getId());
        } else {
            egiaztatuEditatzekoBaimena(auth, erronka, form.getMota(), null);
            if (form.getAurreikusitakoModuloaId() != null) {
                aurreikusitakoModuloa = parteHartzenDuenModuloa(erronka, form.getAurreikusitakoModuloaId());
            }
        }

        BigDecimal iraupena = form.getIraupenOrduak().setScale(2, RoundingMode.HALF_UP);
        ErronkaJardueraMultzoa multzoa = form.getMultzoaId() == null ? null
                : erronkakoMultzoa(erronkaId, form.getMultzoaId());
        jarduera.setIzenburua(form.getIzenburua().strip());
        jarduera.setDeskribapena(hutsikNull(form.getDeskribapena()));
        LocalDate hasiera = kokapenAutomatikoa ? erronka.getHasieraData() : form.getHasieraData();
        jarduera.setHasieraData(hasiera);
        jarduera.setBukaeraData(hasiera);
        jarduera.setIraupenOrduak(iraupena);
        jarduera.setMota(form.getMota());
        jarduera.setModuloa(moduloa);
        jarduera.setAurreikusitakoModuloa(aurreikusitakoModuloa);
        jarduera.setMultzoa(multzoa);

        List<ErronkaKronogramaJarduera> guztiak = new ArrayList<>(
                jarduerak.findByErronkaIdOrderByHasieraDataAscBukaeraDataAscIdAsc(erronkaId));
        if (jarduera.getId() == null) {
            jarduera.setOrdena(hurrengoOrdena(guztiak, moduloa));
            guztiak.add(jarduera);
        } else {
            guztiak.removeIf(j -> jarduera.getId().equals(j.getId()));
            if (form.getMota() == KronogramaJardueraMota.MODULUA
                    && (!java.util.Objects.equals(aurrekoModuloaId, moduloa.getId())
                        || !java.util.Objects.equals(aurrekoMultzoaId,
                                multzoa == null ? null : multzoa.getId()))) {
                jarduera.setOrdena(hurrengoOrdena(guztiak, moduloa));
            }
            guztiak.add(jarduera);
        }
        if (kokapenAutomatikoa) {
            berrordenatuEtaDatakBirkalkulatu(erronka, guztiak);
        } else {
            KronogramaOrdutegiTestuingurua testuingurua = prestatuOrdutegiTestuingurua(
                    erronka, ordenatutakoModuluak(erronka));
            Map<ErronkaKronogramaJarduera, Map<LocalDate, BigDecimal>> banaketak =
                    banatuJardueraGuztiak(guztiak, testuingurua, erronka, true);
            eguneratuDatak(banaketak);
        }
        ErronkaKronogramaJarduera gordea = jarduerak.save(jarduera);
        jarduerak.saveAll(guztiak);
        return gordea.getId();
    }

    @Transactional
    public void berrordenatu(Long erronkaId, Long moduloaId, List<Long> jardueraIds, Authentication auth) {
        Erronka erronka = erronka(erronkaId);
        Moduloa moduloa = parteHartzenDuenModuloa(erronka, moduloaId);
        egiaztatuEditatzekoBaimena(auth, erronka, KronogramaJardueraMota.MODULUA, moduloaId);
        require(jardueraIds != null && !jardueraIds.isEmpty(), "Jardueren ordena falta da.");
        require(jardueraIds.stream().distinct().count() == jardueraIds.size(),
                "Jarduera bera behin baino gehiagotan bidali da.");

        List<ErronkaKronogramaJarduera> guztiak = new ArrayList<>(
                jarduerak.findByErronkaIdOrderByHasieraDataAscBukaeraDataAscIdAsc(erronkaId));
        List<ErronkaKronogramaJarduera> modulukoak = guztiak.stream()
                .filter(j -> j.getMota() == KronogramaJardueraMota.MODULUA && j.getModuloa() != null
                        && moduloaId.equals(j.getModuloa().getId()))
                .toList();
        Set<Long> benetakoIds = modulukoak.stream().map(ErronkaKronogramaJarduera::getId)
                .collect(java.util.stream.Collectors.toSet());
        require(benetakoIds.equals(new LinkedHashSet<>(jardueraIds)),
                "Berrantolatzeko modulu horretako jarduera guztiak bidali behar dira.");

        Map<Long, ErronkaKronogramaJarduera> idz = modulukoak.stream().collect(
                java.util.stream.Collectors.toMap(ErronkaKronogramaJarduera::getId, j -> j));
        for (int i = 0; i < jardueraIds.size(); i++) {
            ErronkaKronogramaJarduera jarduera = idz.get(jardueraIds.get(i));
            jarduera.setOrdena(i + 1);
        }
        berrordenatuEtaDatakBirkalkulatu(erronka, guztiak);
        jarduerak.saveAll(guztiak);
    }

    @Transactional
    public void jardueraKokatu(Long erronkaId, Long moduloaId, Long jardueraId,
            Long multzoaId, List<Long> jardueraIds, List<Long> programazioJardueraIds,
            List<String> blokeak, Authentication auth) {
        Erronka erronka = erronka(erronkaId);
        parteHartzenDuenModuloa(erronka, moduloaId);
        egiaztatuEditatzekoBaimena(auth, erronka, KronogramaJardueraMota.MODULUA, moduloaId);
        require(jardueraId != null, "Mugitutako jarduera falta da.");
        require(jardueraIds != null && !jardueraIds.isEmpty(), "Jardueren ordena falta da.");
        require(jardueraIds.stream().distinct().count() == jardueraIds.size(),
                "Jarduera bera behin baino gehiagotan bidali da.");

        List<ErronkaKronogramaJarduera> guztiak = new ArrayList<>(
                jarduerak.findByErronkaIdOrderByHasieraDataAscBukaeraDataAscIdAsc(erronkaId));
        List<ErronkaKronogramaJarduera> modulukoak = guztiak.stream()
                .filter(j -> j.getMota() == KronogramaJardueraMota.MODULUA && j.getModuloa() != null
                        && moduloaId.equals(j.getModuloa().getId()))
                .toList();
        Map<Long, ErronkaKronogramaJarduera> idz = modulukoak.stream().collect(
                java.util.stream.Collectors.toMap(ErronkaKronogramaJarduera::getId, j -> j));
        require(idz.keySet().equals(new LinkedHashSet<>(jardueraIds)),
                "Berrantolatzeko modulu horretako jarduera guztiak bidali behar dira.");
        ErronkaKronogramaJarduera mugitutakoa = idz.get(jardueraId);
        require(mugitutakoa != null, "Mugitutako jarduera ez da modulu honetakoa.");
        mugitutakoa.setMultzoa(multzoaId == null ? null : erronkakoMultzoa(erronkaId, multzoaId));
        List<ErronkaKronogramaJarduera> programaziokoak = guztiak.stream()
                .filter(j -> j.getMota() == KronogramaJardueraMota.MODULUA && j.getModuloa() != null
                            && moduloaId.equals(j.getModuloa().getId())
                        || j.getMota() == KronogramaJardueraMota.KOMUNA
                            && j.getAurreikusitakoModuloa() != null
                            && moduloaId.equals(j.getAurreikusitakoModuloa().getId()))
                .toList();
        Set<Long> programaziokoIds = programaziokoak.stream().map(ErronkaKronogramaJarduera::getId)
                .collect(java.util.stream.Collectors.toSet());
        require(programazioJardueraIds != null
                        && programaziokoIds.equals(new LinkedHashSet<>(programazioJardueraIds))
                        && programazioJardueraIds.stream().distinct().count() == programazioJardueraIds.size(),
                "Berrantolatzeko moduluko jarduera eta aingura orokor guztiak bidali behar dira.");
        Map<Long, ErronkaKronogramaJarduera> programazioIdz = programaziokoak.stream().collect(
                java.util.stream.Collectors.toMap(ErronkaKronogramaJarduera::getId, j -> j));
        for (int i = 0; i < programazioJardueraIds.size(); i++) {
            programazioIdz.get(programazioJardueraIds.get(i)).setOrdena(i + 1);
        }
        eguneratuProgramazioBlokeenOrdena(erronkaId, moduloaId, blokeak, guztiak);

        berrordenatuEtaDatakBirkalkulatu(erronka, guztiak);
        jarduerak.saveAll(guztiak);
    }

    private int hurrengoOrdena(List<ErronkaKronogramaJarduera> guztiak, Moduloa moduloa) {
        if (moduloa == null) return 0;
        return guztiak.stream().filter(j -> j.getModuloa() != null
                        && moduloa.getId().equals(j.getModuloa().getId()))
                .map(ErronkaKronogramaJarduera::getOrdena).filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue).max().orElse(0) + 1;
    }

    private void berrordenatuEtaDatakBirkalkulatu(Erronka erronka,
            List<ErronkaKronogramaJarduera> guztiak) {
        normalizatuProgramazioOrdena(guztiak);
        guztiak.stream().filter(j -> j.getMota() == KronogramaJardueraMota.MODULUA
                && jarduerarenOrdutegikoModuloa(j) != null).forEach(j -> {
            j.setHasieraData(erronka.getHasieraData());
            j.setBukaeraData(erronka.getHasieraData());
        });
        KronogramaOrdutegiTestuingurua testuingurua = prestatuOrdutegiTestuingurua(
                erronka, ordenatutakoModuluak(erronka));
        Map<ErronkaKronogramaJarduera, Map<LocalDate, BigDecimal>> banaketak =
                banatuJardueraGuztiak(guztiak, testuingurua, erronka, true, true);
        eguneratuDatak(banaketak);
    }

    private void normalizatuProgramazioOrdena(List<ErronkaKronogramaJarduera> guztiak) {
        guztiak.stream()
                .filter(j -> jarduerarenOrdutegikoModuloa(j) != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        j -> jarduerarenOrdutegikoModuloa(j).getId()))
                .values().forEach(modulukoak -> {
                    List<ErronkaKronogramaJarduera> ordenatuta = modulukoak.stream()
                            .sorted(programazioOrdena()).toList();
                    for (int i = 0; i < ordenatuta.size(); i++) ordenatuta.get(i).setOrdena(i + 1);
                });
    }

    private void eguneratuDatak(
            Map<ErronkaKronogramaJarduera, Map<LocalDate, BigDecimal>> banaketak) {
        banaketak.forEach((j, banaketa) -> {
            if (banaketa.isEmpty()) return;
            j.setHasieraData(banaketa.keySet().stream().min(LocalDate::compareTo).orElse(j.getHasieraData()));
            j.setBukaeraData(banaketa.keySet().stream().max(LocalDate::compareTo).orElse(j.getHasieraData()));
        });
    }

    private Comparator<ErronkaKronogramaJarduera> programazioOrdena() {
        return Comparator
                .comparingInt((ErronkaKronogramaJarduera j) -> {
                    Integer ordena = j.getMultzoa() == null ? j.getOrdena() : j.getMultzoa().getOrdena();
                    return ordena != null && ordena > 0 ? ordena : Integer.MAX_VALUE;
                })
                .thenComparingInt(j -> j.getOrdena() != null && j.getOrdena() > 0
                        ? j.getOrdena() : Integer.MAX_VALUE)
                .thenComparingInt(j -> j.getMota() == KronogramaJardueraMota.KOMUNA ? 0 : 1)
                .thenComparing(ErronkaKronogramaJarduera::getId,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    @Transactional
    public void ezabatu(Long erronkaId, Long jardueraId, Authentication auth) {
        Erronka erronka = erronka(erronkaId);
        ErronkaKronogramaJarduera jarduera = jarduerak.findById(jardueraId)
            .orElseThrow(() -> new IllegalArgumentException("Jarduera ez da aurkitu."));
        require(jarduera.getErronka().getId().equals(erronkaId), "Jarduera ez da erronka honetakoa.");
        egiaztatuEditatzekoBaimena(auth, erronka, jarduera.getMota(),
            jarduera.getModuloa() == null ? null : jarduera.getModuloa().getId());
        jarduerak.delete(jarduera);
    }

    @Transactional
    public Long multzoaGorde(Long erronkaId, JardueraMultzoForm form, Authentication auth) {
        Erronka erronka = erronka(erronkaId);
        egiaztatuMultzoaKudeatzekoBaimena(auth, erronka);
        require(form != null && form.getIzena() != null && !form.getIzena().isBlank(),
                "Multzoaren izena beharrezkoa da.");
        String izena = form.getIzena().strip();
        require(izena.length() <= 200, "Multzoaren izenak gehienez 200 karaktere izan ditzake.");

        ErronkaJardueraMultzoa multzoa;
        if (form.getId() == null) {
            require(!multzoak.existsByErronkaIdAndIzenaIgnoreCase(erronkaId, izena),
                    "Erronka honetan izen bereko multzo bat dago jada.");
            multzoa = new ErronkaJardueraMultzoa();
            multzoa.setErronka(erronka);
            int multzoenAzkenOrdena = multzoak.findByErronkaIdOrderByOrdenaAscIdAsc(erronkaId).stream()
                    .map(ErronkaJardueraMultzoa::getOrdena).filter(java.util.Objects::nonNull)
                    .mapToInt(Integer::intValue).max().orElse(0);
            int jarduerenAzkenOrdena = jarduerak
                    .findByErronkaIdOrderByHasieraDataAscBukaeraDataAscIdAsc(erronkaId).stream()
                    .filter(j -> j.getMultzoa() == null)
                    .map(ErronkaKronogramaJarduera::getOrdena).filter(java.util.Objects::nonNull)
                    .mapToInt(Integer::intValue).max().orElse(0);
            multzoa.setOrdena(Math.max(multzoenAzkenOrdena, jarduerenAzkenOrdena) + 1);
        } else {
            multzoa = erronkakoMultzoa(erronkaId, form.getId());
            require(!multzoak.existsByErronkaIdAndIzenaIgnoreCaseAndIdNot(erronkaId, izena, form.getId()),
                    "Erronka honetan izen bereko multzo bat dago jada.");
        }
        multzoa.setIzena(izena);
        return multzoak.save(multzoa).getId();
    }

    @Transactional
    public void multzoakBerrordenatu(Long erronkaId, List<Long> multzoaIds, Authentication auth) {
        Erronka erronka = erronka(erronkaId);
        egiaztatuMultzoaKudeatzekoBaimena(auth, erronka);
        require(multzoaIds != null && !multzoaIds.isEmpty(), "Multzoen ordena falta da.");
        require(multzoaIds.stream().distinct().count() == multzoaIds.size(),
                "Multzo bera behin baino gehiagotan bidali da.");
        List<ErronkaJardueraMultzoa> erronkakoMultzoak =
                multzoak.findByErronkaIdOrderByOrdenaAscIdAsc(erronkaId);
        Set<Long> benetakoIds = erronkakoMultzoak.stream().map(ErronkaJardueraMultzoa::getId)
                .collect(java.util.stream.Collectors.toSet());
        require(benetakoIds.equals(new LinkedHashSet<>(multzoaIds)),
                "Berrantolatzeko erronkako multzo guztiak bidali behar dira.");
        Map<Long, ErronkaJardueraMultzoa> idz = erronkakoMultzoak.stream().collect(
                java.util.stream.Collectors.toMap(ErronkaJardueraMultzoa::getId, m -> m));
        for (int i = 0; i < multzoaIds.size(); i++) idz.get(multzoaIds.get(i)).setOrdena(i + 1);
        multzoak.saveAll(erronkakoMultzoak);

        List<ErronkaKronogramaJarduera> guztiak = new ArrayList<>(
                jarduerak.findByErronkaIdOrderByHasieraDataAscBukaeraDataAscIdAsc(erronkaId));
        berrordenatuEtaDatakBirkalkulatu(erronka, guztiak);
        jarduerak.saveAll(guztiak);
    }

    @Transactional
    public void programazioBlokeakBerrordenatu(Long erronkaId, Long moduloaId,
            List<String> blokeak, Authentication auth) {
        Erronka erronka = erronka(erronkaId);
        parteHartzenDuenModuloa(erronka, moduloaId);
        egiaztatuEditatzekoBaimena(auth, erronka, KronogramaJardueraMota.MODULUA, moduloaId);
        List<ErronkaKronogramaJarduera> guztiak = new ArrayList<>(
                jarduerak.findByErronkaIdOrderByHasieraDataAscBukaeraDataAscIdAsc(erronkaId));
        eguneratuProgramazioBlokeenOrdena(erronkaId, moduloaId, blokeak, guztiak);
        berrordenatuEtaDatakBirkalkulatu(erronka, guztiak);
        jarduerak.saveAll(guztiak);
    }

    private void eguneratuProgramazioBlokeenOrdena(Long erronkaId, Long moduloaId,
            List<String> blokeak, List<ErronkaKronogramaJarduera> guztiak) {
        require(blokeak != null && !blokeak.isEmpty(), "Programazioaren ordena falta da.");
        require(blokeak.stream().allMatch(java.util.Objects::nonNull)
                        && blokeak.stream().distinct().count() == blokeak.size(),
                "Programazio-bloke bera behin baino gehiagotan bidali da.");

        List<ErronkaJardueraMultzoa> erronkakoMultzoak =
                multzoak.findByErronkaIdOrderByOrdenaAscIdAsc(erronkaId);
        List<ErronkaKronogramaJarduera> jardueraSolteIkusgaiak = guztiak.stream()
                .filter(j -> j.getMultzoa() == null)
                .filter(j -> j.getMota() == KronogramaJardueraMota.KOMUNA
                        || j.getModuloa() != null && moduloaId.equals(j.getModuloa().getId()))
                .toList();

        Set<String> benetakoBlokeak = new LinkedHashSet<>();
        erronkakoMultzoak.forEach(m -> benetakoBlokeak.add("MULTZOA:" + m.getId()));
        jardueraSolteIkusgaiak.forEach(j -> benetakoBlokeak.add("JARDUERA:" + j.getId()));
        require(benetakoBlokeak.equals(new LinkedHashSet<>(blokeak)),
                "Berrantolatzeko multzo eta jarduera solte ikusgai guztiak bidali behar dira.");

        Map<String, ErronkaJardueraMultzoa> multzoMap = erronkakoMultzoak.stream().collect(
                java.util.stream.Collectors.toMap(m -> "MULTZOA:" + m.getId(), m -> m));
        Map<String, ErronkaKronogramaJarduera> jardueraMap = jardueraSolteIkusgaiak.stream().collect(
                java.util.stream.Collectors.toMap(j -> "JARDUERA:" + j.getId(), j -> j));
        for (int i = 0; i < blokeak.size(); i++) {
            String blokea = blokeak.get(i);
            int ordena = i + 1;
            if (multzoMap.containsKey(blokea)) multzoMap.get(blokea).setOrdena(ordena);
            else jardueraMap.get(blokea).setOrdena(ordena);
        }
        multzoak.saveAll(erronkakoMultzoak);
    }

    @Transactional
    public void multzoaEzabatu(Long erronkaId, Long multzoaId, Authentication auth) {
        Erronka erronka = erronka(erronkaId);
        egiaztatuMultzoaKudeatzekoBaimena(auth, erronka);
        ErronkaJardueraMultzoa multzoa = erronkakoMultzoa(erronkaId, multzoaId);
        List<ErronkaKronogramaJarduera> multzokoJarduerak = jarduerak.findByMultzoaId(multzoaId);
        multzokoJarduerak.forEach(j -> j.setMultzoa(null));
        multzoak.delete(multzoa);
        List<ErronkaKronogramaJarduera> guztiak = new ArrayList<>(
                jarduerak.findByErronkaIdOrderByHasieraDataAscBukaeraDataAscIdAsc(erronkaId));
        multzokoJarduerak.forEach(j -> {
            if (guztiak.stream().noneMatch(g -> g.getId().equals(j.getId()))) guztiak.add(j);
        });
        berrordenatuEtaDatakBirkalkulatu(erronka, guztiak);
        jarduerak.saveAll(guztiak);
    }

    private void balioztatuOinarrizkoa(Erronka erronka, KronogramaJardueraForm form,
            boolean kokapenAutomatikoa) {
        require(form != null, "Jardueraren datuak falta dira.");
        require(form.getIzenburua() != null && !form.getIzenburua().isBlank(), "Izenburua beharrezkoa da.");
        require(form.getIzenburua().strip().length() <= 200, "Izenburuak gehienez 200 karaktere izan ditzake.");
        require(form.getMota() != null, "Jarduera mota aukeratu behar da.");
        if (!kokapenAutomatikoa) {
            require(form.getHasieraData() != null, "Hasiera data beharrezkoa da.");
            require(!form.getHasieraData().isBefore(erronka.getHasieraData())
                    && !form.getHasieraData().isAfter(erronka.getBukaeraData()),
                "Jardueraren hasiera erronkaren daten barruan egon behar da.");
        }
        require(form.getIraupenOrduak() != null && form.getIraupenOrduak().compareTo(BigDecimal.ZERO) > 0,
            "Iraupena zero baino handiagoa izan behar da.");
        require(form.getIraupenOrduak().compareTo(new BigDecimal("999.99")) <= 0,
            "Iraupena handiegia da.");
        require(form.getIraupenOrduak().stripTrailingZeros().scale() <= 2,
            "Iraupenak gehienez bi hamartar izan ditzake.");
    }

    private void egiaztatuEditatzekoBaimena(Authentication auth, Erronka erronka,
            KronogramaJardueraMota mota, Long moduloaId) {
        if (koordinatzaileaDa(auth)) return;
        if (mota == KronogramaJardueraMota.KOMUNA) {
            throw new AccessDeniedException("Koordinatzaileak bakarrik kudea ditzake jarduera komunak.");
        }
        if (moduloaId == null || !irakasleaModuluanDa(auth, moduloaId)
                || erronka.getIkasturtea() == null || !erronka.getIkasturtea().isAktiboa()) {
            throw new AccessDeniedException("Ez duzu modulu honetako jarduerak kudeatzeko baimenik.");
        }
    }

    private void egiaztatuMultzoaKudeatzekoBaimena(Authentication auth, Erronka erronka) {
        if (koordinatzaileaDa(auth)) return;
        if (editaDaitezkeenModuloak(auth, erronka, ordenatutakoModuluak(erronka)).isEmpty()) {
            throw new AccessDeniedException("Ez duzu erronka honetako jarduera-multzoak kudeatzeko baimenik.");
        }
    }

    private Set<Long> editaDaitezkeenModuloak(Authentication auth, Erronka erronka, List<Moduloa> moduluak) {
        Set<Long> ids = new LinkedHashSet<>();
        if (koordinatzaileaDa(auth)) {
            moduluak.forEach(m -> ids.add(m.getId()));
        } else if (erronka.getIkasturtea() != null && erronka.getIkasturtea().isAktiboa()) {
            moduluak.stream().filter(m -> irakasleaModuluanDa(auth, m.getId())).forEach(m -> ids.add(m.getId()));
        }
        return ids;
    }

    private boolean irakasleaModuluanDa(Authentication auth, Long moduloaId) {
        if (!SecurityUtils.hasAnyRole(auth, "IRAKASLEA") || moduloaId == null) return false;
        return unekoIrakaslea(auth)
            .map(i -> moduloakRepository.existsByIdAndIrakasleak_Id(moduloaId, i.getId()))
            .orElse(false);
    }

    private java.util.Optional<Irakaslea> unekoIrakaslea(Authentication auth) {
        if (auth == null) return java.util.Optional.empty();
        if (auth.getPrincipal() instanceof IrakasleUserDetails details) {
            return java.util.Optional.ofNullable(details.getIrakaslea());
        }
        String ident = auth.getPrincipal() instanceof OAuth2User oauth ? oauth.getAttribute("email") : auth.getName();
        if (ident == null || ident.isBlank()) return java.util.Optional.empty();
        String garbia = ident.strip();
        return irakasleak.findByEmailaIgnoreCase(garbia).or(() -> irakasleak.findByIzenaIgnoreCase(garbia));
    }

    private boolean koordinatzaileaDa(Authentication auth) {
        return SecurityUtils.isKudeatzailea(auth);
    }

    private Erronka erronka(Long id) {
        return erronkak.findById(id).orElseThrow(() -> new IllegalArgumentException("Erronka ez da aurkitu."));
    }

    private ErronkaJardueraMultzoa erronkakoMultzoa(Long erronkaId, Long multzoaId) {
        ErronkaJardueraMultzoa multzoa = multzoak.findById(multzoaId)
                .orElseThrow(() -> new IllegalArgumentException("Jarduera-multzoa ez da aurkitu."));
        require(multzoa.getErronka() != null && erronkaId.equals(multzoa.getErronka().getId()),
                "Jarduera-multzoa ez da erronka honetakoa.");
        return multzoa;
    }

    private Moduloa parteHartzenDuenModuloa(Erronka erronka, Long moduloaId) {
        require(moduloaId != null, "Modulua aukeratu behar da.");
        return erronka.getModuluak().stream().filter(m -> moduloaId.equals(m.getId())).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Modulua ez da erronka honetakoa."));
    }

    private List<Moduloa> ordenatutakoModuluak(Erronka erronka) {
        return erronka.getModuluak().stream().sorted(Comparator
            .comparing((Moduloa m) -> m.getKodea() == null ? "" : m.getKodea(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(m -> m.getIzena() == null ? "" : m.getIzena(), String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private List<KronogramaAstea> sortuAsteak(Erronka erronka, List<Moduloa> moduluak,
            List<ErronkaKronogramaJarduera> guztiak, boolean koordinatzailea, Set<Long> editagarriak,
            KronogramaOrdutegiTestuingurua testuingurua,
            Map<ErronkaKronogramaJarduera, Map<LocalDate, BigDecimal>> banaketak) {
        List<KronogramaAstea> result = new ArrayList<>();
        LocalDate asteHasiera = erronka.getHasieraData().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int zenbakia = 1;
        while (!asteHasiera.isAfter(erronka.getBukaeraData())) {
            LocalDate lehenEguna = asteHasiera.isBefore(erronka.getHasieraData()) ? erronka.getHasieraData() : asteHasiera;
            LocalDate igandea = asteHasiera.plusDays(6);
            LocalDate azkenEguna = igandea.isAfter(erronka.getBukaeraData()) ? erronka.getBukaeraData() : igandea;
            List<LocalDate> egunak = lehenEguna.datesUntil(azkenEguna.plusDays(1))
                    .filter(data -> data.getDayOfWeek() != DayOfWeek.SATURDAY
                            && data.getDayOfWeek() != DayOfWeek.SUNDAY)
                    .toList();
            if (egunak.isEmpty()) {
                asteHasiera = asteHasiera.plusWeeks(1);
                continue;
            }
            lehenEguna = egunak.get(0);
            azkenEguna = egunak.get(egunak.size() - 1);
            List<KronogramaLerroa> lerroak = new ArrayList<>();
            lerroak.add(sortuLerroa(null, "Erronka", "Jarduera komunak", null, true, 0, egunak,
                guztiak.stream().filter(j -> j.getMota() == KronogramaJardueraMota.KOMUNA
                        && j.getAurreikusitakoModuloa() == null).toList(),
                koordinatzailea, koordinatzailea, testuingurua, banaketak));
            int color = 1;
            for (Moduloa moduloa : moduluak) {
                Long moduloaId = moduloa.getId();
                List<ErronkaKronogramaJarduera> modulukoak = guztiak.stream()
                    .filter(j -> (j.getMota() == KronogramaJardueraMota.MODULUA && j.getModuloa() != null
                            && moduloaId.equals(j.getModuloa().getId())
                        || (j.getMota() == KronogramaJardueraMota.KOMUNA && j.getAurreikusitakoModuloa() != null
                            && moduloaId.equals(j.getAurreikusitakoModuloa().getId()))))
                    .toList();
                String azpititulua = moduloa.getIrakasleak().isEmpty() ? "Irakaslerik esleitu gabe"
                    : moduloa.getIrakasleak().stream().map(Irakaslea::getIzena)
                        .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.joining(", "));
                lerroak.add(sortuLerroa(moduloaId,
                    moduloa.getKodea() == null || moduloa.getKodea().isBlank() ? moduloa.getIzena() : moduloa.getKodea(),
                    azpititulua, testuingurua.laburpenak().getOrDefault(moduloaId, new ModuloOrduLaburpena(0, 0)),
                    false, color++, egunak, modulukoak, editagarriak.contains(moduloaId), koordinatzailea,
                    testuingurua, banaketak));
            }
            result.add(new KronogramaAstea(zenbakia++, lehenEguna, azkenEguna, egunak, lerroak));
            asteHasiera = asteHasiera.plusWeeks(1);
        }
        return result;
    }

    private KronogramaLerroa sortuLerroa(Long moduloaId, String etiketa, String azpititulua,
            ModuloOrduLaburpena orduLaburpena, boolean komuna, int koloreIndizea, List<LocalDate> egunak,
            List<ErronkaKronogramaJarduera> lerrokoJarduerak, boolean editagarria, boolean koordinatzailea,
            KronogramaOrdutegiTestuingurua testuingurua,
            Map<ErronkaKronogramaJarduera, Map<LocalDate, BigDecimal>> banaketak) {
        List<KronogramaBlokea> blokeak = new ArrayList<>();
        List<LocalDate> lerroAmaierak = new ArrayList<>();
        LocalDate asteHasiera = egunak.get(0);
        LocalDate asteBukaera = egunak.get(egunak.size() - 1);
        for (ErronkaKronogramaJarduera jarduera : lerrokoJarduerak) {
            Map<LocalDate, BigDecimal> banaketa = banaketak.getOrDefault(jarduera, Map.of());
            boolean jardueraEditagarria = jarduera.getMota() == KronogramaJardueraMota.KOMUNA
                    ? koordinatzailea : editagarria;
            for (Map.Entry<LocalDate, BigDecimal> segmentua : banaketa.entrySet()) {
                LocalDate data = segmentua.getKey();
                if (data.isBefore(asteHasiera) || data.isAfter(asteBukaera)) continue;
                int start = (int) java.time.temporal.ChronoUnit.DAYS.between(asteHasiera, data) + 1;
                int lane = 0;
                while (lane < lerroAmaierak.size() && !lerroAmaierak.get(lane).isBefore(data)) lane++;
                if (lane == lerroAmaierak.size()) lerroAmaierak.add(data);
                else lerroAmaierak.set(lane, data);
                blokeak.add(new KronogramaBlokea(jarduera, start, 1, lane + 1, jardueraEditagarria,
                        segmentua.getValue()));
            }
        }
        Set<LocalDate> klaseEgunak = komuna ? new LinkedHashSet<>(egunak)
                : egunak.stream().filter(testuingurua.egunekoOrduak()
                        .getOrDefault(moduloaId, Map.of())::containsKey)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new KronogramaLerroa(moduloaId, etiketa, azpititulua, orduLaburpena, komuna,
            Math.floorMod(koloreIndizea, 6), Math.max(1, lerroAmaierak.size()), editagarria,
            klaseEgunak, blokeak);
    }

    private KronogramaOrdutegiTestuingurua prestatuOrdutegiTestuingurua(Erronka erronka,
            List<Moduloa> moduluak) {
        Map<Long, ModuloOrduLaburpena> laburpenak = new LinkedHashMap<>();
        Map<Long, Map<LocalDate, Integer>> egunekoOrduak = new LinkedHashMap<>();
        moduluak.forEach(m -> {
            laburpenak.put(m.getId(), new ModuloOrduLaburpena(0, 0));
            egunekoOrduak.put(m.getId(), new LinkedHashMap<>());
        });
        if (erronka.getIkasturtea() == null) {
            return new KronogramaOrdutegiTestuingurua(laburpenak, egunekoOrduak);
        }

        Map<Long, NavigableMap<LocalDate, TaldeOrdutegia>> taldekoBertsioak = new LinkedHashMap<>();
        moduluak.stream().map(Moduloa::getTaldea).filter(java.util.Objects::nonNull)
            .map(t -> t.getId()).filter(java.util.Objects::nonNull).distinct().forEach(taldeaId -> {
                NavigableMap<LocalDate, TaldeOrdutegia> bertsioak = new TreeMap<>();
                taldeOrdutegiak.findByTaldea_IdAndIkasturtea_IdOrderByHasieraDataAsc(
                        taldeaId, erronka.getIkasturtea().getId()).stream()
                    .filter(o -> o.getHasieraData() != null)
                    .forEach(o -> bertsioak.put(o.getHasieraData(), o));
                taldekoBertsioak.put(taldeaId, bertsioak);
            });

        Egutegia egutegia = erronka.getMaila() == null ? null : egutegiak.findByIkasturtea_IdAndMaila_Id(
                erronka.getIkasturtea().getId(), erronka.getMaila().getId()).orElse(null);
        Map<LocalDate, EgunBerezi> egunBereziak = egutegia == null ? Map.of()
                : Optional.ofNullable(egutegia.getEgunBereziak()).orElse(List.of()).stream()
                    .filter(e -> e != null && e.getData() != null)
                    .collect(java.util.stream.Collectors.toMap(EgunBerezi::getData, e -> e, (a, b) -> a));

        for (Moduloa moduloa : moduluak) {
            if (moduloa.getTaldea() == null || moduloa.getTaldea().getId() == null) continue;
            NavigableMap<LocalDate, TaldeOrdutegia> bertsioak = taldekoBertsioak
                    .getOrDefault(moduloa.getTaldea().getId(), new TreeMap<>());
            Map<LocalDate, Integer> modulukoEgunak = egunekoOrduak.get(moduloa.getId());
            for (LocalDate data = erronka.getHasieraData(); !data.isAfter(erronka.getBukaeraData()); data = data.plusDays(1)) {
                if (!egutegiBarruan(data, egutegia)) continue;
                Astegunak asteguna = astegunEraginkorra(data, egunBereziak);
                Map.Entry<LocalDate, TaldeOrdutegia> bertsioa = bertsioak.floorEntry(data);
                if (asteguna == null || bertsioa == null) continue;
                int orduak = (int) Optional.ofNullable(bertsioa.getValue().getLerroak()).orElse(List.of()).stream()
                    .filter(l -> asteguna == l.getAsteguna() && l.getModuloa() != null
                            && moduloa.getId().equals(l.getModuloa().getId()))
                    .count();
                if (orduak > 0) modulukoEgunak.put(data, orduak);
            }
            TaldeOrdutegia hasierakoOrdutegia = Optional.ofNullable(bertsioak.floorEntry(erronka.getHasieraData()))
                    .map(Map.Entry::getValue).orElse(null);
            int astekoOrduak = hasierakoOrdutegia == null ? 0
                    : (int) Optional.ofNullable(hasierakoOrdutegia.getLerroak()).orElse(List.of()).stream()
                        .filter(l -> l.getModuloa() != null && moduloa.getId().equals(l.getModuloa().getId()))
                        .count();
            int guztira = modulukoEgunak.values().stream().mapToInt(Integer::intValue).sum();
            laburpenak.put(moduloa.getId(), new ModuloOrduLaburpena(astekoOrduak, guztira));
        }
        return new KronogramaOrdutegiTestuingurua(laburpenak, egunekoOrduak);
    }

    private boolean egutegiBarruan(LocalDate data, Egutegia egutegia) {
        return egutegia == null
                || (egutegia.getHasieraData() == null || !data.isBefore(egutegia.getHasieraData()))
                && (egutegia.getBukaeraData() == null || !data.isAfter(egutegia.getBukaeraData()));
    }

    private Map<ErronkaKronogramaJarduera, Map<LocalDate, BigDecimal>> banatuJardueraGuztiak(
            List<ErronkaKronogramaJarduera> jardueraGuztiak, KronogramaOrdutegiTestuingurua testuingurua,
            Erronka erronka, boolean zorrotza) {
        return banatuJardueraGuztiak(jardueraGuztiak, testuingurua, erronka, zorrotza, false);
    }

    private Map<ErronkaKronogramaJarduera, Map<LocalDate, BigDecimal>> banatuJardueraGuztiak(
            List<ErronkaKronogramaJarduera> jardueraGuztiak, KronogramaOrdutegiTestuingurua testuingurua,
            Erronka erronka, boolean zorrotza, boolean programazioAutomatikoa) {
        Map<ErronkaKronogramaJarduera, Map<LocalDate, BigDecimal>> emaitza = new LinkedHashMap<>();
        Map<Long, List<ErronkaKronogramaJarduera>> moduluka = new LinkedHashMap<>();
        for (ErronkaKronogramaJarduera jarduera : jardueraGuztiak) {
            Moduloa moduloa = jarduerarenOrdutegikoModuloa(jarduera);
            if (moduloa == null) {
                emaitza.put(jarduera, Map.of(jarduera.getHasieraData(), jarduera.getIraupenOrduak()));
            } else {
                moduluka.computeIfAbsent(moduloa.getId(), __ -> new ArrayList<>()).add(jarduera);
            }
        }
        Comparator<ErronkaKronogramaJarduera> ordena = Comparator
                .comparingInt((ErronkaKronogramaJarduera j) ->
                        j.getMota() == KronogramaJardueraMota.KOMUNA ? 0 : 1)
                .thenComparingInt(j -> j.getMota() == KronogramaJardueraMota.MODULUA
                        && j.getOrdena() != null && j.getOrdena() > 0 ? j.getOrdena() : Integer.MAX_VALUE)
                .thenComparing(ErronkaKronogramaJarduera::getHasieraData)
                .thenComparing(ErronkaKronogramaJarduera::getId,
                        Comparator.nullsLast(Comparator.naturalOrder()));
        moduluka.forEach((moduloaId, modulukoJarduerak) -> {
            Map<LocalDate, Integer> gaitasuna = testuingurua.egunekoOrduak()
                    .getOrDefault(moduloaId, Map.of());
            Map<LocalDate, BigDecimal> erabilgarri = new LinkedHashMap<>();
            gaitasuna.forEach((data, orduak) -> erabilgarri.put(data, BigDecimal.valueOf(orduak)));
            if (programazioAutomatikoa) {
                banatuProgramazioAutomatikoa(modulukoJarduerak, gaitasuna, erabilgarri,
                        erronka, zorrotza, emaitza);
                return;
            }
            modulukoJarduerak.stream().sorted(ordena).forEach(jarduera -> {
                Map<LocalDate, BigDecimal> banaketa = banatuJarduera(
                        jarduera.getHasieraData(),
                        jarduera.getIraupenOrduak(), gaitasuna, erabilgarri, erronka,
                        zorrotza, zorrotza);
                emaitza.put(jarduera, banaketa);
                banaketa.forEach((data, orduak) -> erabilgarri.computeIfPresent(data,
                        (__, libre) -> libre.subtract(orduak).max(BigDecimal.ZERO)));
            });
        });
        return emaitza;
    }

    private void banatuProgramazioAutomatikoa(List<ErronkaKronogramaJarduera> modulukoJarduerak,
            Map<LocalDate, Integer> gaitasuna, Map<LocalDate, BigDecimal> erabilgarri,
            Erronka erronka, boolean zorrotza,
            Map<ErronkaKronogramaJarduera, Map<LocalDate, BigDecimal>> emaitza) {
        List<ErronkaKronogramaJarduera> ordenatuta = modulukoJarduerak.stream()
                .sorted(programazioOrdena()).toList();
        List<ErronkaKronogramaJarduera> aingurak = ordenatuta.stream()
                .filter(j -> j.getMota() == KronogramaJardueraMota.KOMUNA)
                .sorted(Comparator.comparing(ErronkaKronogramaJarduera::getHasieraData)
                        .thenComparing(ErronkaKronogramaJarduera::getId))
                .toList();

        for (ErronkaKronogramaJarduera aingura : aingurak) {
            Map<LocalDate, BigDecimal> banaketa = banatuJarduera(aingura.getHasieraData(),
                    aingura.getIraupenOrduak(), gaitasuna, erabilgarri, erronka, false, zorrotza);
            emaitza.put(aingura, banaketa);
            kenduErabilgarritasuna(erabilgarri, banaketa);
        }

        for (int i = 0; i < ordenatuta.size(); i++) {
            ErronkaKronogramaJarduera jarduera = ordenatuta.get(i);
            if (jarduera.getMota() != KronogramaJardueraMota.MODULUA) continue;
            ErronkaKronogramaJarduera aurrekoAingura = null;
            ErronkaKronogramaJarduera hurrengoAingura = null;
            for (int j = i - 1; j >= 0; j--) {
                if (ordenatuta.get(j).getMota() == KronogramaJardueraMota.KOMUNA) {
                    aurrekoAingura = ordenatuta.get(j);
                    break;
                }
            }
            for (int j = i + 1; j < ordenatuta.size(); j++) {
                if (ordenatuta.get(j).getMota() == KronogramaJardueraMota.KOMUNA) {
                    hurrengoAingura = ordenatuta.get(j);
                    break;
                }
            }
            LocalDate hasiera = aurrekoAingura == null ? erronka.getHasieraData()
                    : emaitza.getOrDefault(aurrekoAingura, Map.of()).keySet().stream()
                            .max(LocalDate::compareTo).orElse(aurrekoAingura.getBukaeraData()).plusDays(1);
            LocalDate azkenData = hurrengoAingura == null ? erronka.getBukaeraData()
                    : hurrengoAingura.getHasieraData().minusDays(1);
            try {
                Map<LocalDate, BigDecimal> banaketa = banatuJarduera(hasiera,
                        jarduera.getIraupenOrduak(), gaitasuna, erabilgarri, erronka,
                        false, zorrotza, azkenData);
                emaitza.put(jarduera, banaketa);
                kenduErabilgarritasuna(erabilgarri, banaketa);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Ezin da '" + jarduera.getIzenburua()
                        + "' jarduera aukeratutako kokapenean jarri: ez dago nahikoa klase-ordurik "
                        + "aurreko eta hurrengo jarduera orokorren artean.");
            }
        }
    }

    private void kenduErabilgarritasuna(Map<LocalDate, BigDecimal> erabilgarri,
            Map<LocalDate, BigDecimal> banaketa) {
        banaketa.forEach((data, orduak) -> erabilgarri.computeIfPresent(data,
                (__, libre) -> libre.subtract(orduak).max(BigDecimal.ZERO)));
    }

    private Moduloa jarduerarenOrdutegikoModuloa(ErronkaKronogramaJarduera jarduera) {
        return jarduera.getModuloa() != null ? jarduera.getModuloa() : jarduera.getAurreikusitakoModuloa();
    }

    private Map<LocalDate, BigDecimal> banatuJarduera(LocalDate hasiera, BigDecimal iraupena,
            Map<LocalDate, Integer> gaitasuna, Map<LocalDate, BigDecimal> erabilgarri,
            Erronka erronka, boolean hasieraKlaseEgunaEskatu, boolean nahikoaEskatu) {
        return banatuJarduera(hasiera, iraupena, gaitasuna, erabilgarri, erronka,
                hasieraKlaseEgunaEskatu, nahikoaEskatu, erronka.getBukaeraData());
    }

    private Map<LocalDate, BigDecimal> banatuJarduera(LocalDate hasiera, BigDecimal iraupena,
            Map<LocalDate, Integer> gaitasuna, Map<LocalDate, BigDecimal> erabilgarri,
            Erronka erronka, boolean hasieraKlaseEgunaEskatu, boolean nahikoaEskatu,
            LocalDate azkenData) {
        Map<LocalDate, BigDecimal> banaketa = new LinkedHashMap<>();
        if (hasiera == null || iraupena == null) return banaketa;
        if (hasieraKlaseEgunaEskatu) require(gaitasuna.containsKey(hasiera),
                "Aukeratutako moduluak ez dauka klaserik hasiera-egun horretan.");
        BigDecimal falta = iraupena;
        for (LocalDate data = hasiera; azkenData != null && !data.isAfter(azkenData)
                && falta.compareTo(BigDecimal.ZERO) > 0; data = data.plusDays(1)) {
            BigDecimal egunekoGaitasuna = erabilgarri.getOrDefault(data, BigDecimal.ZERO);
            if (egunekoGaitasuna.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal zatia = falta.min(egunekoGaitasuna);
            banaketa.put(data, zatia);
            falta = falta.subtract(zatia);
        }
        if (falta.compareTo(BigDecimal.ZERO) > 0) {
            if (nahikoaEskatu) throw new IllegalArgumentException(
                    "Ez dago nahikoa klase-ordurik jarduera erronka amaitu aurretik kokatzeko.");
            if (banaketa.isEmpty()) {
                banaketa.put(hasiera, iraupena);
            } else {
                LocalDate azkena = banaketa.keySet().stream().max(LocalDate::compareTo).orElseThrow();
                banaketa.put(azkena, banaketa.get(azkena).add(falta));
            }
        }
        return banaketa;
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

    private static String hutsikNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    public record KronogramaIkuspegia(Erronka erronka, List<Moduloa> moduluak,
            List<ErronkaKronogramaJarduera> jarduerak, List<KronogramaAstea> asteak,
            boolean koordinatzailea, Set<Long> editaDaitezkeenModuloak, long egunKopurua) {}

    public record KronogramaAstea(int zenbakia, LocalDate hasiera, LocalDate bukaera,
            List<LocalDate> egunak, List<KronogramaLerroa> lerroak) {}

    public record KronogramaLerroa(Long moduloaId, String etiketa, String azpititulua,
            ModuloOrduLaburpena orduLaburpena, boolean komuna, int koloreIndizea,
            int lerroKopurua, boolean editagarria, Set<LocalDate> klaseEgunak,
            List<KronogramaBlokea> blokeak) {}

    public record ModuloOrduLaburpena(int astekoOrduak, int erronkakoOrduak) {}

    public record KronogramaBlokea(ErronkaKronogramaJarduera jarduera, int start, int span,
            int lane, boolean editagarria, BigDecimal orduak) {}

    private record KronogramaOrdutegiTestuingurua(Map<Long, ModuloOrduLaburpena> laburpenak,
            Map<Long, Map<LocalDate, Integer>> egunekoOrduak) {}
}
