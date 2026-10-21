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
    private final GaitasunaRepository gaitasunak;
    private final LorpenAdierazleaRepository adierazleak;
    private final ErronkaErrubrikaRepository errubrikak;
    private final ZeharkakoEbaluazioaRepository zeharkakoEbaluazioak;
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
        zeharkakoAdierazleak().forEach(a -> a.getZeharkakoKinielaLoturak().forEach(l ->
            l.getErronkak().removeIf(item -> item.getId().equals(id))));
        adierazleak.flush();
        zeharkakoEbaluazioak.deleteAll(zeharkakoEbaluazioak.findByErronkaId(id));
        zeharkakoEbaluazioak.flush();
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

    public record ErrubrikaIkaslea(Long id, String izena) {}

    public record ZeharkakoMaila(Long id, String izena, String deskribapena, int ordena) {}
    public record ZeharkakoIkasleNota(Long ikasleaId, Long mailaId, BigDecimal batezbestekoa, int ebaluatzaileak) {}
    public record ZeharkakoAdierazleErrubrika(Long id, String kodea, String deskribapena,
            List<ZeharkakoMaila> mailak, List<ZeharkakoIkasleNota> ikasleNotak) {}
    public record ZeharkakoGaitasunErrubrika(String kodea, String deskribapena,
            List<ZeharkakoAdierazleErrubrika> adierazleak) {}

    public List<ErrubrikaIkaslea> errubrikaIkasleak(Long erronkaId) {
        erronka(erronkaId);
        return erronkaTaldeKideak.findByErronkaId(erronkaId).stream()
            .map(ErronkaTaldeKidea::getIkaslea)
            .map(i -> new ErrubrikaIkaslea(i.getId(), i.getIzenOsoa()))
            .distinct().sorted(Comparator.comparing(ErrubrikaIkaslea::izena, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public List<ZeharkakoGaitasunErrubrika> zeharkakoErrubrika(Long erronkaId, Long moduloaId) {
        var challenge = erronka(erronkaId);
        var module = challenge.getModuluak().stream().filter(m -> m.getId().equals(moduloaId)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Modulua ez da erronka honetakoa."));
        var language = module.getHizkuntza() == Hizkuntza.ZEHAZTU_GABE ? Hizkuntza.EUSKARA : module.getHizkuntza();
        var students = errubrikaIkasleak(erronkaId);
        var evaluations = zeharkakoEbaluazioak.findByErronkaId(erronkaId);
        return gaitasunak.findByZikloaIsNullAndMotaOrderByKodeaAsc(GaitasunMota.ZEHARKAKOA).stream()
            .filter(g -> g.getEredua() != null)
            .map(g -> {
                var selected = g.getMailak().stream().flatMap(gm -> gm.getLorpenAdierazleak().stream())
                    .filter(a -> a.getZeharkakoKinielaLoturak().stream().anyMatch(l ->
                        l.getZikloa().getId().equals(challenge.getZikloa().getId())
                        && l.getErronkak().stream().anyMatch(e -> e.getId().equals(erronkaId))))
                    .map(a -> {
                        var levels = g.getEredua().getMailak().stream().map(level -> {
                            String description = g.getMailak().stream()
                                .filter(gm -> gm.getMaila().getId().equals(level.getId())).findFirst()
                                .map(gm -> gm.deskribapena(language)).orElse("");
                            return new ZeharkakoMaila(level.getId(), level.izena(language), description, level.getOrdena());
                        }).toList();
                        var grades = students.stream().map(student -> {
                            var all = evaluations.stream().filter(n -> n.getAdierazlea().getId().equals(a.getId())
                                && n.getIkaslea().getId().equals(student.id())).toList();
                            Long ownLevel = all.stream().filter(n -> n.getModuloa().getId().equals(moduloaId))
                                .map(n -> n.getMaila().getId()).findFirst().orElse(null);
                            BigDecimal average = all.isEmpty() ? null : all.stream().map(ZeharkakoEbaluazioa::getNota)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .divide(BigDecimal.valueOf(all.size()), 2, java.math.RoundingMode.HALF_UP);
                            return new ZeharkakoIkasleNota(student.id(), ownLevel, average, all.size());
                        }).toList();
                        return new ZeharkakoAdierazleErrubrika(a.getId(), adierazleKodea(a),
                            a.deskribapena(language), levels, grades);
                    }).toList();
                return new ZeharkakoGaitasunErrubrika(g.getKodea(), g.deskribapena(language), selected);
            }).filter(g -> !g.adierazleak().isEmpty()).toList();
    }

    @Transactional public Map<String, Object> gordeZeharkakoNota(Long erronkaId, Long moduloaId,
            Long adierazleaId, Long ikasleaId, Long mailaId, Authentication auth) {
        if (!erronkaKalifikatuDezake(auth, erronkaId, moduloaId))
            throw new AccessDeniedException("Ez duzu modulu honetako kalifikazioak aldatzeko baimenik.");
        var challenge = erronka(erronkaId);
        var module = challenge.getModuluak().stream().filter(m -> m.getId().equals(moduloaId)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Modulua ez da erronka honetakoa."));
        var student = erronkaTaldeKideak.findByErronkaId(erronkaId).stream()
            .filter(k -> k.getIkaslea().getId().equals(ikasleaId)).map(ErronkaTaldeKidea::getIkaslea).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Ikaslea ez da erronka honetakoa."));
        var indicator = adierazleak.findById(adierazleaId)
            .orElseThrow(() -> new IllegalArgumentException("Lorpen-adierazlea ez da aurkitu."));
        require(indicator.getZeharkakoKinielaLoturak().stream().anyMatch(l ->
            l.getZikloa().getId().equals(challenge.getZikloa().getId())
            && l.getErronkak().stream().anyMatch(e -> e.getId().equals(erronkaId))),
            "Lorpen-adierazlea ez da erronka honetarako hautatu.");
        var all = zeharkakoEbaluazioak.findByErronkaId(erronkaId);
        var existing = all.stream().filter(n -> n.getModuloa().getId().equals(moduloaId)
            && n.getAdierazlea().getId().equals(adierazleaId) && n.getIkaslea().getId().equals(ikasleaId)).findFirst();
        if (mailaId == null) existing.ifPresent(zeharkakoEbaluazioak::delete);
        else {
            var level = indicator.getGaitasunMaila().getGaitasuna().getEredua().getMailak().stream()
                .filter(m -> m.getId().equals(mailaId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Maila ez da zeharkako errubrika honetakoa."));
            var grade = existing.orElseGet(() -> {
                var n = new ZeharkakoEbaluazioa(); n.setErronka(challenge); n.setModuloa(module);
                n.setAdierazlea(indicator); n.setIkaslea(student); return n;
            });
            grade.setMaila(level); grade.setNota(BigDecimal.valueOf(level.getOrdena()));
            zeharkakoEbaluazioak.save(grade);
        }
        zeharkakoEbaluazioak.flush();
        var current = zeharkakoEbaluazioak.findByErronkaId(erronkaId).stream()
            .filter(n -> n.getAdierazlea().getId().equals(adierazleaId)
                && n.getIkaslea().getId().equals(ikasleaId)).toList();
        BigDecimal average = current.isEmpty() ? null : current.stream().map(ZeharkakoEbaluazioa::getNota)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(current.size()), 2, java.math.RoundingMode.HALF_UP);
        var result = new LinkedHashMap<String, Object>(); result.put("message", "Gordeta");
        result.put("average", average); result.put("evaluatorCount", current.size()); return result;
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
        for (var a : zeharkakoAdierazleak()) for (var l : a.getZeharkakoKinielaLoturak())
            if (l.getZikloa().getId().equals(target.getZikloa().getId())
                    && l.getErronkak().stream().anyMatch(e -> e.getId().equals(sourceId))) l.getErronkak().add(target);
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
        source.getMailak().size(); source.getEbidentziak().forEach(e -> { e.getMailak().size(); e.getLorpenAdierazleak().size(); e.getKinielaLoturak().size(); });
        Map<Long, ErronkaErrubrikaMaila> levelMap = new HashMap<>();
        for (var old : source.getMailak()) {
            var copy = new ErronkaErrubrikaMaila(); copy.setErrubrika(target); copy.setOrdena(old.getOrdena());
            copy.setIzena(old.getIzena()); copy.setBalioa(old.getBalioa()); target.getMailak().add(copy); entityManager.persist(copy);
            levelMap.put(old.getId(), copy);
        }
        var available = modulukoAdierazleEntitateak(target).stream().map(LorpenAdierazlea::getId)
            .collect(java.util.stream.Collectors.toSet());
        var targetLinks = modulukoKinielaLoturak(target);
        for (var old : source.getEbidentziak()) {
            var copy = new ErronkaEbidentzia(); copy.setErrubrika(target); copy.setOrdena(old.getOrdena());
            copy.setDeskribapena(old.getDeskribapena()); copy.setPisua(old.getPisua());
            copy.setEbaluazioMota(old.getEbaluazioMota());
            old.getLorpenAdierazleak().stream().filter(a -> available.contains(a.getId())).forEach(copy.getLorpenAdierazleak()::add);
            old.getKinielaLoturak().forEach(oldLink -> targetLinks.stream()
                .filter(link -> link.getAdierazlea().getId().equals(oldLink.getAdierazlea().getId())
                    && link.getEmaitza().getId().equals(oldLink.getEmaitza().getId()))
                .findFirst().ifPresent(copy.getKinielaLoturak()::add));
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
            eb.getDeskribapena(), eb.getPisua(), eb.getKinielaLoturak().isEmpty()
                ? eb.getLorpenAdierazleak().stream().map(this::adierazleKodea).sorted(String.CASE_INSENSITIVE_ORDER).toList()
                : eb.getKinielaLoturak().stream().map(l -> l.getEmaitza().getKodea() + " · " + adierazleKodea(l.getAdierazlea()))
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
        r.getEbidentziak().forEach(eb -> {
            eb.getMailak().size(); eb.getLorpenAdierazleak().size(); eb.getKinielaLoturak().size(); eb.getTaldeNotak().size(); eb.getIkasleNotak().size();
        });
        r.getTaldeOharrak().size(); r.getIkasleOharrak().size();
        r.getBestelakoProbak().forEach(p -> { p.getLorpenAdierazleak().size(); p.getKinielaLoturak().size(); p.getNotak().size(); });
        return r;
    }

    public Map<Long, String> taldeOrokorOharrak(Long erronkaId, Long moduloaId) {
        return errubrika(erronkaId, moduloaId).getTaldeOharrak().stream()
            .collect(java.util.stream.Collectors.toMap(o -> o.getTaldea().getId(), ErronkaErrubrikaTaldeOharra::getOharra));
    }

    public Map<Long, String> ikasleOrokorOharrak(Long erronkaId, Long moduloaId) {
        return errubrika(erronkaId, moduloaId).getIkasleOharrak().stream()
            .collect(java.util.stream.Collectors.toMap(o -> o.getIkaslea().getId(), ErronkaErrubrikaIkasleOharra::getOharra));
    }

    public ErronkaErrubrikaForm errubrikaForm(Long erronkaId, Long moduloaId) {
        var r = errubrika(erronkaId, moduloaId); var f = new ErronkaErrubrikaForm();
        f.setErrubrikaPisua(r.getErrubrikaPisua()); f.setBestelakoProbenPisua(r.getBestelakoProbenPisua());
        var teams = taldeak(erronkaId);
        var members = erronkaTaldeKideak.findByErronkaId(erronkaId);
        var students = errubrikaIkasleak(erronkaId);
        for (var m : r.getMailak()) {
            var mf = new com.koadernoa.app.ethazi.dto.EthaziForms.ErronkaMailaForm();
            mf.setId(m.getId()); mf.setIzena(m.getIzena()); mf.setBalioa(m.getBalioa()); f.getMailak().add(mf);
        }
        for (var eb : r.getEbidentziak()) {
            var ef = new com.koadernoa.app.ethazi.dto.EthaziForms.ErronkaEbidentziaForm();
            ef.setId(eb.getId()); ef.setDeskribapena(eb.getDeskribapena()); ef.setPisua(eb.getPisua());
            ef.setEbaluazioMota(eb.getEbaluazioMota() == null ? EbidentziaEbaluazioMota.TALDEKAKOA : eb.getEbaluazioMota());
            eb.getKinielaLoturak().forEach(l -> ef.getAdierazleaIds().add(l.getId()));
            eb.getLorpenAdierazleak().stream()
                .filter(a -> eb.getKinielaLoturak().stream().noneMatch(l -> l.getAdierazlea().getId().equals(a.getId())))
                .forEach(a -> ef.getAdierazleaIds().add(-a.getId()));
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
            for (var student : students) {
                var sf = new com.koadernoa.app.ethazi.dto.EthaziForms.ErronkaIkasleNotaForm(); sf.setIkasleaId(student.id());
                if (eb.getEbaluazioMota() == EbidentziaEbaluazioMota.TALDEKAKOA) {
                    members.stream()
                        .filter(k -> k.getIkaslea().getId().equals(student.id())).findFirst()
                        .flatMap(k -> eb.getTaldeNotak().stream()
                            .filter(n -> n.getTaldea().getId().equals(k.getTaldea().getId())).findFirst())
                        .ifPresent(n -> {
                            if (n.getMaila() != null) { sf.setMailaId(n.getMaila().getId()); sf.setMailaIzena(n.getMaila().getIzena()); }
                            sf.setOndoEgindakoak(n.getOndoEgindakoak());
                            sf.setHobetuBeharrekoak(n.getHobetuBeharrekoak());
                        });
                } else eb.getIkasleNotak().stream().filter(n -> n.getIkaslea().getId().equals(student.id())).findFirst()
                    .ifPresent(n -> {
                        if (n.getMaila() != null) { sf.setMailaId(n.getMaila().getId()); sf.setMailaIzena(n.getMaila().getIzena()); }
                        sf.setOndoEgindakoak(n.getOndoEgindakoak());
                        sf.setHobetuBeharrekoak(n.getHobetuBeharrekoak());
                    });
                ef.getIkasleNotak().add(sf);
            }
            f.getEbidentziak().add(ef);
        }
        return f;
    }

    @Transactional public void gordeErrubrika(Long erronkaId, Long moduloaId, ErronkaErrubrikaForm f) {
        var r = errubrika(erronkaId, moduloaId);
        if (f.getErrubrikaPisua() == null) f.setErrubrikaPisua(r.getErrubrikaPisua());
        if (f.getBestelakoProbenPisua() == null) f.setBestelakoProbenPisua(r.getBestelakoProbenPisua());
        egiaztatuEbaluazioPisuak(r, f.getErrubrikaPisua(), f.getBestelakoProbenPisua());
        r.setErrubrikaPisua(f.getErrubrikaPisua()); r.setBestelakoProbenPisua(f.getBestelakoProbenPisua());
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
            require(ef.getEbaluazioMota() != null, "Ebidentziaren ebaluazio mota aukeratu behar da.");
            eb.setEbaluazioMota(ef.getEbaluazioMota());
            // Bertsio zaharreko bezero/probetan adierazlearen IDa bidaltzen zen.
            // Lotura bakarra bada, atzeranzko bateragarritasunez onartzen da.
            var requested = ef.getAdierazleaIds() == null ? java.util.Set.<Long>of() : new java.util.LinkedHashSet<>(ef.getAdierazleaIds());
            var technicalLinks = modulukoKinielaLoturak(r);
            var resolved = new java.util.LinkedHashSet<Long>();
            for (Long id : requested) {
                if (availableIndicators.contains(id)) { resolved.add(id); continue; }
                var matches = technicalLinks.stream().filter(l -> l.getAdierazlea().getId().equals(id)).toList();
                if (matches.size() == 1) resolved.add(matches.get(0).getId());
                else resolved.add(id);
            }
            require(availableIndicators.containsAll(resolved),
                "Hautatutako lorpen-adierazleren bat ez da modulu honetakoa.");
            eb.getKinielaLoturak().removeIf(l -> !resolved.contains(l.getId()));
            var selectedLinkIds = eb.getKinielaLoturak().stream().map(KinielaLotura::getId).collect(java.util.stream.Collectors.toSet());
            technicalLinks.stream().filter(l -> resolved.contains(l.getId()) && !selectedLinkIds.contains(l.getId()))
                .forEach(eb.getKinielaLoturak()::add);
            var transversalIds = resolved.stream().filter(id -> id < 0).map(id -> -id).collect(java.util.stream.Collectors.toSet());
            var technicalIndicatorIds = eb.getKinielaLoturak().stream().map(l -> l.getAdierazlea().getId()).collect(java.util.stream.Collectors.toSet());
            eb.getLorpenAdierazleak().removeIf(a -> !technicalIndicatorIds.contains(a.getId()) && !transversalIds.contains(a.getId()));
            modulukoAdierazleEntitateak(r).stream().filter(a -> (technicalIndicatorIds.contains(a.getId()) || transversalIds.contains(a.getId()))
                && eb.getLorpenAdierazleak().stream().noneMatch(existing -> existing.getId().equals(a.getId())))
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
        r.getEbidentziak().stream().flatMap(eb -> eb.getIkasleNotak().stream())
            .filter(n -> n.getMaila() != null).forEach(n -> n.setNota(n.getMaila().getBalioa()));
        errubrikak.flush();
    }

    @Transactional public void gordeEbaluazioPisuak(Long erronkaId, Long moduloaId,
            BigDecimal errubrikaPisua, BigDecimal bestelakoProbenPisua) {
        var r = errubrika(erronkaId, moduloaId);
        egiaztatuEbaluazioPisuak(r, errubrikaPisua, bestelakoProbenPisua);
        r.setErrubrikaPisua(errubrikaPisua); r.setBestelakoProbenPisua(bestelakoProbenPisua);
        errubrikak.flush();
    }

    private void egiaztatuEbaluazioPisuak(ErronkaErrubrika r, BigDecimal rubricWeight, BigDecimal testsWeight) {
        require(rubricWeight != null && testsWeight != null && ehunekoa(rubricWeight) && ehunekoa(testsWeight),
            "Errubrikaren eta bestelako proben pisuak 0 eta 100 artekoak izan behar dira.");
        require(rubricWeight.add(testsWeight).compareTo(new BigDecimal("100")) == 0,
            "Errubrikaren eta bestelako proben pisuen baturak %100 izan behar du.");
        require(!r.getBestelakoProbak().isEmpty() || testsWeight.compareTo(BigDecimal.ZERO) == 0,
            "Bestelako probarik ez badago, errubrikaren pisuak %100 izan behar du.");
    }

    private static boolean ehunekoa(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) >= 0 && value.compareTo(new BigDecimal("100")) <= 0
            && value.stripTrailingZeros().scale() <= 2;
    }

    public com.koadernoa.app.ethazi.dto.EthaziForms.BestelakoProbakForm bestelakoProbakForm(Long erronkaId, Long moduloaId) {
        var r = errubrika(erronkaId, moduloaId);
        var students = errubrikaIkasleak(erronkaId);
        var f = new com.koadernoa.app.ethazi.dto.EthaziForms.BestelakoProbakForm();
        for (var p : r.getBestelakoProbak()) {
            var pf = new com.koadernoa.app.ethazi.dto.EthaziForms.BestelakoProbaForm();
            pf.setId(p.getId()); pf.setIzena(p.getIzena()); pf.setData(p.getData()); pf.setPisua(p.getPisua());
            p.getKinielaLoturak().forEach(l -> pf.getAdierazleaIds().add(l.getId()));
            p.getLorpenAdierazleak().stream()
                .filter(a -> p.getKinielaLoturak().stream().noneMatch(l -> l.getAdierazlea().getId().equals(a.getId())))
                .forEach(a -> pf.getAdierazleaIds().add(-a.getId()));
            for (var student : students) {
                var nf = new com.koadernoa.app.ethazi.dto.EthaziForms.BestelakoProbaNotaForm();
                nf.setIkasleaId(student.id()); nf.setIkasleaIzena(student.izena());
                p.getNotak().stream().filter(n -> n.getIkaslea().getId().equals(student.id())).findFirst().ifPresent(n -> nf.setNota(n.getNota()));
                pf.getNotak().add(nf);
            }
            f.getProbak().add(pf);
        }
        return f;
    }

    @Transactional public Long gehituBestelakoProba(Long erronkaId, Long moduloaId, String izena,
            java.time.LocalDate data) {
        var r = errubrika(erronkaId, moduloaId);
        require(data != null, "Probaren data derrigorrezkoa da.");
        var p = new ErronkaBestelakoProba(); p.setErrubrika(r); p.setIzena(testua(izena, 200));
        p.setData(data); p.setPisua(BigDecimal.ZERO); r.getBestelakoProbak().add(p);
        if (r.getBestelakoProbak().size() == 1) {
            r.setErrubrikaPisua(new BigDecimal("50.00")); r.setBestelakoProbenPisua(new BigDecimal("50.00"));
        }
        banatuProbenPisuak(r.getBestelakoProbak());
        errubrikak.flush(); return p.getId();
    }

    @Transactional public void ezabatuBestelakoProba(Long erronkaId, Long moduloaId, Long probaId) {
        var r = errubrika(erronkaId, moduloaId);
        var proba = r.getBestelakoProbak().stream().filter(p -> p.getId().equals(probaId)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Bestelako proba ez da aurkitu."));
        require(proba.getNotak().isEmpty(), "Ezin da proba ezabatu: ikasleren batek kalifikazioa dauka.");
        r.getBestelakoProbak().remove(proba);
        if (r.getBestelakoProbak().isEmpty()) {
            r.setErrubrikaPisua(new BigDecimal("100.00")); r.setBestelakoProbenPisua(BigDecimal.ZERO);
        } else banatuProbenPisuak(r.getBestelakoProbak());
        errubrikak.flush();
    }

    private void banatuProbenPisuak(List<ErronkaBestelakoProba> tests) {
        if (tests.isEmpty()) return;
        BigDecimal each = new BigDecimal("100").divide(BigDecimal.valueOf(tests.size()), 2, java.math.RoundingMode.DOWN);
        for (int i = 0; i < tests.size(); i++) tests.get(i).setPisua(i == tests.size() - 1
            ? new BigDecimal("100.00").subtract(each.multiply(BigDecimal.valueOf(tests.size() - 1))) : each);
    }

    @Transactional public void gordeBestelakoProbak(Long erronkaId, Long moduloaId,
            com.koadernoa.app.ethazi.dto.EthaziForms.BestelakoProbakForm f) {
        var r = errubrika(erronkaId, moduloaId);
        var rows = f.getProbak() == null ? List.<com.koadernoa.app.ethazi.dto.EthaziForms.BestelakoProbaForm>of() : f.getProbak();
        var ids = r.getBestelakoProbak().stream().map(ErronkaBestelakoProba::getId).collect(java.util.stream.Collectors.toSet());
        require(rows.size() == ids.size() && rows.stream().map(x -> x.getId()).collect(java.util.stream.Collectors.toSet()).equals(ids),
            "Bestelako proben zerrenda aldatu da. Kargatu berriro.");
        require(rows.isEmpty() || rows.stream().allMatch(x -> x.getPisua() != null && ehunekoa(x.getPisua()))
            && rows.stream().map(com.koadernoa.app.ethazi.dto.EthaziForms.BestelakoProbaForm::getPisua)
                .reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(new BigDecimal("100")) == 0,
            "Bestelako proben barruko pisuen baturak %100 izan behar du.");
        var available = modulukoAdierazleEntitateak(r);
        var availableLinks = modulukoKinielaLoturak(r);
        var availableIds = modulukoAdierazleak(r).stream().map(ErrubrikaAdierazlea::id)
            .collect(java.util.stream.Collectors.toSet());
        for (var row : rows) {
            var p = r.getBestelakoProbak().stream().filter(x -> x.getId().equals(row.getId())).findFirst().orElseThrow();
            p.setIzena(testua(row.getIzena(), 200)); require(row.getData() != null, "Probaren data derrigorrezkoa da.");
            p.setData(row.getData()); p.setPisua(row.getPisua());
            var requested = row.getAdierazleaIds() == null ? java.util.Set.<Long>of() : new java.util.LinkedHashSet<>(row.getAdierazleaIds());
            var resolved = new java.util.LinkedHashSet<Long>();
            for (Long id : requested) {
                if (availableIds.contains(id)) { resolved.add(id); continue; }
                var matches = availableLinks.stream().filter(l -> l.getAdierazlea().getId().equals(id)).toList();
                if (matches.size() == 1) resolved.add(matches.get(0).getId());
                else resolved.add(id);
            }
            require(availableIds.containsAll(resolved),
                "Proban hautatutako lorpen-adierazleren bat ez da modulu honetakoa.");
            p.getKinielaLoturak().removeIf(l -> !resolved.contains(l.getId()));
            var selectedLinks = p.getKinielaLoturak().stream().map(KinielaLotura::getId).collect(java.util.stream.Collectors.toSet());
            availableLinks.stream().filter(l -> resolved.contains(l.getId()) && !selectedLinks.contains(l.getId()))
                .forEach(p.getKinielaLoturak()::add);
            var transversalIds = resolved.stream().filter(id -> id < 0).map(id -> -id).collect(java.util.stream.Collectors.toSet());
            var technicalIndicatorIds = p.getKinielaLoturak().stream().map(l -> l.getAdierazlea().getId()).collect(java.util.stream.Collectors.toSet());
            p.getLorpenAdierazleak().removeIf(a -> !technicalIndicatorIds.contains(a.getId()) && !transversalIds.contains(a.getId()));
            available.stream().filter(a -> (technicalIndicatorIds.contains(a.getId()) || transversalIds.contains(a.getId()))
                && p.getLorpenAdierazleak().stream().noneMatch(existing -> existing.getId().equals(a.getId())))
                .forEach(p.getLorpenAdierazleak()::add);
        }
        errubrikak.flush();
    }

    @Transactional public void gordeBestelakoProbaNota(Long erronkaId, Long moduloaId, Long probaId,
            Long ikasleaId, BigDecimal nota, Authentication auth) {
        if (!erronkaKalifikatuDezake(auth, erronkaId, moduloaId))
            throw new AccessDeniedException("Ez duzu modulu honetako kalifikazioak aldatzeko baimenik.");
        var r = errubrika(erronkaId, moduloaId);
        var p = r.getBestelakoProbak().stream().filter(x -> x.getId().equals(probaId)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Bestelako proba ez da errubrika honetakoa."));
        var student = erronkaTaldeKideak.findByErronkaId(erronkaId).stream()
            .filter(k -> k.getIkaslea().getId().equals(ikasleaId)).map(ErronkaTaldeKidea::getIkaslea).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Ikaslea ez da erronka honetakoa."));
        var existing = p.getNotak().stream().filter(n -> n.getIkaslea().getId().equals(ikasleaId)).findFirst();
        if (nota == null) existing.ifPresent(p.getNotak()::remove);
        else {
            require(nota.compareTo(BigDecimal.ZERO) >= 0 && nota.compareTo(BigDecimal.TEN) <= 0
                && nota.stripTrailingZeros().scale() <= 2, "Nota 0 eta 10 artekoa izan behar da.");
            existing.orElseGet(() -> { var n = new ErronkaBestelakoProbaNota(); n.setProba(p); n.setIkaslea(student); p.getNotak().add(n); return n; }).setNota(nota);
        }
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
        require(eb.getEbaluazioMota() == EbidentziaEbaluazioMota.TALDEKAKOA,
            "Ebidentzia hau pertsonalki kalifikatzen da.");
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
        require(eb.getEbaluazioMota() == EbidentziaEbaluazioMota.TALDEKAKOA,
            "Ebidentzia hau pertsonalki kalifikatzen da.");
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

    @Transactional public List<Long> gordeIkasleNota(Long erronkaId, Long moduloaId, Long ebidentziaId,
            Long ikasleaId, Long mailaId, Authentication auth) {
        if (!erronkaKalifikatuDezake(auth, erronkaId, moduloaId))
            throw new AccessDeniedException("Ez duzu modulu honetako kalifikazioak aldatzeko baimenik.");
        var r = errubrika(erronkaId, moduloaId);
        var eb = r.getEbidentziak().stream().filter(x -> x.getId().equals(ebidentziaId)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Ebidentzia ez da errubrika honetakoa."));
        var member = erronkaTaldeKideak.findByErronkaId(erronkaId).stream()
            .filter(k -> k.getIkaslea().getId().equals(ikasleaId)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Ikaslea ez da erronka honetakoa."));
        if (eb.getEbaluazioMota() == EbidentziaEbaluazioMota.TALDEKAKOA) {
            gordeTaldeNotaBalioa(r, eb, member.getTaldea(), mailaId);
            errubrikak.flush();
            return member.getTaldea().getKideak().stream().map(k -> k.getIkaslea().getId()).toList();
        }
        var existing = eb.getIkasleNotak().stream().filter(n -> n.getIkaslea().getId().equals(ikasleaId)).findFirst();
        if (mailaId == null) {
            existing.ifPresent(note -> { note.setMaila(null); note.setNota(null); if (feedbackHutsik(note)) eb.getIkasleNotak().remove(note); });
        } else {
            var level = r.getMailak().stream().filter(m -> m.getId().equals(mailaId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Hautatutako maila ez da errubrika honetakoa."));
            var note = existing.orElseGet(() -> {
                var n = new ErronkaEbidentziaIkasleNota(); n.setEbidentzia(eb); n.setIkaslea(member.getIkaslea());
                eb.getIkasleNotak().add(n); return n;
            });
            note.setMaila(level); note.setNota(level.getBalioa());
        }
        errubrikak.flush();
        return List.of(ikasleaId);
    }

    private void gordeTaldeNotaBalioa(ErronkaErrubrika r, ErronkaEbidentzia eb, ErronkaTaldea team, Long mailaId) {
        var existing = eb.getTaldeNotak().stream().filter(n -> n.getTaldea().getId().equals(team.getId())).findFirst();
        if (mailaId == null) {
            existing.ifPresent(note -> {
                note.setMaila(null); note.setNota(null);
                if (feedbackHutsik(note)) eb.getTaldeNotak().remove(note);
            });
            return;
        }
        var level = r.getMailak().stream().filter(m -> m.getId().equals(mailaId)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Hautatutako maila ez da errubrika honetakoa."));
        var note = existing.orElseGet(() -> {
            var n = new ErronkaEbidentziaTaldeNota(); n.setEbidentzia(eb); n.setTaldea(team);
            eb.getTaldeNotak().add(n); return n;
        });
        note.setMaila(level); note.setNota(level.getBalioa());
    }

    @Transactional public List<Long> gordeIkasleOharrak(Long erronkaId, Long moduloaId, Long ebidentziaId,
            Long ikasleaId, String ondoEgindakoak, String hobetuBeharrekoak, Authentication auth) {
        if (!erronkaKalifikatuDezake(auth, erronkaId, moduloaId))
            throw new AccessDeniedException("Ez duzu modulu honetako kalifikazioak aldatzeko baimenik.");
        var r = errubrika(erronkaId, moduloaId);
        var eb = r.getEbidentziak().stream().filter(x -> x.getId().equals(ebidentziaId)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Ebidentzia ez da errubrika honetakoa."));
        var member = erronkaTaldeKideak.findByErronkaId(erronkaId).stream()
            .filter(k -> k.getIkaslea().getId().equals(ikasleaId)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Ikaslea ez da erronka honetakoa."));
        if (eb.getEbaluazioMota() == EbidentziaEbaluazioMota.TALDEKAKOA) {
            gordeTaldeOharrakBalioa(eb, member.getTaldea(), ondoEgindakoak, hobetuBeharrekoak);
            errubrikak.flush();
            return member.getTaldea().getKideak().stream().map(k -> k.getIkaslea().getId()).toList();
        }
        var student = member.getIkaslea();
        String strengths = aukerakoTestua(ondoEgindakoak, 60000), improvements = aukerakoTestua(hobetuBeharrekoak, 60000);
        var existing = eb.getIkasleNotak().stream().filter(n -> n.getIkaslea().getId().equals(ikasleaId)).findFirst();
        if (strengths.isEmpty() && improvements.isEmpty() && existing.isPresent()
                && existing.get().getMaila() == null && existing.get().getNota() == null) eb.getIkasleNotak().remove(existing.get());
        else if (!strengths.isEmpty() || !improvements.isEmpty() || existing.isPresent()) {
            var note = existing.orElseGet(() -> {
                var n = new ErronkaEbidentziaIkasleNota(); n.setEbidentzia(eb); n.setIkaslea(student);
                eb.getIkasleNotak().add(n); return n;
            });
            note.setOndoEgindakoak(strengths); note.setHobetuBeharrekoak(improvements);
        }
        errubrikak.flush();
        return List.of(ikasleaId);
    }

    private void gordeTaldeOharrakBalioa(ErronkaEbidentzia eb, ErronkaTaldea team,
            String ondoEgindakoak, String hobetuBeharrekoak) {
        String strengths = aukerakoTestua(ondoEgindakoak, 60000);
        String improvements = aukerakoTestua(hobetuBeharrekoak, 60000);
        var existing = eb.getTaldeNotak().stream().filter(n -> n.getTaldea().getId().equals(team.getId())).findFirst();
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
    }

    @Transactional public void gordeOharOrokorra(Long erronkaId, Long moduloaId, Long taldeaId,
            Long ikasleaId, String oharra, Authentication auth) {
        if (!erronkaKalifikatuDezake(auth, erronkaId, moduloaId))
            throw new AccessDeniedException("Ez duzu modulu honetako kalifikazioak aldatzeko baimenik.");
        require((taldeaId == null) != (ikasleaId == null), "Taldea edo ikaslea adierazi behar da.");
        var r = errubrika(erronkaId, moduloaId);
        String text = aukerakoTestua(oharra, 60000);
        if (taldeaId != null) {
            var team = erronkaTaldeak.findById(taldeaId).orElseThrow(() -> new IllegalArgumentException("Taldea ez da aurkitu."));
            require(team.getErronka().getId().equals(erronkaId), "Taldea ez da erronka honetakoa.");
            var existing = r.getTaldeOharrak().stream().filter(o -> o.getTaldea().getId().equals(taldeaId)).findFirst();
            if (text.isEmpty()) existing.ifPresent(r.getTaldeOharrak()::remove);
            else existing.orElseGet(() -> { var o = new ErronkaErrubrikaTaldeOharra(); o.setErrubrika(r); o.setTaldea(team); r.getTaldeOharrak().add(o); return o; }).setOharra(text);
        } else {
            var student = erronkaTaldeKideak.findByErronkaId(erronkaId).stream()
                .filter(k -> k.getIkaslea().getId().equals(ikasleaId)).map(ErronkaTaldeKidea::getIkaslea).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ikaslea ez da erronka honetakoa."));
            var existing = r.getIkasleOharrak().stream().filter(o -> o.getIkaslea().getId().equals(ikasleaId)).findFirst();
            if (text.isEmpty()) existing.ifPresent(r.getIkasleOharrak()::remove);
            else existing.orElseGet(() -> { var o = new ErronkaErrubrikaIkasleOharra(); o.setErrubrika(r); o.setIkaslea(student); r.getIkasleOharrak().add(o); return o; }).setOharra(text);
        }
        errubrikak.flush();
    }

    private static boolean feedbackHutsik(ErronkaEbidentziaTaldeNota note) {
        return (note.getOndoEgindakoak() == null || note.getOndoEgindakoak().isBlank())
            && (note.getHobetuBeharrekoak() == null || note.getHobetuBeharrekoak().isBlank());
    }

    private static boolean feedbackHutsik(ErronkaEbidentziaIkasleNota note) {
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
        kalifikazioetan = kalifikazioetan || r.getEbidentziak().stream().flatMap(eb -> eb.getIkasleNotak().stream())
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

    public record ErrubrikaAdierazlea(Long id, String kodea, String deskribapena, String emaitzaKodea,
            String oharra, boolean erabilita) {}

    private List<KinielaLotura> modulukoKinielaLoturak(ErronkaErrubrika r) {
        return adierazleGuztiak(r.getErronka().getZikloa().getId()).stream()
            .flatMap(a -> a.getKinielaLoturak().stream())
            .filter(l -> l.getModuloa().getId().equals(r.getModuloa().getId())
                && l.getErronkak().stream().anyMatch(e -> e.getId().equals(r.getErronka().getId())))
            .sorted(Comparator.comparing((KinielaLotura l) -> l.getEmaitza().getOrdena())
                .thenComparing(l -> adierazleKodea(l.getAdierazlea()), String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private List<LorpenAdierazlea> modulukoAdierazleEntitateak(ErronkaErrubrika r) {
        return adierazleGuztiak(r.getErronka().getZikloa().getId()).stream()
            .filter(a -> a.getKinielaLoturak().stream().anyMatch(l -> l.getModuloa().getId().equals(r.getModuloa().getId())
                && l.getErronkak().stream().anyMatch(e -> e.getId().equals(r.getErronka().getId())))
                || a.getZeharkakoKinielaLoturak().stream().anyMatch(l -> l.getZikloa().getId().equals(r.getErronka().getZikloa().getId())
                    && l.getErronkak().stream().anyMatch(e -> e.getId().equals(r.getErronka().getId()))))
            .sorted(Comparator.comparing(this::adierazleKodea, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public List<ErrubrikaAdierazlea> modulukoAdierazleak(Long erronkaId, Long moduloaId) {
        return modulukoAdierazleak(errubrika(erronkaId, moduloaId));
    }

    private List<ErrubrikaAdierazlea> modulukoAdierazleak(ErronkaErrubrika r) {
        var usedLinks = r.getEbidentziak().stream().flatMap(eb -> eb.getKinielaLoturak().stream())
            .map(KinielaLotura::getId).collect(java.util.stream.Collectors.toSet());
        var usedLegacy = r.getEbidentziak().stream().flatMap(eb -> eb.getLorpenAdierazleak().stream())
            .map(LorpenAdierazlea::getId).collect(java.util.stream.Collectors.toSet());
        Hizkuntza language = r.getModuloa().getHizkuntza() == Hizkuntza.ZEHAZTU_GABE ? r.getErronka().getHizkuntza() : r.getModuloa().getHizkuntza();
        if (language == Hizkuntza.ZEHAZTU_GABE) language = Hizkuntza.EUSKARA;
        final Hizkuntza displayLanguage = language;
        var result = new java.util.ArrayList<ErrubrikaAdierazlea>();
        modulukoKinielaLoturak(r).forEach(l -> result.add(new ErrubrikaAdierazlea(l.getId(),
            adierazleKodea(l.getAdierazlea()), l.getAdierazlea().deskribapena(displayLanguage),
            l.getEmaitza().getKodea(), l.getOharra(), usedLinks.contains(l.getId()))));
        // Zeharkako loturek ez dute ikaskuntza-emaitzarik; ID negatiboek bi
        // lotura moten arteko talkak saihesten dituzte formularioan.
        modulukoAdierazleEntitateak(r).stream()
            .filter(a -> a.getZeharkakoKinielaLoturak().stream().anyMatch(l -> l.getZikloa().getId().equals(r.getErronka().getZikloa().getId())
                && l.getErronkak().stream().anyMatch(e -> e.getId().equals(r.getErronka().getId()))))
            .forEach(a -> result.add(new ErrubrikaAdierazlea(-a.getId(), adierazleKodea(a),
                a.deskribapena(displayLanguage), null,
                a.getZeharkakoKinielaLoturak().stream().filter(l -> l.getZikloa().getId().equals(r.getErronka().getZikloa().getId()))
                    .map(ZeharkakoKinielaLotura::getOharra).findFirst().orElse(""), usedLegacy.contains(a.getId()))));
        return result;
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

    public record Ponderazioa(String mota, Long elementuaId, String izena, BigDecimal pisua) {}
    public record Lotura(Long id, String kodea, String deskribapena, BigDecimal pisua, Set<Long> erronkaIds,
            String oharra, List<Ponderazioa> ponderazioak) {}
    public record Emaitza(Long id, String kodea, String deskribapena, List<Lotura> loturak, BigDecimal guztira) {}
    public record Modulua(Long id, String izena, String taldea, String hizkuntza, Hizkuntza hizkuntzaKodea,
            List<Emaitza> emaitzak, BigDecimal guztira, List<ErronkaZutabea> erronkak, boolean ponderazioaErakutsi) {}
    public List<LorpenAdierazlea> adierazleak(Long zikloaId) {
        return errubrikakoAdierazleak(List.of(ethazi.errubrika(zikloaId, GaitasunMota.TEKNIKOA)));
    }
    public List<LorpenAdierazlea> zeharkakoAdierazleak() {
        return errubrikakoAdierazleak(ethazi.zeharkakoErrubrikak());
    }
    private List<LorpenAdierazlea> adierazleGuztiak(Long zikloaId) {
        var result = new ArrayList<LorpenAdierazlea>(adierazleak(zikloaId));
        result.addAll(zeharkakoAdierazleak());
        return result;
    }
    private List<LorpenAdierazlea> errubrikakoAdierazleak(List<EthaziService.Errubrika> rubrics) {
        return rubrics.stream().filter(r -> r.eredua() != null).flatMap(r -> r.lerroak().stream())
            .flatMap(r -> r.gelaxkak().stream()).filter(Objects::nonNull).flatMap(gm -> gm.getLorpenAdierazleak().stream()).toList();
    }
    public List<Modulua> kiniela(Long zikloaId) {
        return kiniela(zikloaId, null);
    }
    @Transactional
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
                var links = indicators.stream().filter(a -> lotuta(a, ie.getId())).map(a -> {
                    var link = a.getKinielaLoturak().stream().filter(l -> l.getEmaitza().getId().equals(ie.getId())
                        && l.getModuloa().getId().equals(c.moduloa().getId())).findFirst();
                    return new Lotura(a.getId(),
                        a.getGaitasunMaila().getGaitasuna().getKodea() + "." + a.getGaitasunMaila().getMaila().getOrdena() + "." + a.getOrdena(),
                        a.deskribapena(c.moduloa().getHizkuntza()), pisua(a, ie.getId()),
                        erronkaIds(a, ie.getId(), zikloaId, c.moduloa().getId()), oharra(a, ie.getId(), c.moduloa().getId()),
                        link.map(this::sinkronizatuPonderazioak).orElseGet(List::of));
                }).toList();
                return new Emaitza(ie.getId(), ie.getKodea(), ie.deskribapena(c.moduloa().getHizkuntza()), links, links.stream().map(Lotura::pisua).reduce(BigDecimal.ZERO, BigDecimal::add));
            }).toList();
            var m = c.moduloa();
            var moduleLanguage = m.getHizkuntza() == Hizkuntza.ZEHAZTU_GABE ? Hizkuntza.EUSKARA : m.getHizkuntza();
            return new Modulua(m.getId(), m.getKodea() + " · " + m.getIzena(), m.getTaldea().getIzena(), m.getHizkuntza().getEtiketa(), m.getHizkuntza(), results,
                results.stream().map(Emaitza::guztira).reduce(BigDecimal.ZERO, BigDecimal::add),
                challenges.stream().filter(e -> e.hizkuntza() == moduleLanguage && e.moduloIds().contains(m.getId())).toList(),
                results.stream().flatMap(e -> e.loturak().stream()).anyMatch(l -> l.ponderazioak().size() > 1));
        }).toList();
    }

    /** Zeharkako Kiniela ziklo-mailako taula bakarra da; ez dago modulu bati lotuta. */
    public List<Modulua> zeharkakoKiniela(Long zikloaId, Hizkuntza hizkuntza) {
        if (zikloaId == null) return List.of();
        var cycle = zikloak.findById(zikloaId).orElseThrow(() -> new IllegalArgumentException("Zikloa ez da aurkitu."));
        var competencies = gaitasunak.findByZikloaIsNullAndMotaOrderByKodeaAsc(GaitasunMota.ZEHARKAKOA);
        var language = hizkuntza == null || hizkuntza == Hizkuntza.ZEHAZTU_GABE ? Hizkuntza.EUSKARA : hizkuntza;
        var rows = competencies.stream().map(g -> {
                    var links = g.getMailak().stream().flatMap(gm -> gm.getLorpenAdierazleak().stream())
                        .filter(a -> zeharkakoLotura(a, g.getId(), zikloaId).isPresent())
                        .map(a -> {
                            var l = zeharkakoLotura(a, g.getId(), zikloaId).orElseThrow();
                            return new Lotura(a.getId(), adierazleKodea(a), a.deskribapena(language),
                                l.getPisua() == null ? BigDecimal.ZERO : l.getPisua(),
                                l.getErronkak().stream().map(Erronka::getId).collect(java.util.stream.Collectors.toSet()),
                                l.getOharra() == null ? "" : l.getOharra(), List.of());
                        }).toList();
                    return new Emaitza(g.getId(), g.getKodea(), g.deskribapena(language), links,
                        links.stream().map(Lotura::pisua).reduce(BigDecimal.ZERO, BigDecimal::add));
                }).toList();
        var challenges = erronkaZutabeak(zikloaId).stream()
            .filter(e -> hizkuntza == null || e.hizkuntza() == hizkuntza).toList();
        return List.of(new Modulua(zikloaId, cycle.getIzena(), "", "", language, rows,
            rows.stream().map(Emaitza::guztira).reduce(BigDecimal.ZERO, BigDecimal::add), challenges, false));
    }

    private List<Ponderazioa> sinkronizatuPonderazioak(KinielaLotura link) {
        var evidences = new ArrayList<ErronkaEbidentzia>();
        var tests = new ArrayList<ErronkaBestelakoProba>();
        for (var challenge : link.getErronkak()) {
            errubrikak.findByErronkaIdAndModuloaId(challenge.getId(), link.getModuloa().getId()).ifPresent(r -> {
                r.getEbidentziak().stream().filter(e -> e.getKinielaLoturak().stream().anyMatch(l -> l.getId().equals(link.getId())))
                    .forEach(evidences::add);
                r.getBestelakoProbak().stream().filter(p -> p.getKinielaLoturak().stream().anyMatch(l -> l.getId().equals(link.getId())))
                    .forEach(tests::add);
            });
        }
        boolean changed = !idMultzoa(link.getEbidentziaPonderazioak().keySet()).equals(idMultzoa(evidences))
            || !idMultzoa(link.getProbaPonderazioak().keySet()).equals(idMultzoa(tests));
        if (changed) {
            link.getEbidentziaPonderazioak().clear(); link.getProbaPonderazioak().clear();
            int count = evidences.size() + tests.size();
            if (count > 0) {
                BigDecimal each = new BigDecimal("100").divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.DOWN);
                int index = 0;
                for (var evidence : evidences) link.getEbidentziaPonderazioak().put(evidence,
                    ++index == count ? new BigDecimal("100.00").subtract(each.multiply(BigDecimal.valueOf(count - 1))) : each);
                for (var test : tests) link.getProbaPonderazioak().put(test,
                    ++index == count ? new BigDecimal("100.00").subtract(each.multiply(BigDecimal.valueOf(count - 1))) : each);
            }
        }
        var result = new ArrayList<Ponderazioa>();
        evidences.stream().sorted(Comparator.comparing(e -> e.getErrubrika().getErronka().getId())).forEach(e -> result.add(new Ponderazioa(
            "EBIDENTZIA", e.getId(), e.getErrubrika().getErronka().getIzena() + " · " + e.getDeskribapena(),
            link.getEbidentziaPonderazioak().get(e))));
        tests.stream().sorted(Comparator.comparing(p -> p.getErrubrika().getErronka().getId())).forEach(p -> result.add(new Ponderazioa(
            "PROBA", p.getId(), p.getErrubrika().getErronka().getIzena() + " · " + p.getIzena(),
            link.getProbaPonderazioak().get(p))));
        return result;
    }

    private Set<Long> idMultzoa(Collection<?> entities) {
        return entities.stream().map(entity -> entity instanceof ErronkaEbidentzia e ? e.getId()
            : ((ErronkaBestelakoProba) entity).getId()).collect(java.util.stream.Collectors.toSet());
    }

    @Transactional public void gordePonderazioa(Long zikloaId, Long ieId, Long moduloaId, Long adierazleaId,
            String mota, Long elementuaId, BigDecimal pisua) {
        balidatuPisua(pisua);
        var indicator = ziklokoAdierazlea(zikloaId, adierazleaId);
        var link = indicator.getKinielaLoturak().stream().filter(l -> l.getEmaitza().getId().equals(ieId)
            && l.getModuloa().getId().equals(moduloaId)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Kinielako lotura ez da aurkitu."));
        sinkronizatuPonderazioak(link);
        if ("EBIDENTZIA".equals(mota)) {
            var element = link.getEbidentziaPonderazioak().keySet().stream().filter(e -> e.getId().equals(elementuaId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ebidentzia ez dago lorpen-adierazle honekin lotuta."));
            link.getEbidentziaPonderazioak().put(element, pisua);
        } else if ("PROBA".equals(mota)) {
            var element = link.getProbaPonderazioak().keySet().stream().filter(p -> p.getId().equals(elementuaId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Proba ez dago lorpen-adierazle honekin lotuta."));
            link.getProbaPonderazioak().put(element, pisua);
        } else throw new IllegalArgumentException("Ponderazio mota ez da baliozkoa.");
    }

    private Optional<ZeharkakoKinielaLotura> zeharkakoLotura(LorpenAdierazlea a, Long gaitasunaId, Long zikloaId) {
        return a.getZeharkakoKinielaLoturak().stream().filter(l -> l.getGaitasuna().getId().equals(gaitasunaId)
            && l.getZikloa().getId().equals(zikloaId)).findFirst();
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
                var removedLinks = a.getKinielaLoturak().stream().filter(l -> l.getEmaitza().getId().equals(ieId)).toList();
                kenduKinielaLoturakErrubriketatik(removedLinks);
                a.getKinielaLoturak().removeAll(removedLinks); }
        }
    }

    private void kenduKinielaLoturakErrubriketatik(Collection<KinielaLotura> links) {
        var ids = links.stream().map(KinielaLotura::getId).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        if (ids.isEmpty()) return;
        errubrikak.findAll().forEach(r -> {
            r.getEbidentziak().forEach(e -> e.getKinielaLoturak().removeIf(l -> ids.contains(l.getId())));
            r.getBestelakoProbak().forEach(p -> p.getKinielaLoturak().removeIf(l -> ids.contains(l.getId())));
        });
        errubrikak.flush();
    }

    @Transactional public void gordeZeharkakoLoturak(Long zikloaId, Long gaitasunaId, Set<Long> ids) {
        var cycle = zikloak.findById(zikloaId).orElseThrow(() -> new IllegalArgumentException("Zikloa ez da aurkitu."));
        var competency = zeharkakoGaitasuna(gaitasunaId);
        var available = competency.getMailak().stream().flatMap(m -> m.getLorpenAdierazleak().stream()).toList();
        require(ids != null && available.stream().map(LorpenAdierazlea::getId).toList().containsAll(ids),
            "Adierazleak ez dira zeharkako gaitasun honetakoak.");
        for (var a : available) {
            var existing = zeharkakoLotura(a, gaitasunaId, zikloaId);
            if (ids.contains(a.getId()) && existing.isEmpty()) {
                var link = new ZeharkakoKinielaLotura(); link.setAdierazlea(a); link.setGaitasuna(competency);
                link.setZikloa(cycle); a.getZeharkakoKinielaLoturak().add(link);
            } else if (!ids.contains(a.getId())) {
                existing.ifPresent(a.getZeharkakoKinielaLoturak()::remove);
            }
        }
    }

    @Transactional public void gordeZeharkakoPisua(Long zikloaId, Long gaitasunaId,
            Long adierazleaId, BigDecimal pisua) {
        balidatuPisua(pisua);
        var a = zeharkakoAdierazlea(gaitasunaId, adierazleaId);
        zeharkakoLotura(a, gaitasunaId, zikloaId)
            .orElseThrow(() -> new IllegalArgumentException("Lotura ez da baliozkoa.")).setPisua(pisua);
    }

    @Transactional public void gordeZeharkakoErronkaLotura(Long zikloaId, Long gaitasunaId,
            Long adierazleaId, Long erronkaId, boolean landuta) {
        var a = zeharkakoAdierazlea(gaitasunaId, adierazleaId);
        var link = zeharkakoLotura(a, gaitasunaId, zikloaId)
            .orElseThrow(() -> new IllegalArgumentException("Lotura ez da baliozkoa."));
        var challenge = erronka(erronkaId);
        require(challenge.getZikloa().getId().equals(zikloaId), "Erronka ez da ziklo honetakoa.");
        if (landuta) link.getErronkak().add(challenge);
        else link.getErronkak().removeIf(e -> e.getId().equals(erronkaId));
    }

    @Transactional public void gordeZeharkakoOharra(Long zikloaId, Long gaitasunaId,
            Long adierazleaId, String oharra) {
        require(oharra != null && oharra.length() <= 10000, "Oharrak gehienez 10000 karaktere izan ditzake.");
        var a = zeharkakoAdierazlea(gaitasunaId, adierazleaId);
        zeharkakoLotura(a, gaitasunaId, zikloaId)
            .orElseThrow(() -> new IllegalArgumentException("Lotura ez da baliozkoa.")).setOharra(oharra.strip());
    }

    private Gaitasuna zeharkakoGaitasuna(Long id) {
        var g = ethazi.gaitasuna(id);
        require(g.getMota() == GaitasunMota.ZEHARKAKOA && g.getZikloa() == null,
            "Gaitasuna ez da zeharkakoa.");
        return g;
    }
    private LorpenAdierazlea zeharkakoAdierazlea(Long gaitasunaId, Long id) {
        return zeharkakoGaitasuna(gaitasunaId).getMailak().stream().flatMap(m -> m.getLorpenAdierazleak().stream())
            .filter(a -> a.getId().equals(id)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Adierazlea ez da zeharkako gaitasun honetakoa."));
    }
    @Transactional public void gordePisua(Long zikloaId, Long ieId, Long adierazleaId, BigDecimal pisua) {
        var ie = ethazi.emaitza(ieId);
        var a = adierazleak.findById(adierazleaId).orElseThrow(() -> new IllegalArgumentException("Adierazlea ez da aurkitu."));
        var gaitasuna = a.getGaitasunMaila().getGaitasuna();
        require(ethazi.ziklokoa(ie, zikloaId)
            && (gaitasuna.getZikloa() == null || gaitasuna.getZikloa().getId().equals(zikloaId)) && lotuta(a, ieId), "Lotura ez da baliozkoa.");
        balidatuPisua(pisua);
        a.getPisuak().put(ie, pisua);
    }
    private void balidatuPisua(BigDecimal pisua) {
        require(pisua != null && pisua.signum() >= 0 && pisua.compareTo(new BigDecimal("100")) <= 0
            && pisua.stripTrailingZeros().scale() <= 2,
            "Pisua 0 eta 100 artekoa izan behar da, gehienez bi hamartarrekin.");
    }
    private static void require(boolean ok, String message) { if (!ok) throw new IllegalArgumentException(message); }
    private static String testua(String s, int max) { require(s != null && !s.isBlank() && s.strip().length() <= max, "Bete izena eta deskribapena, baimendutako luzerarekin."); return s.strip(); }
}
