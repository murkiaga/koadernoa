package com.koadernoa.app.ethazi.service;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import com.koadernoa.app.ethazi.dto.EthaziForms.ErronkaForm;
import com.koadernoa.app.ethazi.dto.EthaziForms.ErronkaErrubrikaForm;
import com.koadernoa.app.ethazi.entitateak.*;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.*;
import com.koadernoa.app.ethazi.entitateak.errubrikak.*;
import com.koadernoa.app.ethazi.repository.*;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.egutegia.repository.MailaRepository;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.modulua.entitateak.*;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.security.SecurityUtils;
import com.koadernoa.app.objektuak.zikloak.repository.ZikloaRepository;
import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class KinielaService {
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
    private final EthaziService ethazi;
    private final ErronkaRepository erronkak;
    private final MailaRepository mailak;
    private final IkasturteaRepository ikasturteak;
    private final ZikloaRepository zikloak;
    private final LorpenAdierazleaRepository adierazleak;
    private final ErronkaErrubrikaRepository errubrikak;
    private final ErronkaTaldeaRepository erronkaTaldeak;
    private final ErronkaTaldeKideaRepository erronkaTaldeKideak;
    private final MatrikulaRepository matrikulak;
    private final IrakasleaRepository irakasleak;
    private final KoadernoaRepository koadernoak;

    private Ikasturtea ikasturteAktiboa() {
        return ikasturteak.findFirstByAktiboaTrueOrderByIdDesc()
            .orElseThrow(() -> new IllegalArgumentException("Ez dago ikasturte aktiborik."));
    }
    public List<Ikasturtea> ikasturteak() { return ikasturteak.findAllByOrderByIzenaDesc(); }
    public Long ikasturteAktiboaId() { return ikasturteak.findFirstByAktiboaTrueOrderByIdDesc().map(Ikasturtea::getId).orElse(null); }

    public List<Maila> mailak() { return mailak.findAllByAktiboTrueOrderByOrdenaAscIzenaAsc(); }
    public List<Erronka> erronkak(Long zikloaId) {
        return erronkak(zikloaId, null, null);
    }
    public List<Erronka> erronkak(Long zikloaId, Long mailaId, Hizkuntza hizkuntza) {
        Long ikasturteaId = ikasturteak.findFirstByAktiboaTrueOrderByIdDesc().map(Ikasturtea::getId).orElse(null);
        return erronkak(zikloaId, mailaId, hizkuntza, ikasturteaId);
    }
    public List<Erronka> erronkak(Long zikloaId, Long mailaId, Hizkuntza hizkuntza, Long ikasturteaId) {
        var result = zikloaId == null || ikasturteaId == null ? List.<Erronka>of()
            : erronkak.findByZikloaIdAndIkasturteaIdOrderByHasieraDataDescIdDesc(zikloaId, ikasturteaId).stream()
            .filter(e -> mailaId == null || Objects.equals(e.getMaila().getId(), mailaId))
            .filter(e -> hizkuntza == null || e.getHizkuntza() == hizkuntza)
            .toList();
        result.forEach(e -> e.getModuluak().size()); return result;
    }
    public Erronka erronka(Long id) {
        var e = erronkak.findById(id).orElseThrow(() -> new IllegalArgumentException("Erronka ez da aurkitu."));
        e.getModuluak().size(); return e;
    }
    public ErronkaForm form(Long id) {
        var e = erronka(id); var f = new ErronkaForm();
        f.setZikloaId(e.getZikloa().getId()); f.setMailaId(e.getMaila().getId());
        f.setIzena(e.getIzena()); f.setDeskribapena(e.getDeskribapena()); f.setHizkuntza(e.getHizkuntza());
        f.setHasieraData(e.getHasieraData()); f.setBukaeraData(e.getBukaeraData());
        e.getModuluak().forEach(m -> f.getModuloIds().add(m.getId())); return f;
    }
    public static boolean onargarria(Moduloa m, Long zikloaId, Long mailaId, Hizkuntza h) {
        return m.getTaldea() != null && m.getTaldea().getZikloa() != null
            && Objects.equals(m.getTaldea().getZikloa().getId(), zikloaId)
            && m.getMaila() != null && Objects.equals(m.getMaila().getId(), mailaId)
            && h != null && (h == Hizkuntza.ZEHAZTU_GABE || m.getHizkuntza() == h || m.getHizkuntza() == Hizkuntza.ZEHAZTU_GABE);
    }
    @Transactional public void gordeErronka(Long id, ErronkaForm f) {
        require(f.getZikloaId() != null && f.getMailaId() != null, "Aukeratu zikloa eta maila.");
        var z = zikloak.findById(f.getZikloaId()).orElseThrow(() -> new IllegalArgumentException("Zikloa ez da aurkitu."));
        var m = mailak.findById(f.getMailaId()).orElseThrow(() -> new IllegalArgumentException("Maila ez da aurkitu."));
        require(Boolean.TRUE.equals(m.getAktibo()), "Aukeratu maila aktibo bat.");
        require(f.getHizkuntza() != null, "Aukeratu hizkuntza.");
        require(f.getHasieraData() != null && f.getBukaeraData() != null && !f.getBukaeraData().isBefore(f.getHasieraData()), "Bukaera data ezin da hasiera data baino lehenagokoa izan.");
        String izena = testua(f.getIzena(), 200), deskribapena = testua(f.getDeskribapena(), 60000);
        Set<Moduloa> selected = new LinkedHashSet<>();
        for (var module : ethazi.moduluak(f.getZikloaId())) if (f.getModuloIds().contains(module.getId())) {
            require(onargarria(module, f.getZikloaId(), f.getMailaId(), f.getHizkuntza()), "Moduluek erronkaren zikloa, maila eta hizkuntza bete behar dituzte.");
            selected.add(module);
        }
        require(!selected.isEmpty() && selected.size() == f.getModuloIds().size(), "Aukeratu gutxienez baliozko modulu bat.");
        var e = id == null ? new Erronka() : blokeatuErronka(id);
        if (id == null) e.setIkasturtea(ikasturteAktiboa());
        if (id != null && e.getBertsioTaldea() != null)
            require(e.getHizkuntza() == f.getHizkuntza(), "Lotutako bertsioaren hizkuntza ezin da aldatu.");
        e.setIzena(izena); e.setDeskribapena(deskribapena); e.setZikloa(z); e.setMaila(m); e.setHizkuntza(f.getHizkuntza());
        e.setHasieraData(f.getHasieraData()); e.setBukaeraData(f.getBukaeraData()); e.getModuluak().clear(); e.getModuluak().addAll(selected);
        erronkak.save(e);
        if (e.getBertsioTaldea() != null) for (var other : erronkak.findByBertsioTaldea(e.getBertsioTaldea())) {
            if (Objects.equals(other.getId(), e.getId())) continue;
            var mapped = parekoModuluak(e, other.getHizkuntza());
            other.setZikloa(e.getZikloa()); other.setMaila(e.getMaila());
            other.setIkasturtea(e.getIkasturtea());
            other.setHasieraData(e.getHasieraData()); other.setBukaeraData(e.getBukaeraData());
            other.getModuluak().clear(); other.getModuluak().addAll(mapped);
        }
        // Challenge selections that no longer belong to a participating module are removed.
        var family = e.getBertsioTaldea() == null ? List.of(e) : erronkak.findByBertsioTaldea(e.getBertsioTaldea());
        for (var version : family) adierazleak.findDistinctByKinielaLoturakErronkakId(version.getId()).forEach(a ->
            a.getKinielaLoturak().forEach(l -> {
                if (version.getModuluak().stream().noneMatch(module -> module.getId().equals(l.getModuloa().getId())))
                    l.getErronkak().removeIf(item -> item.getId().equals(version.getId()));
            }));
        family.forEach(this::sinkronizatuErrubrikak);
        family.forEach(this::kenduHautagaiEzDirenKideak);
    }
    private Erronka blokeatuErronka(Long id) {
        var groups = entityManager.createQuery("select e.bertsioTaldea from Erronka e where e.id = :id", Long.class)
            .setParameter("id", id).getResultList();
        require(!groups.isEmpty(), "Erronka ez da aurkitu.");
        Long group = groups.get(0);
        if (group != null) {
            // Lock the family before loading a version, including its current common fields.
            entityManager.find(com.koadernoa.app.ethazi.entitateak.ErronkaBertsioTaldea.class,
                group, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        }
        var e = erronkak.findLockedById(id).orElseThrow(() -> new IllegalArgumentException("Erronka ez da aurkitu."));
        require(Objects.equals(group, e.getBertsioTaldea()), "Bertsioak aldatu dira. Kargatu berriro erronka.");
        e.getModuluak().size();
        return e;
    }
    public List<Erronka> bertsioak(Long id) {
        var e = erronka(id);
        return e.getBertsioTaldea() == null ? List.of(e) : erronkak.findByBertsioTaldea(e.getBertsioTaldea());
    }
    private Set<Moduloa> parekoModuluak(Erronka source, Hizkuntza language) {
        Set<Moduloa> result = new LinkedHashSet<>();
        var available = ethazi.moduluak(source.getZikloa().getId());
        for (var original : source.getModuluak()) {
            require(original.getEeiKodea() != null && !original.getEeiKodea().isBlank(), "Bertsioa lotzeko modulu guztiek EEI kodea behar dute.");
            var matches = available.stream().filter(m -> Objects.equals(m.getEeiKodea(), original.getEeiKodea())
                && Objects.equals(m.getMaila().getId(), source.getMaila().getId()) && m.getHizkuntza() == language).toList();
            if (matches.isEmpty()) matches = available.stream().filter(m -> Objects.equals(m.getEeiKodea(), original.getEeiKodea())
                && Objects.equals(m.getMaila().getId(), source.getMaila().getId()) && m.getHizkuntza() == Hizkuntza.ZEHAZTU_GABE).toList();
            require(matches.size() == 1, "EEI " + original.getEeiKodea() + ": " + language.getEtiketa() + " hizkuntzako pareko modulu bakarra behar da. Berrikusi moduluak.");
            result.add(matches.get(0));
        }
        return result;
    }
    @Transactional public Long sortuBertsioa(Long id, Hizkuntza language) {
        require(language != null && language != Hizkuntza.ZEHAZTU_GABE, "Aukeratu bertsioaren hizkuntza.");
        var source = blokeatuErronka(id);
        require(source.getHizkuntza() != Hizkuntza.ZEHAZTU_GABE, "Zehaztu jatorrizko erronkaren hizkuntza lehenengo.");
        for (var version : bertsioak(id)) if (version.getHizkuntza() == language) return version.getId();
        var modules = parekoModuluak(source, language);
        if (source.getBertsioTaldea() == null) {
            var group = new com.koadernoa.app.ethazi.entitateak.ErronkaBertsioTaldea();
            entityManager.persist(group); source.setBertsioTaldea(group.getId());
        }
        var version = new Erronka(); version.setBertsioTaldea(source.getBertsioTaldea());
        version.setIzena(source.getIzena()); version.setDeskribapena(source.getDeskribapena());
        version.setZikloa(source.getZikloa()); version.setMaila(source.getMaila()); version.setHizkuntza(language);
        version.setIkasturtea(source.getIkasturtea());
        version.setHasieraData(source.getHasieraData()); version.setBukaeraData(source.getBukaeraData());
        version.getModuluak().addAll(modules);
        version = erronkak.save(version);
        sinkronizatuErrubrikak(version);
        return version.getId();
    }
    @Transactional public void ezabatuErronka(Long id) {
        var e = blokeatuErronka(id);
        adierazleak.findByErronkakId(id).forEach(a -> a.getErronkak().remove(e));
        adierazleak.findDistinctByKinielaLoturakErronkakId(id).forEach(a ->
            a.getKinielaLoturak().forEach(l -> l.getErronkak().removeIf(item -> item.getId().equals(id))));
        adierazleak.flush();
        errubrikak.deleteAll(errubrikak.findByErronkaId(id));
        errubrikak.flush();
        erronkaTaldeKideak.deleteByErronkaId(id);
        erronkaTaldeak.deleteByErronkaId(id);
        erronkak.delete(e);
    }

    private void sinkronizatuErrubrikak(Erronka erronka) {
        var parteHartzaileak = erronka.getModuluak().stream().map(Moduloa::getId).collect(java.util.stream.Collectors.toSet());
        var daudenak = errubrikak.findByErronkaId(erronka.getId());
        errubrikak.deleteAll(daudenak.stream().filter(r -> !parteHartzaileak.contains(r.getModuloa().getId())).toList());
        var badira = daudenak.stream().map(r -> r.getModuloa().getId()).collect(java.util.stream.Collectors.toSet());
        for (var moduloa : erronka.getModuluak()) if (!badira.contains(moduloa.getId())) {
            var r = new ErronkaErrubrika(); r.setErronka(erronka); r.setModuloa(moduloa); errubrikak.save(r);
        }
    }

    public List<ErronkaTaldea> taldeak(Long erronkaId) {
        erronka(erronkaId);
        var result = erronkaTaldeak.findByErronkaIdOrderByOrdenaAsc(erronkaId);
        result.forEach(t -> t.getKideak().forEach(k -> k.getIkaslea().getIzenOsoa()));
        return result;
    }

    public List<Ikaslea> taldeHautagaiak(Long erronkaId) {
        var e = erronka(erronkaId);
        if (e.getIkasturtea() == null || e.getModuluak().isEmpty()) return List.of();
        return matrikulak.findErronkarakoHautagaiak(e.getIkasturtea().getId(),
            e.getModuluak().stream().map(Moduloa::getId).toList());
    }

    public com.koadernoa.app.ethazi.dto.EthaziForms.ErronkaTaldeEsleipenForm taldeEsleipenForm(Long erronkaId) {
        var f = new com.koadernoa.app.ethazi.dto.EthaziForms.ErronkaTaldeEsleipenForm();
        var assigned = erronkaTaldeKideak.findByErronkaId(erronkaId).stream()
            .collect(java.util.stream.Collectors.toMap(k -> k.getIkaslea().getId(), k -> k.getTaldea().getId()));
        for (var ikaslea : taldeHautagaiak(erronkaId)) {
            var row = new com.koadernoa.app.ethazi.dto.EthaziForms.ErronkaIkasleTaldeForm();
            row.setIkasleaId(ikaslea.getId()); row.setTaldeaId(assigned.get(ikaslea.getId())); f.getIkasleak().add(row);
        }
        return f;
    }

    @Transactional public Long gehituTaldea(Long erronkaId) {
        var e = blokeatuErronka(erronkaId);
        int ordena = erronkaTaldeak.findByErronkaIdOrderByOrdenaAsc(erronkaId).stream()
            .mapToInt(ErronkaTaldea::getOrdena).max().orElse(0) + 1;
        var t = new ErronkaTaldea(); t.setErronka(e); t.setOrdena(ordena); t.setIzena("Taldea " + ordena);
        return erronkaTaldeak.save(t).getId();
    }

    @Transactional public void ezabatuTaldea(Long erronkaId, Long taldeaId) {
        var t = erronkaTaldeak.findById(taldeaId).orElseThrow(() -> new IllegalArgumentException("Taldea ez da aurkitu."));
        require(t.getErronka().getId().equals(erronkaId), "Taldea ez da erronka honetakoa.");
        require(t.getKideak().isEmpty(), "Ezin da taldea ezabatu ikasleak dituen bitartean.");
        erronkaTaldeak.delete(t);
    }

    @Transactional public void gordeTaldeEsleipenak(Long erronkaId,
            com.koadernoa.app.ethazi.dto.EthaziForms.ErronkaTaldeEsleipenForm f) {
        blokeatuErronka(erronkaId);
        var hautagaiak = taldeHautagaiak(erronkaId);
        var hautagaiIds = hautagaiak.stream().map(Ikaslea::getId).collect(java.util.stream.Collectors.toSet());
        var rows = f.getIkasleak() == null ? List.<com.koadernoa.app.ethazi.dto.EthaziForms.ErronkaIkasleTaldeForm>of() : f.getIkasleak();
        require(rows.size() == hautagaiIds.size()
                && rows.stream().map(x -> x.getIkasleaId()).collect(java.util.stream.Collectors.toSet()).equals(hautagaiIds),
            "Ikasleen zerrenda aldatu da. Kargatu berriro taldeak.");
        var taldeMap = erronkaTaldeak.findByErronkaIdOrderByOrdenaAsc(erronkaId).stream()
            .collect(java.util.stream.Collectors.toMap(ErronkaTaldea::getId, t -> t));
        require(hautagaiak.isEmpty() || !taldeMap.isEmpty(), "Sortu gutxienez talde bat.");
        require(rows.stream().allMatch(x -> x.getTaldeaId() != null && taldeMap.containsKey(x.getTaldeaId())),
            "Ikasle guztiek talde batean egon behar dute.");
        require(rows.isEmpty() || rows.stream().map(x -> x.getTaldeaId()).collect(java.util.stream.Collectors.toSet())
                .equals(taldeMap.keySet()), "Sortutako talde guztiek gutxienez ikasle bat izan behar dute.");
        var previous = erronkaTaldeKideak.findByErronkaId(erronkaId);
        previous.forEach(k -> k.getTaldea().getKideak().remove(k));
        erronkaTaldeKideak.deleteAll(previous); erronkaTaldeKideak.flush();
        var ikasleMap = hautagaiak.stream().collect(java.util.stream.Collectors.toMap(Ikaslea::getId, i -> i));
        var e = erronka(erronkaId);
        for (var row : rows) {
            var k = new ErronkaTaldeKidea(); k.setErronka(e); k.setTaldea(taldeMap.get(row.getTaldeaId()));
            k.setIkaslea(ikasleMap.get(row.getIkasleaId()));
            taldeMap.get(row.getTaldeaId()).getKideak().add(k); erronkaTaldeKideak.save(k);
        }
    }

    private void kenduHautagaiEzDirenKideak(Erronka e) {
        if (e.getIkasturtea() == null || e.getModuluak().isEmpty()) return;
        var allowed = matrikulak.findErronkarakoHautagaiak(e.getIkasturtea().getId(),
            e.getModuluak().stream().map(Moduloa::getId).toList()).stream().map(Ikaslea::getId)
            .collect(java.util.stream.Collectors.toSet());
        var remove = erronkaTaldeKideak.findByErronkaId(e.getId()).stream()
            .filter(k -> !allowed.contains(k.getIkaslea().getId())).toList();
        remove.forEach(k -> k.getTaldea().getKideak().remove(k));
        erronkaTaldeKideak.deleteAll(remove);
    }

    @Transactional public Long inportatuErronka(Long sourceId) {
        var source = erronka(sourceId);
        var targetYear = ikasturteAktiboa();
        require(source.getIkasturtea() != null && !source.getIkasturtea().getId().equals(targetYear.getId()),
            "Aukeratu aurreko ikasturte bateko erronka.");
        int years = ikasturteHasiera(targetYear) - ikasturteHasiera(source.getIkasturtea());
        var target = new Erronka(); target.setIzena(source.getIzena()); target.setDeskribapena(source.getDeskribapena());
        target.setZikloa(source.getZikloa()); target.setMaila(source.getMaila()); target.setHizkuntza(source.getHizkuntza());
        target.setIkasturtea(targetYear); target.setHasieraData(source.getHasieraData().plusYears(years));
        target.setBukaeraData(source.getBukaeraData().plusYears(years)); target.getModuluak().addAll(source.getModuluak());
        target = erronkak.save(target);

        // Kinielako erronka-hautaketak kopiatzen dira oraindik indarrean dauden loturetan.
        for (var a : adierazleak.findDistinctByKinielaLoturakErronkakId(sourceId)) for (var l : a.getKinielaLoturak()) {
            boolean sourceSelected = l.getErronkak().stream().anyMatch(e -> e.getId().equals(sourceId));
            boolean targetModule = target.getModuluak().stream().anyMatch(m -> m.getId().equals(l.getModuloa().getId()));
            if (sourceSelected && targetModule) l.getErronkak().add(target);
        }
        sinkronizatuErrubrikak(target);
        for (var sourceRubric : errubrikak.findByErronkaId(sourceId)) {
            var targetRubric = errubrikak.findByErronkaIdAndModuloaId(target.getId(), sourceRubric.getModuloa().getId()).orElseThrow();
            kopiatuErrubrika(sourceRubric, targetRubric);
        }
        errubrikak.flush();
        return target.getId();
    }

    private int ikasturteHasiera(Ikasturtea year) {
        var matcher = java.util.regex.Pattern.compile("(\\d{4})").matcher(year.getIzena() == null ? "" : year.getIzena());
        require(matcher.find(), "Ikasturtearen izenak hasierako urtea eduki behar du (adib. 2026-2027).");
        return Integer.parseInt(matcher.group(1));
    }

    private void kopiatuErrubrika(ErronkaErrubrika source, ErronkaErrubrika target) {
        source.getMailak().size(); source.getEbidentziak().forEach(e -> { e.getMailak().size(); e.getLorpenAdierazleak().size(); });
        Map<Long, ErronkaErrubrikaMaila> levelMap = new HashMap<>();
        for (var old : source.getMailak()) {
            var copy = new ErronkaErrubrikaMaila(); copy.setErrubrika(target); copy.setOrdena(old.getOrdena());
            copy.setIzena(old.getIzena()); copy.setBalioa(old.getBalioa()); target.getMailak().add(copy); entityManager.persist(copy);
            levelMap.put(old.getId(), copy);
        }
        var available = modulukoAdierazleEntitateak(target).stream().map(LorpenAdierazlea::getId)
            .collect(java.util.stream.Collectors.toSet());
        for (var old : source.getEbidentziak()) {
            var copy = new ErronkaEbidentzia(); copy.setErrubrika(target); copy.setOrdena(old.getOrdena());
            copy.setDeskribapena(old.getDeskribapena()); copy.setPisua(old.getPisua());
            old.getLorpenAdierazleak().stream().filter(a -> available.contains(a.getId())).forEach(copy.getLorpenAdierazleak()::add);
            for (var oldCell : old.getMailak()) {
                var cell = new ErronkaEbidentziaMaila(); cell.setEbidentzia(copy); cell.setMaila(levelMap.get(oldCell.getMaila().getId()));
                cell.setDeskribapena(oldCell.getDeskribapena()); copy.getMailak().add(cell);
            }
            target.getEbidentziak().add(copy); entityManager.persist(copy);
        }
    }

    public record ErrubrikaIturriMaila(Long id, String izena, BigDecimal balioa) {}
    public record ErrubrikaIturriEbidentzia(String deskribapena, BigDecimal pisua,
            List<String> adierazleKodeak, List<String> mailaAzalpenak) {}
    public record ErrubrikaIturria(Long id, String erronkaIzena, String moduluaKodea, String hizkuntza,
            String ikasturtea, List<ErrubrikaIturriMaila> mailak, List<ErrubrikaIturriEbidentzia> ebidentziak) {}

    public List<ErrubrikaIturria> errubrikaIturriak(Long erronkaId, Long moduloaId) {
        var target = errubrika(erronkaId, moduloaId);
        String eeiKodea = target.getModuloa().getEeiKodea();
        if (eeiKodea == null || eeiKodea.isBlank()) return List.of();
        return errubrikak.findByModuloa_EeiKodeaOrderByErronka_IdDesc(eeiKodea).stream()
            .filter(source -> !source.getId().equals(target.getId()))
            .filter(source -> !source.getMailak().isEmpty() || !source.getEbidentziak().isEmpty())
            .map(this::errubrikaIturria).toList();
    }

    private ErrubrikaIturria errubrikaIturria(ErronkaErrubrika source) {
        var levels = source.getMailak().stream()
            .map(m -> new ErrubrikaIturriMaila(m.getId(), m.getIzena(), m.getBalioa())).toList();
        var evidence = source.getEbidentziak().stream().map(eb -> new ErrubrikaIturriEbidentzia(
            eb.getDeskribapena(), eb.getPisua(), eb.getLorpenAdierazleak().stream().map(this::adierazleKodea)
                .sorted(String.CASE_INSENSITIVE_ORDER).toList(),
            source.getMailak().stream().map(level -> eb.getMailak().stream()
                .filter(cell -> cell.getMaila().getId().equals(level.getId()))
                .map(ErronkaEbidentziaMaila::getDeskribapena).findFirst().orElse("")).toList())).toList();
        var challenge = source.getErronka();
        String language = challenge.getHizkuntza() == null ? "" : challenge.getHizkuntza().getEtiketa();
        String year = challenge.getIkasturtea() == null ? "—" : challenge.getIkasturtea().getIzena();
        return new ErrubrikaIturria(source.getId(), challenge.getIzena(), source.getModuloa().getKodea(),
            language, year, levels, evidence);
    }

    @Transactional public void inportatuErrubrika(Long erronkaId, Long moduloaId, Long iturriErrubrikaId) {
        var target = errubrika(erronkaId, moduloaId);
        entityManager.lock(target, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        require(target.getMailak().isEmpty() && target.getEbidentziak().isEmpty(),
            "Errubrika ez dago hutsik; inportazioa ezin da egin.");
        var source = errubrikak.findById(iturriErrubrikaId)
            .orElseThrow(() -> new IllegalArgumentException("Inportatzeko errubrika ez da aurkitu."));
        require(!source.getId().equals(target.getId()), "Errubrika ezin da bere burutik inportatu.");
        String targetEei = target.getModuloa().getEeiKodea();
        String sourceEei = source.getModuloa().getEeiKodea();
        require(targetEei != null && !targetEei.isBlank() && targetEei.equals(sourceEei),
            "Iturriaren modulua ez da EEI bereko modulua.");
        require(!source.getMailak().isEmpty() || !source.getEbidentziak().isEmpty(),
            "Iturri-errubrika hutsik dago.");
        kopiatuErrubrika(source, target);
        errubrikak.flush();
    }

    @Transactional public ErronkaErrubrika errubrika(Long erronkaId, Long moduloaId) {
        var e = erronka(erronkaId);
        require(e.getModuluak().stream().anyMatch(m -> m.getId().equals(moduloaId)), "Modulua ez da erronka honetako parte-hartzailea.");
        if (errubrikak.findByErronkaIdAndModuloaId(erronkaId, moduloaId).isEmpty()) sinkronizatuErrubrikak(e);
        var r = errubrikak.findByErronkaIdAndModuloaId(erronkaId, moduloaId)
            .orElseThrow(() -> new IllegalArgumentException("Moduluaren errubrika ez da aurkitu."));
        r.getMailak().size();
        r.getEbidentziak().forEach(eb -> { eb.getMailak().size(); eb.getLorpenAdierazleak().size(); eb.getTaldeNotak().size(); });
        return r;
    }

    public ErronkaErrubrikaForm errubrikaForm(Long erronkaId, Long moduloaId) {
        var r = errubrika(erronkaId, moduloaId); var f = new ErronkaErrubrikaForm();
        var teams = taldeak(erronkaId);
        for (var m : r.getMailak()) {
            var mf = new com.koadernoa.app.ethazi.dto.EthaziForms.ErronkaMailaForm();
            mf.setId(m.getId()); mf.setIzena(m.getIzena()); mf.setBalioa(m.getBalioa()); f.getMailak().add(mf);
        }
        for (var eb : r.getEbidentziak()) {
            var ef = new com.koadernoa.app.ethazi.dto.EthaziForms.ErronkaEbidentziaForm();
            ef.setId(eb.getId()); ef.setDeskribapena(eb.getDeskribapena()); ef.setPisua(eb.getPisua());
            eb.getLorpenAdierazleak().forEach(a -> ef.getAdierazleaIds().add(a.getId()));
            for (var m : r.getMailak()) {
                var cm = new com.koadernoa.app.ethazi.dto.EthaziForms.ErronkaEbidentziaMailaForm(); cm.setMailaId(m.getId());
                eb.getMailak().stream().filter(c -> c.getMaila().getId().equals(m.getId())).findFirst().ifPresent(c -> cm.setDeskribapena(c.getDeskribapena()));
                ef.getMailak().add(cm);
            }
            for (var team : teams) {
                var tf = new com.koadernoa.app.ethazi.dto.EthaziForms.ErronkaTaldeNotaForm(); tf.setTaldeaId(team.getId());
                eb.getTaldeNotak().stream().filter(n -> n.getTaldea().getId().equals(team.getId())).findFirst()
                    .ifPresent(n -> {
                        if (n.getMaila() != null) { tf.setMailaId(n.getMaila().getId()); tf.setMailaIzena(n.getMaila().getIzena()); }
                        else if (n.getNota() != null) r.getMailak().stream()
                            .filter(m -> m.getBalioa().compareTo(n.getNota()) == 0).findFirst().ifPresent(m -> {
                                tf.setMailaId(m.getId()); tf.setMailaIzena(m.getIzena());
                            });
                        tf.setOndoEgindakoak(n.getOndoEgindakoak());
                        tf.setHobetuBeharrekoak(n.getHobetuBeharrekoak());
                    });
                ef.getTaldeNotak().add(tf);
            }
            f.getEbidentziak().add(ef);
        }
        return f;
    }

    @Transactional public void gordeErrubrika(Long erronkaId, Long moduloaId, ErronkaErrubrikaForm f) {
        var r = errubrika(erronkaId, moduloaId);
        var availableIndicators = modulukoAdierazleak(r).stream().map(ErrubrikaAdierazlea::id).collect(java.util.stream.Collectors.toSet());
        var levelIds = r.getMailak().stream().map(ErronkaErrubrikaMaila::getId).collect(java.util.stream.Collectors.toSet());
        var evidenceIds = r.getEbidentziak().stream().map(ErronkaEbidentzia::getId).collect(java.util.stream.Collectors.toSet());
        require(f.getMailak().size() == levelIds.size() && f.getMailak().stream().map(x -> x.getId()).collect(java.util.stream.Collectors.toSet()).equals(levelIds),
            "Mailakatzea aldatu da. Kargatu berriro errubrika.");
        require(f.getEbidentziak().size() == evidenceIds.size() && f.getEbidentziak().stream().map(x -> x.getId()).collect(java.util.stream.Collectors.toSet()).equals(evidenceIds),
            "Ebidentziak aldatu dira. Kargatu berriro errubrika.");
        for (var mf : f.getMailak()) {
            var m = r.getMailak().stream().filter(x -> x.getId().equals(mf.getId())).findFirst().orElseThrow();
            m.setIzena(testua(mf.getIzena(), 100));
            require(mf.getBalioa() != null && mf.getBalioa().compareTo(BigDecimal.ZERO) >= 0
                && mf.getBalioa().compareTo(BigDecimal.TEN) <= 0 && mf.getBalioa().stripTrailingZeros().scale() <= 2,
                "Mailaren balioa 0 eta 10 artekoa izan behar da, gehienez bi hamartarrekin.");
            m.setBalioa(mf.getBalioa());
        }
        for (var ef : f.getEbidentziak()) {
            var eb = r.getEbidentziak().stream().filter(x -> x.getId().equals(ef.getId())).findFirst().orElseThrow();
            eb.setDeskribapena(testua(ef.getDeskribapena(), 60000));
            require(ef.getPisua() != null && ef.getPisua().compareTo(BigDecimal.ZERO) >= 0
                && ef.getPisua().compareTo(new BigDecimal("100")) <= 0 && ef.getPisua().stripTrailingZeros().scale() <= 2,
                "Ebidentziaren pisua 0 eta 100 artekoa izan behar da, gehienez bi hamartarrekin.");
            eb.setPisua(ef.getPisua());
            require(ef.getAdierazleaIds() != null && availableIndicators.containsAll(ef.getAdierazleaIds()),
                "Hautatutako lorpen-adierazleren bat ez da modulu honetakoa.");
            eb.getLorpenAdierazleak().removeIf(a -> !ef.getAdierazleaIds().contains(a.getId()));
            var selectedIds = eb.getLorpenAdierazleak().stream().map(LorpenAdierazlea::getId).collect(java.util.stream.Collectors.toSet());
            modulukoAdierazleEntitateak(r).stream().filter(a -> ef.getAdierazleaIds().contains(a.getId()) && !selectedIds.contains(a.getId()))
                .forEach(eb.getLorpenAdierazleak()::add);
            require(ef.getMailak().size() == levelIds.size() && ef.getMailak().stream().map(x -> x.getMailaId()).collect(java.util.stream.Collectors.toSet()).equals(levelIds),
                "Ebidentziaren mailak aldatu dira. Kargatu berriro errubrika.");
            for (var cf : ef.getMailak()) {
                var cell = eb.getMailak().stream().filter(x -> x.getMaila().getId().equals(cf.getMailaId())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Ebidentziaren maila ez da aurkitu."));
                String description = cf.getDeskribapena() == null ? "" : cf.getDeskribapena().strip();
                require(description.length() <= 60000, "Mailaren azalpena luzeegia da.");
                cell.setDeskribapena(description);
            }
        }
        // Mailaren balioa aldatu bada, lehendik maila hori hautatuta duten
        // talde-noten kalkulu-balioa ere eguneratu.
        r.getEbidentziak().stream().flatMap(eb -> eb.getTaldeNotak().stream())
            .filter(n -> n.getMaila() != null).forEach(n -> n.setNota(n.getMaila().getBalioa()));
        errubrikak.flush();
    }

    public boolean errubrikaEditatuDezake(Authentication auth) {
        return SecurityUtils.isKudeatzailea(auth);
    }

    public boolean moduluaKalifikatuDezake(Authentication auth, Long moduloaId) {
        if (SecurityUtils.isKudeatzailea(auth)) return true;
        if (!SecurityUtils.hasAnyRole(auth, "IRAKASLEA") || moduloaId == null) return false;
        String ident = auth.getPrincipal() instanceof OAuth2User oauth
            ? oauth.getAttribute("email") : auth.getName();
        if (ident == null || ident.isBlank()) return false;
        var irakaslea = irakasleak.findByEmailaIgnoreCase(ident.strip())
            .or(() -> irakasleak.findByIzenaIgnoreCase(ident.strip()));
        return irakaslea.isPresent()
            && koadernoak.existsAktiboIkasturtekoKoadernoaIrakaslearentzat(moduloaId, irakaslea.get().getId());
    }

    public boolean erronkaKalifikatuDezake(Authentication auth, Long erronkaId, Long moduloaId) {
        if (SecurityUtils.isKudeatzailea(auth)) return true;
        var challenge = erronkak.findById(erronkaId).orElse(null);
        return challenge != null && challenge.getIkasturtea() != null && challenge.getIkasturtea().isAktiboa()
            && challenge.getModuluak().stream().anyMatch(m -> m.getId().equals(moduloaId))
            && moduluaKalifikatuDezake(auth, moduloaId);
    }

    @Transactional public void gordeTaldeNota(Long erronkaId, Long moduloaId, Long ebidentziaId,
            Long taldeaId, Long mailaId, Authentication auth) {
        if (!erronkaKalifikatuDezake(auth, erronkaId, moduloaId)) {
            throw new AccessDeniedException("Ez duzu modulu honetako kalifikazioak aldatzeko baimenik.");
        }
        var r = errubrika(erronkaId, moduloaId);
        var eb = r.getEbidentziak().stream().filter(x -> x.getId().equals(ebidentziaId)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Ebidentzia ez da errubrika honetakoa."));
        var team = erronkaTaldeak.findById(taldeaId)
            .orElseThrow(() -> new IllegalArgumentException("Taldea ez da aurkitu."));
        require(team.getErronka().getId().equals(erronkaId), "Taldea ez da erronka honetakoa.");
        var existing = eb.getTaldeNotak().stream().filter(n -> n.getTaldea().getId().equals(taldeaId)).findFirst();
        if (mailaId == null) {
            existing.ifPresent(note -> {
                note.setMaila(null); note.setNota(null);
                if (feedbackHutsik(note)) eb.getTaldeNotak().remove(note);
            });
        } else {
            var selectedLevel = r.getMailak().stream().filter(m -> m.getId().equals(mailaId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Hautatutako maila ez da errubrika honetakoa."));
            var note = existing.orElseGet(() -> {
                var n = new ErronkaEbidentziaTaldeNota(); n.setEbidentzia(eb); n.setTaldea(team);
                eb.getTaldeNotak().add(n); return n;
            });
            note.setMaila(selectedLevel); note.setNota(selectedLevel.getBalioa());
        }
        errubrikak.flush();
    }

    @Transactional public void gordeTaldeOharrak(Long erronkaId, Long moduloaId, Long ebidentziaId,
            Long taldeaId, String ondoEgindakoak, String hobetuBeharrekoak, Authentication auth) {
        if (!erronkaKalifikatuDezake(auth, erronkaId, moduloaId)) {
            throw new AccessDeniedException("Ez duzu modulu honetako kalifikazioak aldatzeko baimenik.");
        }
        var r = errubrika(erronkaId, moduloaId);
        var eb = r.getEbidentziak().stream().filter(x -> x.getId().equals(ebidentziaId)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Ebidentzia ez da errubrika honetakoa."));
        var team = erronkaTaldeak.findById(taldeaId)
            .orElseThrow(() -> new IllegalArgumentException("Taldea ez da aurkitu."));
        require(team.getErronka().getId().equals(erronkaId), "Taldea ez da erronka honetakoa.");
        String strengths = aukerakoTestua(ondoEgindakoak, 60000);
        String improvements = aukerakoTestua(hobetuBeharrekoak, 60000);
        var existing = eb.getTaldeNotak().stream().filter(n -> n.getTaldea().getId().equals(taldeaId)).findFirst();
        if (strengths.isEmpty() && improvements.isEmpty() && existing.isPresent()
                && existing.get().getMaila() == null && existing.get().getNota() == null) {
            eb.getTaldeNotak().remove(existing.get());
        } else if (!strengths.isEmpty() || !improvements.isEmpty() || existing.isPresent()) {
            var note = existing.orElseGet(() -> {
                var n = new ErronkaEbidentziaTaldeNota(); n.setEbidentzia(eb); n.setTaldea(team);
                eb.getTaldeNotak().add(n); return n;
            });
            note.setOndoEgindakoak(strengths); note.setHobetuBeharrekoak(improvements);
        }
        errubrikak.flush();
    }

    private static boolean feedbackHutsik(ErronkaEbidentziaTaldeNota note) {
        return (note.getOndoEgindakoak() == null || note.getOndoEgindakoak().isBlank())
            && (note.getHobetuBeharrekoak() == null || note.getHobetuBeharrekoak().isBlank());
    }

    private static String aukerakoTestua(String value, int max) {
        String normalized = value == null ? "" : value.strip();
        require(normalized.length() <= max, "Oharra luzeegia da.");
        return normalized;
    }

    @Transactional public Long gehituErrubrikaMaila(Long erronkaId, Long moduloaId) {
        var r = errubrika(erronkaId, moduloaId); int ordena = r.getMailak().stream().mapToInt(ErronkaErrubrikaMaila::getOrdena).max().orElse(0) + 1;
        var m = new ErronkaErrubrikaMaila(); m.setErrubrika(r); m.setOrdena(ordena); m.setIzena(ordena + ". maila"); m.setBalioa(BigDecimal.ZERO);
        r.getMailak().add(m);
        // Persist the level first: evidence cells have a mandatory FK to it and Hibernate may otherwise
        // flush the child collection before assigning the new level an identity.
        entityManager.persist(m);
        for (var eb : r.getEbidentziak()) {
            var c = new ErronkaEbidentziaMaila(); c.setEbidentzia(eb); c.setMaila(m); c.setDeskribapena("");
            eb.getMailak().add(c); m.getEbidentziaMailak().add(c); entityManager.persist(c);
        }
        errubrikak.flush(); return m.getId();
    }

    @Transactional public void ezabatuErrubrikaMaila(Long erronkaId, Long moduloaId, Long mailaId) {
        var r = errubrika(erronkaId, moduloaId); var m = r.getMailak().stream().filter(x -> x.getId().equals(mailaId)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Maila ez da errubrika honetakoa."));
        boolean erabilita = r.getEbidentziak().stream().flatMap(eb -> eb.getMailak().stream())
            .anyMatch(c -> c.getMaila().getId().equals(mailaId) && c.getDeskribapena() != null && !c.getDeskribapena().isBlank());
        boolean kalifikazioetan = r.getEbidentziak().stream().flatMap(eb -> eb.getTaldeNotak().stream())
            .anyMatch(n -> n.getMaila() != null && n.getMaila().getId().equals(mailaId));
        require(!erabilita && !kalifikazioetan, kalifikazioetan
            ? "Mailakatzea talde baten kalifikazioan erabiltzen ari da. Kendu hautaketa ezabatu aurretik."
            : "Mailakatzea ebidentzia batean erabiltzen ari da. Hustu maila horretako azalpenak ezabatu aurretik.");
        r.getEbidentziak().forEach(eb -> eb.getMailak().removeIf(c -> c.getMaila().getId().equals(mailaId)));
        r.getMailak().remove(m); errubrikak.flush();
    }

    @Transactional public Long gehituEbidentzia(Long erronkaId, Long moduloaId) {
        var r = errubrika(erronkaId, moduloaId); int ordena = r.getEbidentziak().stream().mapToInt(ErronkaEbidentzia::getOrdena).max().orElse(0) + 1;
        var eb = new ErronkaEbidentzia(); eb.setErrubrika(r); eb.setOrdena(ordena); eb.setDeskribapena("Ebidentzia berria"); eb.setPisua(BigDecimal.ZERO);
        for (var m : r.getMailak()) { var c = new ErronkaEbidentziaMaila(); c.setEbidentzia(eb); c.setMaila(m); c.setDeskribapena(""); eb.getMailak().add(c); m.getEbidentziaMailak().add(c); }
        r.getEbidentziak().add(eb); errubrikak.flush(); return eb.getId();
    }

    @Transactional public void ezabatuEbidentzia(Long erronkaId, Long moduloaId, Long ebidentziaId) {
        var r = errubrika(erronkaId, moduloaId); var eb = r.getEbidentziak().stream().filter(x -> x.getId().equals(ebidentziaId)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Ebidentzia ez da errubrika honetakoa."));
        r.getMailak().forEach(m -> m.getEbidentziaMailak().removeIf(c -> c.getEbidentzia().getId().equals(ebidentziaId)));
        r.getEbidentziak().remove(eb); errubrikak.flush();
    }

    public record ErrubrikaAdierazlea(Long id, String kodea, String deskribapena, boolean erabilita) {}

    private List<LorpenAdierazlea> modulukoAdierazleEntitateak(ErronkaErrubrika r) {
        return adierazleak(r.getErronka().getZikloa().getId()).stream()
            .filter(a -> a.getKinielaLoturak().stream().anyMatch(l -> l.getModuloa().getId().equals(r.getModuloa().getId())
                && l.getErronkak().stream().anyMatch(e -> e.getId().equals(r.getErronka().getId()))))
            .sorted(Comparator.comparing(this::adierazleKodea, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public List<ErrubrikaAdierazlea> modulukoAdierazleak(Long erronkaId, Long moduloaId) {
        return modulukoAdierazleak(errubrika(erronkaId, moduloaId));
    }

    private List<ErrubrikaAdierazlea> modulukoAdierazleak(ErronkaErrubrika r) {
        var used = r.getEbidentziak().stream().flatMap(eb -> eb.getLorpenAdierazleak().stream())
            .map(LorpenAdierazlea::getId).collect(java.util.stream.Collectors.toSet());
        Hizkuntza language = r.getModuloa().getHizkuntza() == Hizkuntza.ZEHAZTU_GABE ? r.getErronka().getHizkuntza() : r.getModuloa().getHizkuntza();
        if (language == Hizkuntza.ZEHAZTU_GABE) language = Hizkuntza.EUSKARA;
        final Hizkuntza displayLanguage = language;
        return modulukoAdierazleEntitateak(r).stream().map(a -> new ErrubrikaAdierazlea(
            a.getId(), adierazleKodea(a), a.deskribapena(displayLanguage), used.contains(a.getId()))).toList();
    }

    private String adierazleKodea(LorpenAdierazlea a) {
        return a.getGaitasunMaila().getGaitasuna().getKodea() + "." + a.getGaitasunMaila().getMaila().getOrdena() + "." + a.getOrdena();
    }

    public record ErronkaZutabea(Long id, String izena, String maila, Hizkuntza hizkuntza, Set<Long> moduloIds,
            String sinkronizazioGakoa) {}
    public List<ErronkaZutabea> erronkaZutabeak(Long zikloaId) {
        return erronkak(zikloaId).stream()
            .sorted(Comparator.comparing((Erronka e) -> e.getMaila().getOrdena(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(e -> e.getMaila().getId())
                .thenComparing(Erronka::getHasieraData).thenComparing(Erronka::getId))
            .map(e -> new ErronkaZutabea(e.getId(), e.getIzena(), e.getMaila().getIzena(), e.getHizkuntza(),
                e.getModuluak().stream().map(Moduloa::getId).collect(java.util.stream.Collectors.toSet()),
                e.getBertsioTaldea() == null ? "erronka-" + e.getId() : "bertsioa-" + e.getBertsioTaldea())).toList();
    }

    private LorpenAdierazlea ziklokoAdierazlea(Long zikloaId, Long id) {
        var a = adierazleak.findById(id).orElseThrow(() -> new IllegalArgumentException("Adierazlea ez da aurkitu."));
        var gaitasuna = a.getGaitasunMaila().getGaitasuna();
        require(gaitasuna.getZikloa() == null || Objects.equals(gaitasuna.getZikloa().getId(), zikloaId),
            "Adierazlea ez da ziklo honetakoa.");
        return a;
    }
    @Transactional public void gordeErronkaLotura(Long zikloaId, Long adierazleaId, Long ieId, Long moduloaId, Long erronkaId, boolean landuta) {
        var a = ziklokoAdierazlea(zikloaId, adierazleaId);
        var e = erronka(erronkaId);
        require(Objects.equals(e.getZikloa().getId(), zikloaId), "Erronka ez da ziklo honetakoa.");
        var ie = ethazi.emaitza(ieId);
        var sourceModule = ethazi.moduloa(zikloaId, moduloaId);
        kinielaLotura(a, zikloaId, ieId, moduloaId);
        require(e.getModuluak().stream().anyMatch(m -> m.getId().equals(moduloaId)), "Erronkak ez du modulu honetan parte hartzen.");
        var versions = e.getBertsioTaldea() == null ? List.of(e) : erronkak.findByBertsioTaldea(e.getBertsioTaldea());
        for (var version : versions) for (var module : version.getModuluak()) {
            if (!Objects.equals(module.getEeiKodea(), sourceModule.getEeiKodea()) || !ethazi.dagokio(ie, module)) continue;
            var related = kinielaLotura(a, zikloaId, ieId, module.getId());
            if (landuta) related.getErronkak().add(version);
            else related.getErronkak().removeIf(item -> item.getId().equals(version.getId()));
        }
    }
    @Transactional public void gordeOharra(Long zikloaId, Long adierazleaId, Long ieId, Long moduloaId, String oharra) {
        var a = ziklokoAdierazlea(zikloaId, adierazleaId);
        require(oharra != null && oharra.length() <= 10000, "Oharrak gehienez 10000 karaktere izan ditzake.");
        kinielaLotura(a, zikloaId, ieId, moduloaId).setOharra(oharra.strip());
    }

    private KinielaLotura kinielaLotura(LorpenAdierazlea a, Long zikloaId, Long ieId, Long moduloaId) {
        var ie = ethazi.emaitza(ieId);
        var module = ethazi.moduloa(zikloaId, moduloaId);
        require(ethazi.dagokio(ie, module) && lotuta(a, ieId), "IEaren eta adierazlearen arteko lotura ez da baliozkoa.");
        return a.getKinielaLoturak().stream().filter(l -> l.getEmaitza().getId().equals(ieId) && l.getModuloa().getId().equals(moduloaId))
            .findFirst().orElseGet(() -> {
                var l = new KinielaLotura(); l.setAdierazlea(a); l.setEmaitza(ie); l.setModuloa(module);
                a.getKinielaLoturak().add(l); return l;
            });
    }
    private Set<Long> erronkaIds(LorpenAdierazlea a, Long ieId, Long zikloaId, Long moduloaId) {
        return a.getKinielaLoturak().stream().filter(l -> l.getEmaitza().getId().equals(ieId) && l.getModuloa().getId().equals(moduloaId))
            .flatMap(l -> l.getErronkak().stream()).filter(e -> e.getZikloa().getId().equals(zikloaId))
            .map(Erronka::getId).collect(java.util.stream.Collectors.toSet());
    }
    private String oharra(LorpenAdierazlea a, Long ieId, Long moduloaId) {
        return a.getKinielaLoturak().stream().filter(l -> l.getEmaitza().getId().equals(ieId) && l.getModuloa().getId().equals(moduloaId))
            .map(KinielaLotura::getOharra).filter(Objects::nonNull).findFirst().orElse("");
    }

    public record Lotura(Long id, String kodea, String deskribapena, BigDecimal pisua, Set<Long> erronkaIds, String oharra) {}
    public record Emaitza(Long id, String kodea, String deskribapena, List<Lotura> loturak, BigDecimal guztira) {}
    public record Modulua(Long id, String izena, String taldea, String hizkuntza, Hizkuntza hizkuntzaKodea,
            List<Emaitza> emaitzak, BigDecimal guztira, List<ErronkaZutabea> erronkak) {}
    public List<LorpenAdierazlea> adierazleak(Long zikloaId) {
        return ethazi.errubrikak(zikloaId).stream().flatMap(r -> r.lerroak().stream())
            .flatMap(r -> r.gelaxkak().stream()).filter(Objects::nonNull).flatMap(gm -> gm.getLorpenAdierazleak().stream()).toList();
    }
    public List<Modulua> kiniela(Long zikloaId) {
        return kiniela(zikloaId, null);
    }
    public List<Modulua> kiniela(Long zikloaId, Hizkuntza hizkuntza) {
        var indicators = adierazleak(zikloaId);
        var challenges = erronkaZutabeak(zikloaId);
        return ethazi.curriculum(zikloaId).stream()
            .filter(c -> hizkuntza == null || c.moduloa().getHizkuntza() == hizkuntza)
            .sorted(Comparator.comparing((EthaziService.ModuluEmaitzak c) -> c.moduloa().getKodea(),
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(c -> c.moduloa().getHizkuntza().ordinal())
                .thenComparing(c -> c.moduloa().getId()))
            .map(c -> {
            var results = c.emaitzak().stream().map(ie -> {
                var links = indicators.stream().filter(a -> lotuta(a, ie.getId())).map(a -> new Lotura(a.getId(),
                    a.getGaitasunMaila().getGaitasuna().getKodea() + "." + a.getGaitasunMaila().getMaila().getOrdena() + "." + a.getOrdena(),
                    a.deskribapena(c.moduloa().getHizkuntza()), pisua(a, ie.getId()),
                    erronkaIds(a, ie.getId(), zikloaId, c.moduloa().getId()), oharra(a, ie.getId(), c.moduloa().getId()))).toList();
                return new Emaitza(ie.getId(), ie.getKodea(), ie.deskribapena(c.moduloa().getHizkuntza()), links, links.stream().map(Lotura::pisua).reduce(BigDecimal.ZERO, BigDecimal::add));
            }).toList();
            var m = c.moduloa();
            var moduleLanguage = m.getHizkuntza() == Hizkuntza.ZEHAZTU_GABE ? Hizkuntza.EUSKARA : m.getHizkuntza();
            return new Modulua(m.getId(), m.getKodea() + " · " + m.getIzena(), m.getTaldea().getIzena(), m.getHizkuntza().getEtiketa(), m.getHizkuntza(), results,
                results.stream().map(Emaitza::guztira).reduce(BigDecimal.ZERO, BigDecimal::add),
                challenges.stream().filter(e -> e.hizkuntza() == moduleLanguage && e.moduloIds().contains(m.getId())).toList());
        }).toList();
    }
    private boolean lotuta(LorpenAdierazlea a, Long ieId) { return a.getIkaskuntzaEmaitzak().stream().anyMatch(ie -> ie.getId().equals(ieId)); }
    private BigDecimal pisua(LorpenAdierazlea a, Long ieId) {
        return a.getPisuak().entrySet().stream().filter(e -> e.getKey().getId().equals(ieId)).map(Map.Entry::getValue).findFirst().orElse(BigDecimal.ZERO);
    }
    @Transactional public void gordeLoturak(Long zikloaId, Long ieId, Set<Long> ids) {
        var ie = ethazi.emaitza(ieId);
        require(ethazi.ziklokoa(ie, zikloaId), "Ikaskuntza-emaitza ez da ziklo honetakoa.");
        var available = adierazleak(zikloaId);
        require(ids != null && available.stream().map(LorpenAdierazlea::getId).toList().containsAll(ids), "Adierazleak ez dira ziklo honetakoak.");
        for (var a : available) {
            if (ids.contains(a.getId())) { if (!lotuta(a, ieId)) a.getIkaskuntzaEmaitzak().add(ie); }
            else { a.getIkaskuntzaEmaitzak().removeIf(e -> e.getId().equals(ieId)); a.getPisuak().keySet().removeIf(e -> e.getId().equals(ieId));
                a.getKinielaLoturak().removeIf(l -> l.getEmaitza().getId().equals(ieId)); }
        }
    }
    @Transactional public void gordePisua(Long zikloaId, Long ieId, Long adierazleaId, BigDecimal pisua) {
        var ie = ethazi.emaitza(ieId);
        var a = adierazleak.findById(adierazleaId).orElseThrow(() -> new IllegalArgumentException("Adierazlea ez da aurkitu."));
        var gaitasuna = a.getGaitasunMaila().getGaitasuna();
        require(ethazi.ziklokoa(ie, zikloaId)
            && (gaitasuna.getZikloa() == null || gaitasuna.getZikloa().getId().equals(zikloaId)) && lotuta(a, ieId), "Lotura ez da baliozkoa.");
        require(pisua != null && pisua.signum() >= 0 && pisua.compareTo(new BigDecimal("100")) <= 0 && pisua.stripTrailingZeros().scale() <= 2,
            "Pisua 0 eta 100 artekoa izan behar da, gehienez bi hamartarrekin.");
        a.getPisuak().put(ie, pisua);
    }
    private static void require(boolean ok, String message) { if (!ok) throw new IllegalArgumentException(message); }
    private static String testua(String s, int max) { require(s != null && !s.isBlank() && s.strip().length() <= max, "Bete izena eta deskribapena, baimendutako luzerarekin."); return s.strip(); }
}
