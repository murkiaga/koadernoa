package com.koadernoa.app.ethazi.service;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.koadernoa.app.ethazi.dto.EthaziForms.ErronkaForm;
import com.koadernoa.app.ethazi.entitateak.Erronka;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.*;
import com.koadernoa.app.ethazi.repository.*;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.egutegia.repository.MailaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.*;
import com.koadernoa.app.objektuak.zikloak.repository.ZikloaRepository;
import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class KinielaService {
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
    private final EthaziService ethazi;
    private final ErronkaRepository erronkak;
    private final MailaRepository mailak;
    private final ZikloaRepository zikloak;
    private final LorpenAdierazleaRepository adierazleak;

    public List<Maila> mailak() { return mailak.findAllByAktiboTrueOrderByOrdenaAscIzenaAsc(); }
    public List<Erronka> erronkak(Long zikloaId) {
        var result = zikloaId == null ? List.<Erronka>of() : erronkak.findByZikloaIdOrderByHasieraDataDescIdDesc(zikloaId);
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
        if (id != null && e.getBertsioTaldea() != null)
            require(e.getHizkuntza() == f.getHizkuntza(), "Lotutako bertsioaren hizkuntza ezin da aldatu.");
        e.setIzena(izena); e.setDeskribapena(deskribapena); e.setZikloa(z); e.setMaila(m); e.setHizkuntza(f.getHizkuntza());
        e.setHasieraData(f.getHasieraData()); e.setBukaeraData(f.getBukaeraData()); e.getModuluak().clear(); e.getModuluak().addAll(selected);
        erronkak.save(e);
        if (e.getBertsioTaldea() != null) for (var other : erronkak.findByBertsioTaldea(e.getBertsioTaldea())) {
            if (Objects.equals(other.getId(), e.getId())) continue;
            var mapped = parekoModuluak(e, other.getHizkuntza());
            other.setZikloa(e.getZikloa()); other.setMaila(e.getMaila());
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
        version.setHasieraData(source.getHasieraData()); version.setBukaeraData(source.getBukaeraData());
        version.getModuluak().addAll(modules);
        return erronkak.save(version).getId();
    }
    @Transactional public void ezabatuErronka(Long id) {
        var e = blokeatuErronka(id);
        adierazleak.findByErronkakId(id).forEach(a -> a.getErronkak().remove(e));
        adierazleak.findDistinctByKinielaLoturakErronkakId(id).forEach(a ->
            a.getKinielaLoturak().forEach(l -> l.getErronkak().removeIf(item -> item.getId().equals(id))));
        adierazleak.flush();
        erronkak.delete(e);
    }

    public record ErronkaZutabea(Long id, String izena, String maila, Set<Long> moduloIds) {}
    public List<ErronkaZutabea> erronkaZutabeak(Long zikloaId) {
        return erronkak(zikloaId).stream()
            .sorted(Comparator.comparing((Erronka e) -> e.getMaila().getOrdena(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(e -> e.getMaila().getId())
                .thenComparing(Erronka::getHasieraData).thenComparing(Erronka::getId))
            .map(e -> new ErronkaZutabea(e.getId(), e.getIzena(), e.getMaila().getIzena(), e.getModuluak().stream().map(Moduloa::getId).collect(java.util.stream.Collectors.toSet()))).toList();
    }

    private LorpenAdierazlea ziklokoAdierazlea(Long zikloaId, Long id) {
        var a = adierazleak.findById(id).orElseThrow(() -> new IllegalArgumentException("Adierazlea ez da aurkitu."));
        require(Objects.equals(a.getGaitasunMaila().getGaitasuna().getZikloa().getId(), zikloaId),
            "Adierazlea ez da ziklo honetakoa.");
        return a;
    }
    @Transactional public void gordeErronkaLotura(Long zikloaId, Long adierazleaId, Long ieId, Long moduloaId, Long erronkaId, boolean landuta) {
        var a = ziklokoAdierazlea(zikloaId, adierazleaId);
        var e = erronka(erronkaId);
        require(Objects.equals(e.getZikloa().getId(), zikloaId), "Erronka ez da ziklo honetakoa.");
        var lotura = kinielaLotura(a, zikloaId, ieId, moduloaId);
        require(e.getModuluak().stream().anyMatch(m -> m.getId().equals(moduloaId)), "Erronkak ez du modulu honetan parte hartzen.");
        if (landuta) lotura.getErronkak().add(e);
        else lotura.getErronkak().removeIf(item -> item.getId().equals(erronkaId));
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
    public record Modulua(Long id, String izena, String taldea, String hizkuntza, Hizkuntza hizkuntzaKodea, List<Emaitza> emaitzak, BigDecimal guztira) {}
    public List<LorpenAdierazlea> adierazleak(Long zikloaId) {
        return Arrays.stream(GaitasunMota.values()).flatMap(mota -> ethazi.errubrika(zikloaId, mota).lerroak().stream())
            .flatMap(r -> r.gelaxkak().stream()).filter(Objects::nonNull).flatMap(gm -> gm.getLorpenAdierazleak().stream()).toList();
    }
    public List<Modulua> kiniela(Long zikloaId) {
        var indicators = adierazleak(zikloaId);
        return ethazi.curriculum(zikloaId).stream().map(c -> {
            var results = c.emaitzak().stream().map(ie -> {
                var links = indicators.stream().filter(a -> lotuta(a, ie.getId())).map(a -> new Lotura(a.getId(),
                    a.getGaitasunMaila().getGaitasuna().getKodea() + "." + a.getGaitasunMaila().getMaila().getOrdena() + "." + a.getOrdena(),
                    a.deskribapena(c.moduloa().getHizkuntza()), pisua(a, ie.getId()),
                    erronkaIds(a, ie.getId(), zikloaId, c.moduloa().getId()), oharra(a, ie.getId(), c.moduloa().getId()))).toList();
                return new Emaitza(ie.getId(), ie.getKodea(), ie.deskribapena(c.moduloa().getHizkuntza()), links, links.stream().map(Lotura::pisua).reduce(BigDecimal.ZERO, BigDecimal::add));
            }).toList();
            var m = c.moduloa();
            return new Modulua(m.getId(), m.getKodea() + " · " + m.getIzena(), m.getTaldea().getIzena(), m.getHizkuntza().getEtiketa(), m.getHizkuntza(), results,
                results.stream().map(Emaitza::guztira).reduce(BigDecimal.ZERO, BigDecimal::add));
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
        require(ethazi.ziklokoa(ie, zikloaId)
            && a.getGaitasunMaila().getGaitasuna().getZikloa().getId().equals(zikloaId) && lotuta(a, ieId), "Lotura ez da baliozkoa.");
        require(pisua != null && pisua.signum() >= 0 && pisua.compareTo(new BigDecimal("100")) <= 0 && pisua.stripTrailingZeros().scale() <= 2,
            "Pisua 0 eta 100 artekoa izan behar da, gehienez bi hamartarrekin.");
        a.getPisuak().put(ie, pisua);
    }
    private static void require(boolean ok, String message) { if (!ok) throw new IllegalArgumentException(message); }
    private static String testua(String s, int max) { require(s != null && !s.isBlank() && s.strip().length() <= max, "Bete izena eta deskribapena, baimendutako luzerarekin."); return s.strip(); }
}
