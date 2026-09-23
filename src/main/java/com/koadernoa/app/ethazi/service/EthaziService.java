package com.koadernoa.app.ethazi.service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.ethazi.dto.EthaziForms.*;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.*;
import com.koadernoa.app.ethazi.repository.*;
import com.koadernoa.app.objektuak.modulua.entitateak.*;
import com.koadernoa.app.objektuak.modulua.repository.*;
import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;
import com.koadernoa.app.objektuak.zikloak.repository.ZikloaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EthaziService {
    private final ZikloaRepository zikloak;
    private final ModuloaRepository moduluak;
    private final MailakatzeEreduaRepository ereduak;
    private final MailakatzeMailaRepository mailak;
    private final GaitasunaRepository gaitasunak;
    private final GaitasunMailaRepository gaitasunMailak;
    private final LorpenAdierazleaRepository adierazleak;
    private final IkaskuntzaEmaitzaRepository emaitzak;

    public record ErrubrikaLerroa(Gaitasuna gaitasuna, List<GaitasunMaila> gelaxkak) {}
    public record Errubrika(MailakatzeEredua eredua, List<ErrubrikaLerroa> lerroak) {}
    public record ModuluEmaitzak(Moduloa moduloa, List<IkaskuntzaEmaitza> emaitzak) {}

    public List<Zikloa> zikloak() { return zikloak.findAllByOrderByIzenaAsc(); }
    public List<Moduloa> moduluak(Long zikloaId) {
        return zikloaId == null ? List.of() : moduluak.findByTaldea_Zikloa_IdOrderByIzenaAsc(zikloaId);
    }
    public List<ModuluEmaitzak> curriculum(Long zikloaId) {
        return moduluak(zikloaId).stream()
                .map(m -> new ModuluEmaitzak(m, emaitzak.findByModuloaIdOrderByOrdenaAsc(m.getId()))).toList();
    }
    public List<MailakatzeEredua> ereduak() {
        var result = ereduak.findAll();
        result.forEach(this::prestatu);
        result.sort(Comparator.comparing((MailakatzeEredua e) -> e.getZikloa().getIzena())
                .thenComparing(MailakatzeEredua::getMota));
        return result;
    }
    private MailakatzeEredua prestatu(MailakatzeEredua e) {
        e.getZikloa().getIzena();
        e.getMailak().sort(Comparator.comparing(MailakatzeMaila::getOrdena));
        return e;
    }
    public MailakatzeEredua eredua(Long id) {
        return prestatu(ereduak.findById(id).orElseThrow(() -> errorea("Mailakatze eredua ez da aurkitu.")));
    }
    public MailakatzeEredua eredua(Long zikloaId, GaitasunMota mota) {
        if (zikloaId == null || mota == null) return null;
        return ereduak.findByZikloaIdAndMota(zikloaId, mota).map(this::prestatu).orElse(null);
    }
    public Errubrika errubrika(Long zikloaId, GaitasunMota mota) {
        var e = eredua(zikloaId, mota);
        if (e == null) return new Errubrika(null, List.of());
        var rows = gaitasunak.findByZikloaIdAndMotaOrderByKodeaAsc(zikloaId, mota).stream().map(g -> {
            prestatu(g);
            var byLevel = g.getMailak().stream().collect(Collectors.toMap(m -> m.getMaila().getId(), Function.identity()));
            return new ErrubrikaLerroa(g, e.getMailak().stream().map(m -> byLevel.get(m.getId())).toList());
        }).toList();
        return new Errubrika(e, rows);
    }
    private Gaitasuna prestatu(Gaitasuna g) {
        g.getZikloa().getIzena();
        g.getMailak().forEach(m -> {
            m.getMaila().getIzena();
            m.getLorpenAdierazleak().sort(Comparator.comparing(LorpenAdierazlea::getOrdena));
            m.getLorpenAdierazleak().forEach(a -> a.getIkaskuntzaEmaitzak().forEach(ie -> ie.getModuloa().getIzena()));
        });
        g.getMailak().sort(Comparator.comparing(m -> m.getMaila().getOrdena()));
        return g;
    }
    public Gaitasuna gaitasuna(Long id) {
        return prestatu(gaitasunak.findById(id).orElseThrow(() -> errorea("Gaitasuna ez da aurkitu.")));
    }
    public Map<Long, String> mailaIzenak(Long zikloaId, GaitasunMota mota) {
        var e = eredua(zikloaId, mota);
        return e == null ? Map.of() : e.getMailak().stream().collect(Collectors.toMap(
                MailakatzeMaila::getId, m -> m.getOrdena() + ". maila · " + m.getIzena()));
    }
    public GaitasunaForm gaitasunaForm(Long id, Long zikloaId, GaitasunMota mota) {
        var f = new GaitasunaForm();
        Gaitasuna g = id == null ? null : gaitasuna(id);
        f.setZikloaId(g == null ? zikloaId : g.getZikloa().getId());
        f.setMota(g == null ? mota : g.getMota());
        if (g != null) { f.setKodea(g.getKodea()); f.setDeskribapena(g.getDeskribapena()); }
        var e = eredua(f.getZikloaId(), f.getMota());
        if (e != null) for (var m : e.getMailak()) {
            var mf = new GaitasunMailaForm();
            mf.setMailaId(m.getId());
            if (g != null) g.getMailak().stream().filter(gm -> gm.getMaila().getId().equals(m.getId()))
                    .findFirst().ifPresent(gm -> mf.setDeskribapena(gm.getDeskribapena()));
            f.getMailak().add(mf);
        }
        return f;
    }

    @Transactional
    public Long gordeEredua(Long id, EreduaForm f) {
        var zikloa = zikloa(f.getZikloaId());
        require(f.getMota() != null, "Aukeratu gaitasun mota.");
        String izena = testua(f.getIzena(), 150, "Izena");
        var e = id == null ? new MailakatzeEredua() : eredua(id);
        var duplicate = eredua(f.getZikloaId(), f.getMota());
        require(duplicate == null || Objects.equals(duplicate.getId(), id), "Ziklo eta mota horrek badu mailakatze eredua.");
        if (id != null && (!e.getZikloa().getId().equals(f.getZikloaId()) || e.getMota() != f.getMota())) {
            require(!gaitasunak.existsByZikloaIdAndMota(e.getZikloa().getId(), e.getMota()),
                    "Erabiltzen ari den ereduaren zikloa eta mota ezin dira aldatu.");
        }
        e.setZikloa(zikloa); e.setMota(f.getMota()); e.setIzena(izena);
        return ereduak.save(e).getId();
    }
    @Transactional
    public void ezabatuEredua(Long id) {
        var e = eredua(id);
        require(!gaitasunak.existsByZikloaIdAndMota(e.getZikloa().getId(), e.getMota())
                && e.getMailak().stream().noneMatch(m -> gaitasunMailak.existsByMailaId(m.getId())),
                "Mailakatze eredua erabiltzen ari da; ezin da ezabatu.");
        ereduak.delete(e); ereduak.flush();
    }
    @Transactional
    public void gordeMaila(Long ereduaId, Long id, MailaForm f) {
        var e = eredua(ereduaId);
        var m = id == null ? new MailakatzeMaila() : maila(e, id);
        m.setIzena(testua(f.getIzena(), 100, "Mailaren izena"));
        if (id == null) {
            m.setEredua(e);
            m.setOrdena(e.getMailak().stream().mapToInt(MailakatzeMaila::getOrdena).max().orElse(0) + 1);
            e.getMailak().add(m);
        }
        mailak.save(m);
    }
    @Transactional
    public void ezabatuMaila(Long ereduaId, Long id) {
        var e = eredua(ereduaId); var m = maila(e, id);
        var cells = gaitasunMailak.findByMailaId(id);
        var blockers = cells.stream()
                .filter(cell -> (cell.getDeskribapena() != null && !cell.getDeskribapena().isBlank())
                        || !cell.getLorpenAdierazleak().isEmpty())
                .map(cell -> cell.getGaitasuna().getKodea() + " — " + cell.getGaitasuna().getDeskribapena())
                .distinct().sorted().collect(Collectors.joining("; "));
        require(blockers.isEmpty(), "Maila erabiltzen ari da. Lehenik ezabatu maila honetako deskribapenak eta lorpen-adierazleak gaitasun hauetan: " + blockers);
        for (var cell : cells) cell.getGaitasuna().getMailak().remove(cell);
        gaitasunMailak.deleteAll(cells);
        gaitasunMailak.flush();
        e.getMailak().remove(m); ereduak.flush();
    }

    @Transactional
    public void ezabatuErrubrikaMaila(Long zikloaId, GaitasunMota mota, Long mailaId) {
        var e = eredua(zikloaId, mota);
        require(e != null, "Mailakatze eredua ez da aurkitu.");
        ezabatuMaila(e.getId(), mailaId);
    }
    @Transactional
    public void mugituMaila(Long ereduaId, Long id, int norabidea) {
        require(norabidea == -1 || norabidea == 1, "Mugimendu baliogabea.");
        var e = eredua(ereduaId); var m = maila(e, id);
        int target = e.getMailak().indexOf(m) + norabidea;
        if (target < 0 || target >= e.getMailak().size()) return;
        var other = e.getMailak().get(target);
        int first = m.getOrdena(), second = other.getOrdena();
        // Flush each step to respect the unique (eredua, ordena) constraint during swaps.
        int temporary = e.getMailak().stream().mapToInt(MailakatzeMaila::getOrdena).min().orElse(0) - 1;
        m.setOrdena(temporary); mailak.flush();
        other.setOrdena(first); mailak.flush();
        m.setOrdena(second); mailak.flush();
    }
    private MailakatzeMaila maila(MailakatzeEredua e, Long id) {
        return e.getMailak().stream().filter(m -> m.getId().equals(id)).findFirst()
                .orElseThrow(() -> errorea("Maila ez da eredu honetakoa."));
    }

    @Transactional
    public Long gordeGaitasuna(Long id, GaitasunaForm f) {
        var z = zikloa(f.getZikloaId());
        var e = eredua(f.getZikloaId(), f.getMota());
        require(e != null && !e.getMailak().isEmpty(), "Lehenengo sortu ziklo eta mota honen mailakatze eredua eta mailak.");
        String kodea = testua(f.getKodea(), 30, "Kodea");
        String deskribapena = testua(f.getDeskribapena(), 60000, "Deskribapena");
        var expected = e.getMailak().stream().map(MailakatzeMaila::getId).collect(Collectors.toSet());
        var submitted = f.getMailak().stream().map(GaitasunMailaForm::getMailaId).collect(Collectors.toSet());
        require(expected.equals(submitted) && f.getMailak().size() == expected.size(),
                "Mailakatzea aldatu da edo mailak ez dira zuzenak. Kargatu berriro editatzeko pantaila.");
        var g = id == null ? new Gaitasuna() : gaitasuna(id);
        if (id != null) require(g.getZikloa().getId().equals(f.getZikloaId()) && g.getMota() == f.getMota(),
                "Gaitasunaren zikloa eta mota ezin dira aldatu mailen loturak mantentzeko.");
        g.setZikloa(z); g.setMota(f.getMota()); g.setKodea(kodea); g.setDeskribapena(deskribapena);
        g.setLegacyIzena(deskribapena.substring(0, Math.min(200, deskribapena.length())));
        for (var mf : f.getMailak()) {
            var gm = g.getMailak().stream().filter(m -> m.getMaila().getId().equals(mf.getMailaId())).findFirst().orElse(null);
            if (gm == null) { gm = new GaitasunMaila(); gm.setGaitasuna(g); gm.setMaila(maila(e, mf.getMailaId())); g.getMailak().add(gm); }
            require(mf.getDeskribapena() == null || mf.getDeskribapena().length() <= 60000, "Mailaren deskribapena luzeegia da.");
            gm.setDeskribapena(mf.getDeskribapena());
        }
        return gaitasunak.save(g).getId();
    }
    @Transactional
    public void ezabatuGaitasuna(Long id) { gaitasunak.delete(gaitasuna(id)); gaitasunak.flush(); }

    /** A rubric column is a level of the unique cycle/type model, not another model. */
    @Transactional
    public Long gehituErrubrikaMaila(Long zikloaId, GaitasunMota mota) {
        var z = zikloa(zikloaId);
        require(mota != null, "Aukeratu gaitasun mota.");
        var e = eredua(zikloaId, mota);
        if (e == null) {
            e = new MailakatzeEredua(); e.setZikloa(z); e.setMota(mota);
            String izena = z.getIzena() + " · " + mota;
            e.setIzena(izena.substring(0, Math.min(150, izena.length())));
            e = ereduak.saveAndFlush(e);
        } else {
            e = ereduak.findLockedById(e.getId()).orElseThrow(() -> errorea("Eredua ez da aurkitu."));
        }
        int ordena = e.getMailak().stream().mapToInt(MailakatzeMaila::getOrdena).max().orElse(0) + 1;
        var m = new MailakatzeMaila(); m.setEredua(e); m.setOrdena(ordena); m.setIzena(ordena + ". maila");
        e.getMailak().add(m);
        return mailak.save(m).getId();
    }

    @Transactional
    public void izendatuErrubrikaMaila(Long zikloaId, GaitasunMota mota, Long mailaId, MailaForm f) {
        var e = eredua(zikloaId, mota);
        require(e != null, "Mailakatze eredua ez da aurkitu.");
        maila(e, mailaId).setIzena(testua(f.getIzena(), 100, "Mailaren izena"));
    }

    private Gaitasuna gaitasunaAldatzeko(Long id) {
        return prestatu(gaitasunak.findLockedById(id).orElseThrow(() -> errorea("Gaitasuna ez da aurkitu.")));
    }

    /** Resolve a rubric cell using the model level ID; materialize empty cells only on write. */
    private GaitasunMaila errubrikaGelaxka(Gaitasuna g, Long mailaId, boolean sortu) {
        var e = eredua(g.getZikloa().getId(), g.getMota());
        require(e != null, "Mailakatze eredua ez da aurkitu.");
        var level = maila(e, mailaId);
        var cell = g.getMailak().stream().filter(m -> m.getMaila().getId().equals(mailaId)).findFirst().orElse(null);
        if (cell == null) {
            require(sortu, "Gelaxka hau hutsik dago. Kargatu berriro errubrika.");
            cell = new GaitasunMaila(); cell.setGaitasuna(g); cell.setMaila(level);
            g.getMailak().add(cell); gaitasunMailak.save(cell);
        }
        return cell;
    }

    @Transactional
    public void gordeErrubrikaAdierazlea(Long gaitasunaId, Long mailaId, Long id, AdierazleaForm f) {
        var g = gaitasunaAldatzeko(gaitasunaId);
        gordeAdierazlea(g, errubrikaGelaxka(g, mailaId, id == null), id, f);
    }

    @Transactional
    public void ezabatuErrubrikaAdierazlea(Long gaitasunaId, Long mailaId, Long id) {
        var g = gaitasunaAldatzeko(gaitasunaId);
        kenduAdierazlea(errubrikaGelaxka(g, mailaId, false), id);
    }

    @Transactional
    public void berrordenatuAdierazleak(Long gaitasunaId, Long mailaId, List<Long> ids) {
        var g = gaitasunaAldatzeko(gaitasunaId);
        var gm = errubrikaGelaxka(g, mailaId, false);
        var current = gm.getLorpenAdierazleak().stream().collect(Collectors.toMap(LorpenAdierazlea::getId, Function.identity()));
        require(ids != null && ids.size() == current.size() && new HashSet<>(ids).equals(current.keySet()),
                "Ordena ez da baliozkoa edo gelaxka aldatu da. Kargatu berriro errubrika.");
        for (int i = 0; i < ids.size(); i++) current.get(ids.get(i)).setOrdena(i + 1);
        adierazleak.flush();
    }

    @Transactional
    public void gordeAdierazlea(Long gaitasunaId, Long mailaId, Long id, AdierazleaForm f) {
        var g = gaitasunaAldatzeko(gaitasunaId);
        gordeAdierazlea(g, gaitasunMaila(g, mailaId), id, f);
    }
    private void gordeAdierazlea(Gaitasuna g, GaitasunMaila gm, Long id, AdierazleaForm f) {
        String deskribapena = testua(f.getDeskribapena(), 60000, "Deskribapena");
        Set<IkaskuntzaEmaitza> selected = new LinkedHashSet<>();
        for (Long ieId : f.getEmaitzaIds()) {
            var ie = emaitza(ieId);
            require(ie.getModuloa().getTaldea() != null && ie.getModuloa().getTaldea().getZikloa().getId().equals(g.getZikloa().getId()),
                    "Ikaskuntza-emaitzak gaitasunaren ziklokoak izan behar dira.");
            selected.add(ie);
        }
        var a = id == null ? new LorpenAdierazlea() : adierazlea(gm, id);
        a.setGaitasunMaila(gm);
        if (id == null) a.setOrdena(gm.getLorpenAdierazleak().stream().mapToInt(LorpenAdierazlea::getOrdena).max().orElse(0) + 1);
        a.setDeskribapena(deskribapena);
        a.getIkaskuntzaEmaitzak().clear(); a.getIkaskuntzaEmaitzak().addAll(selected);
        if (id == null) gm.getLorpenAdierazleak().add(a);
        adierazleak.save(a);
    }
    @Transactional
    public void ezabatuAdierazlea(Long gaitasunaId, Long mailaId, Long id) {
        kenduAdierazlea(gaitasunMaila(gaitasunaAldatzeko(gaitasunaId), mailaId), id);
    }
    private void kenduAdierazlea(GaitasunMaila gm, Long id) {
        gm.getLorpenAdierazleak().remove(adierazlea(gm, id));
        gm.getLorpenAdierazleak().sort(Comparator.comparing(LorpenAdierazlea::getOrdena));
        for (int i = 0; i < gm.getLorpenAdierazleak().size(); i++) gm.getLorpenAdierazleak().get(i).setOrdena(i + 1);
        gaitasunak.flush();
    }
    private GaitasunMaila gaitasunMaila(Gaitasuna g, Long id) {
        return g.getMailak().stream().filter(m -> m.getId().equals(id)).findFirst()
                .orElseThrow(() -> errorea("Maila ez da gaitasun honetakoa. Gorde lehenengo gaitasuna."));
    }
    private LorpenAdierazlea adierazlea(GaitasunMaila gm, Long id) {
        return gm.getLorpenAdierazleak().stream().filter(a -> a.getId().equals(id)).findFirst()
                .orElseThrow(() -> errorea("Lorpen-adierazlea ez da maila honetakoa."));
    }
    public List<IkaskuntzaEmaitza> emaitzak(Long zikloaId, Long moduloaId) {
        if (zikloaId == null || moduloaId == null) return List.of();
        moduloa(zikloaId, moduloaId);
        var result = emaitzak.findByModuloaIdOrderByOrdenaAsc(moduloaId);
        result.forEach(ie -> ie.getModuloa().getIzena());
        return result;
    }
    public IkaskuntzaEmaitza emaitza(Long id) {
        var ie = emaitzak.findById(id).orElseThrow(() -> errorea("Ikaskuntza-emaitza ez da aurkitu."));
        ie.getModuloa().getIzena();
        return ie;
    }
    public EmaitzaForm emaitzaForm(Long id) {
        var ie = emaitza(id); var f = new EmaitzaForm();
        require(ie.getModuloa().getTaldea() != null, "Moduluak ez du ziklorik.");
        f.setZikloaId(ie.getModuloa().getTaldea().getZikloa().getId()); f.setModuloaId(ie.getModuloa().getId());
        f.setKodea(ie.getKodea()); f.setOrdena(ie.getOrdena()); f.setDeskribapena(ie.getDeskribapena());
        return f;
    }
    @Transactional
    public void gordeEmaitza(Long id, EmaitzaForm f) {
        var m = moduloa(f.getZikloaId(), f.getModuloaId());
        var ie = id == null ? new IkaskuntzaEmaitza() : emaitza(id);
        if (id != null && !ie.getModuloa().getId().equals(m.getId()))
            require(!adierazleak.existsByIkaskuntzaEmaitzakId(id), "Lotutako ikaskuntza-emaitzaren modulua ezin da aldatu.");
        require(f.getOrdena() != null && f.getOrdena() > 0, "Ordenak zero baino handiagoa izan behar du.");
        ie.setKodea(testua(f.getKodea(), 20, "Kodea")); ie.setDeskribapena(testua(f.getDeskribapena(), 60000, "Deskribapena"));
        ie.setOrdena(f.getOrdena()); ie.setModuloa(m); emaitzak.save(ie);
    }
    @Transactional
    public void ezabatuEmaitza(Long id) {
        var ie = emaitza(id);
        require(!adierazleak.existsByIkaskuntzaEmaitzakId(id), "Ikaskuntza-emaitza lorpen-adierazleetan erabiltzen ari da. Kendu loturak ezabatu aurretik.");
        emaitzak.delete(ie); emaitzak.flush();
    }
    private Moduloa moduloa(Long zikloaId, Long id) {
        require(zikloaId != null && id != null, "Aukeratu zikloa eta modulua.");
        var m = moduluak.findById(id).orElseThrow(() -> errorea("Modulua ez da aurkitu."));
        require(m.getTaldea() != null && m.getTaldea().getZikloa().getId().equals(zikloaId), "Modulua ez da aukeratutako ziklokoa.");
        return m;
    }
    private Zikloa zikloa(Long id) {
        require(id != null, "Aukeratu zikloa.");
        return zikloak.findById(id).orElseThrow(() -> errorea("Zikloa ez da aurkitu."));
    }
    private String testua(String s, int max, String label) {
        require(s != null && !s.isBlank(), label + " bete behar da.");
        require(s.strip().length() <= max, label + " luzeegia da (gehienez " + max + " karaktere).");
        return s.strip();
    }
    private void require(boolean condition, String message) { if (!condition) throw errorea(message); }
    private IllegalArgumentException errorea(String message) { return new IllegalArgumentException(message); }
}
