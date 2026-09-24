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
        var e = id == null ? new Erronka() : erronka(id);
        e.setIzena(izena); e.setDeskribapena(deskribapena); e.setZikloa(z); e.setMaila(m); e.setHizkuntza(f.getHizkuntza());
        e.setHasieraData(f.getHasieraData()); e.setBukaeraData(f.getBukaeraData()); e.getModuluak().clear(); e.getModuluak().addAll(selected);
        erronkak.save(e);
    }
    @Transactional public void ezabatuErronka(Long id) { erronkak.delete(erronka(id)); }

    public record Lotura(Long id, String kodea, String deskribapena, BigDecimal pisua) {}
    public record Emaitza(Long id, String kodea, String deskribapena, List<Lotura> loturak, BigDecimal guztira) {}
    public record Modulua(Long id, String izena, String taldea, String hizkuntza, List<Emaitza> emaitzak, BigDecimal guztira) {}
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
                    a.getDeskribapena(), pisua(a, ie.getId()))).toList();
                return new Emaitza(ie.getId(), ie.getKodea(), ie.getDeskribapena(), links, links.stream().map(Lotura::pisua).reduce(BigDecimal.ZERO, BigDecimal::add));
            }).toList();
            var m = c.moduloa();
            return new Modulua(m.getId(), m.getKodea() + " · " + m.getIzena(), m.getTaldea().getIzena(), m.getHizkuntza().getEtiketa(), results,
                results.stream().map(Emaitza::guztira).reduce(BigDecimal.ZERO, BigDecimal::add));
        }).toList();
    }
    private boolean lotuta(LorpenAdierazlea a, Long ieId) { return a.getIkaskuntzaEmaitzak().stream().anyMatch(ie -> ie.getId().equals(ieId)); }
    private BigDecimal pisua(LorpenAdierazlea a, Long ieId) {
        return a.getPisuak().entrySet().stream().filter(e -> e.getKey().getId().equals(ieId)).map(Map.Entry::getValue).findFirst().orElse(BigDecimal.ZERO);
    }
    @Transactional public void gordeLoturak(Long zikloaId, Long ieId, Set<Long> ids) {
        var ie = ethazi.emaitza(ieId);
        require(ie.getModuloa().getTaldea() != null && ie.getModuloa().getTaldea().getZikloa().getId().equals(zikloaId), "Ikaskuntza-emaitza ez da ziklo honetakoa.");
        var available = adierazleak(zikloaId);
        require(ids != null && available.stream().map(LorpenAdierazlea::getId).toList().containsAll(ids), "Adierazleak ez dira ziklo honetakoak.");
        for (var a : available) {
            if (ids.contains(a.getId())) { if (!lotuta(a, ieId)) a.getIkaskuntzaEmaitzak().add(ie); }
            else { a.getIkaskuntzaEmaitzak().removeIf(e -> e.getId().equals(ieId)); a.getPisuak().keySet().removeIf(e -> e.getId().equals(ieId)); }
        }
    }
    @Transactional public void gordePisua(Long zikloaId, Long ieId, Long adierazleaId, BigDecimal pisua) {
        var ie = ethazi.emaitza(ieId);
        var a = adierazleak.findById(adierazleaId).orElseThrow(() -> new IllegalArgumentException("Adierazlea ez da aurkitu."));
        require(ie.getModuloa().getTaldea() != null && ie.getModuloa().getTaldea().getZikloa().getId().equals(zikloaId)
            && a.getGaitasunMaila().getGaitasuna().getZikloa().getId().equals(zikloaId) && lotuta(a, ieId), "Lotura ez da baliozkoa.");
        require(pisua != null && pisua.signum() >= 0 && pisua.compareTo(new BigDecimal("100")) <= 0 && pisua.stripTrailingZeros().scale() <= 2,
            "Pisua 0 eta 100 artekoa izan behar da, gehienez bi hamartarrekin.");
        a.getPisuak().put(ie, pisua);
    }
    private static void require(boolean ok, String message) { if (!ok) throw new IllegalArgumentException(message); }
    private static String testua(String s, int max) { require(s != null && !s.isBlank() && s.strip().length() <= max, "Bete izena eta deskribapena, baimendutako luzerarekin."); return s.strip(); }
}
