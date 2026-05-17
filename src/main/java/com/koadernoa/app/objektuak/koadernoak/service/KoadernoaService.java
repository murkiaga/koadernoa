package com.koadernoa.app.objektuak.koadernoak.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioMomentuaRepository;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioNotaRepository;
import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.egutegia.repository.EgutegiaRepository;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EstatistikaEbaluazioan;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Jarduera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.JardueraSortuDto;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoOrdutegiBlokea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoaSortuDto;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa;
import com.koadernoa.app.objektuak.koadernoak.repository.AsistentziaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.EstatistikaEbaluazioanRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.JardueraRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoOrdutegiBlokeaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.NotaFitxategiaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.ProgramazioaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.UnitateDidaktikoaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.SaioaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;
import com.koadernoa.app.objektuak.modulua.repository.MintegiModuluBaimenaRepository;
import com.koadernoa.app.objektuak.logak.entitateak.LogMota;
import com.koadernoa.app.objektuak.logak.service.LogService;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;
import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;
import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KoadernoaService {

    private final ModuloaRepository moduloaRepository;
    private final EgutegiaRepository egutegiaRepository;
    private final IrakasleaRepository irakasleaRepository;
    private final KoadernoaRepository koadernoaRepository;
    private final JardueraRepository jardueraRepository;
    private final EbaluazioMomentuaRepository ebaluazioMomentuaRepository;
    private final ProgramazioaRepository programazioaRepository;
    private final UnitateDidaktikoaRepository unitateDidaktikoaRepository;
    private final LogService logService;
    private final EstatistikaEbaluazioanRepository estatistikaRepository;
    private final SaioaRepository saioaRepository;
    private final MatrikulaRepository matrikulaRepository;
    private final NotaFitxategiaRepository notaFitxategiaRepository;
    private final AsistentziaRepository asistentziaRepository;
    private final EbaluazioNotaRepository ebaluazioNotaRepository;
    private final KoadernoOrdutegiBlokeaRepository koadernoOrdutegiBlokeaRepository;
    private final AplikazioAukeraService aplikazioAukeraService;
    private final MintegiModuluBaimenaRepository mintegiModuluBaimenaRepository;
    
    //Aste-orden estandarra (astelehen-ostiral)
    private static final List<Astegunak> ASTE_ORDENA = List.of(
            Astegunak.ASTELEHENA, Astegunak.ASTEARTEA, Astegunak.ASTEAZKENA, Astegunak.OSTEGUNA, Astegunak.OSTIRALA
    );

    public void sortuKoadernoakIkasturteBerrirako(Ikasturtea ikasturtea) {
        for (Egutegia egutegia : ikasturtea.getEgutegiak()) {
            Maila maila = egutegia.getMaila();
            List<Moduloa> moduluak = moduloaRepository.findByMaila_Id(maila.getId());
            for (Moduloa moduloa : moduluak) {
                Koadernoa koadernoa = new Koadernoa();
                koadernoa.setModuloa(moduloa);
                koadernoa.setEgutegia(egutegia);
                koadernoaRepository.save(koadernoa);
            }
        }
    }

    public Koadernoa findById(Long id) {
        return koadernoaRepository.findById(id).orElseThrow();
    }


    @Transactional
    public Koadernoa gordeMoodleEsteka(Long koadernoId, String moodleEsteka) {
        Koadernoa koadernoa = koadernoaRepository.findById(koadernoId)
                .orElseThrow(() -> new IllegalArgumentException("Koadernoa ez da aurkitu."));

        String esteka = moodleEsteka == null ? null : moodleEsteka.trim();
        koadernoa.setMoodleEsteka(esteka == null || esteka.isBlank() ? null : esteka);
        return koadernoaRepository.save(koadernoa);
    }

    public Optional<Koadernoa> findByIdWithEgutegiaAndEgunBereziak(Long id) {
        return koadernoaRepository.findByIdWithEgutegiaAndEgunBereziak(id);
    }
    
    public List<Koadernoa> findAll() {
        return koadernoaRepository.findAllWithRelations();
    }

    public List<Koadernoa> findByIrakaslea(Irakaslea irakaslea) {
        return koadernoaRepository.findByIrakasleakContaining(irakaslea);
    }

    @Transactional
    public void aldatuJabea(Long koadernoId, Long jabeaId) {
        Koadernoa koadernoa = koadernoaRepository.findById(koadernoId)
                .orElseThrow(() -> new IllegalArgumentException("Koadernoa ez da aurkitu."));

        if (jabeaId == null) {
            koadernoa.setJabea(null);
            koadernoaRepository.save(koadernoa);
            return;
        }

        Irakaslea jabea = irakasleaRepository.findById(jabeaId)
                .orElseThrow(() -> new IllegalArgumentException("Irakaslea ez da aurkitu."));
        boolean koadernokoIrakaslea = koadernoa.getIrakasleak() != null
                && koadernoa.getIrakasleak().stream().anyMatch(ir -> jabeaId.equals(ir.getId()));
        if (!koadernokoIrakaslea) {
            throw new IllegalArgumentException("Jabea koadernoko irakasleen artean egon behar da.");
        }

        koadernoa.setJabea(jabea);
        koadernoaRepository.save(koadernoa);
    }
    
    public List<Moduloa> lortuErabilgarriDaudenModuluak(Irakaslea irakaslea, Long familiaId) {
        if (familiaId == null) return List.of();
        return lortuErabilgarriDaudenModuluak(irakaslea, familiaId, null, null);
    }

    public List<Moduloa> lortuErabilgarriDaudenModuluak(Irakaslea irakaslea, Long familiaId, Long zikloaId, Long mailaId) {
        if (familiaId == null && zikloaId == null && mailaId == null) return List.of();
        if (!familiaIragazkiaErabilgarriaDa(irakaslea, familiaId)) return List.of();
        if (mintegiarenFamiliaDa(irakaslea, familiaId) && mintegiakEeiBaimenakDitu(irakaslea)) {
            return moduloaRepository.bilatuFamiliaEdoEeiKodeekin(familiaId, baimendutakoEeiKodeak(irakaslea), zikloaId, mailaId);
        }
        return moduloaRepository.bilatuFiltroekin(familiaId, zikloaId, mailaId);
    }

    public List<Moduloa> lortuErabilgarriDaudenModuluak(Irakaslea irakaslea) {
        return moduloaRepository.findByTaldea_Zikloa_Familia(irakaslea.getMintegia());
    }

    public List<Zikloa> lortuErabilgarriDaudenZikloak(Irakaslea irakaslea, Long familiaId) {
        if (familiaId == null || !familiaIragazkiaErabilgarriaDa(irakaslea, familiaId)) return List.of();
        if (mintegiarenFamiliaDa(irakaslea, familiaId) && mintegiakEeiBaimenakDitu(irakaslea)) {
            return moduloaRepository.findDistinctZikloakByFamiliaOrEeiKodeak(familiaId, baimendutakoEeiKodeak(irakaslea));
        }
        return moduloaRepository.findDistinctZikloakByFamiliaOrEeiKodeak(familiaId, eeiKodeHutsak());
    }

    public List<Maila> lortuErabilgarriDaudenMailak(Irakaslea irakaslea, Long familiaId, Long zikloaId) {
        if (familiaId == null || !familiaIragazkiaErabilgarriaDa(irakaslea, familiaId)) return List.of();
        if (mintegiarenFamiliaDa(irakaslea, familiaId) && mintegiakEeiBaimenakDitu(irakaslea)) {
            return moduloaRepository.findDistinctMailakByFamiliaOrEeiKodeakAndZikloa(familiaId, baimendutakoEeiKodeak(irakaslea), zikloaId);
        }
        return moduloaRepository.findDistinctMailakByFamiliaAndZikloa(familiaId, zikloaId);
    }

    private boolean familiaIragazkiaErabilgarriaDa(Irakaslea irakaslea, Long familiaId) {
        if (familiaId == null) return false;
        return mintegiarenFamiliaDa(irakaslea, familiaId) || aplikazioAukeraService.getBool(
                AplikazioAukeraService.KOADERNO_BESTE_MINTEGIA_BAIMENDU, false);
    }

    private boolean mintegiarenFamiliaDa(Irakaslea irakaslea, Long familiaId) {
        return irakaslea != null && irakaslea.getMintegia() != null
                && java.util.Objects.equals(irakaslea.getMintegia().getId(), familiaId);
    }

    private boolean mintegiakEeiBaimenakDitu(Irakaslea irakaslea) {
        return irakaslea != null && irakaslea.getMintegia() != null
                && !mintegiModuluBaimenaRepository.findByFamilia_IdAndAktiboTrue(irakaslea.getMintegia().getId()).isEmpty();
    }

    private List<String> baimendutakoEeiKodeak(Irakaslea irakaslea) {
        if (irakaslea == null || irakaslea.getMintegia() == null) return eeiKodeHutsak();
        List<String> kodeak = mintegiModuluBaimenaRepository.findByFamilia_IdAndAktiboTrue(irakaslea.getMintegia().getId())
                .stream()
                .map(b -> b.getEeiKodea() == null ? "" : b.getEeiKodea().trim().toUpperCase())
                .filter(k -> !k.isBlank())
                .distinct()
                .toList();
        return kodeak.isEmpty() ? eeiKodeHutsak() : kodeak;
    }

    private List<String> eeiKodeHutsak() {
        return List.of("__EEI_KODERIK_EZ__");
    }

    public List<Egutegia> lortuEgutegiGuztiak() {
        return egutegiaRepository.findAll();
    }

    public List<Irakaslea> lortuFamiliaBerekoIrakasleak(Irakaslea irakaslea) {
        return irakasleaRepository.findByMintegia(irakaslea.getMintegia())
            .stream().filter(i -> !i.getId().equals(irakaslea.getId()))
            .toList();
    }
    

    @Transactional
    public Koadernoa sortuKoadernoa(KoadernoaSortuDto dto, Irakaslea irakaslea, List<String> cells) {
        KoadernoSorreraEmaitza emaitza = sortuEdoEsleituKoadernoa(dto, irakaslea, cells);
        if (emaitza.egoera() == KoadernoSorreraEmaitza.Egoera.EXISTITZEN_DA) {
            throw new IllegalArgumentException(emaitza.mezua());
        }
        return emaitza.koadernoa();
    }

    @Transactional
    public KoadernoSorreraEmaitza sortuEdoEsleituKoadernoa(KoadernoaSortuDto dto, Irakaslea irakaslea, List<String> cells) {
        Moduloa moduloa = moduloaRepository.findById(dto.getModuloaId())
                .orElseThrow(() -> new IllegalArgumentException("Ez da modulu hori aurkitu."));

        Egutegia egutegia = egutegiaRepository
                .findByIkasturtea_AktiboaTrueAndMaila_Id(moduloa.getMaila().getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Ez da egutegirik aurkitu aktibo dagoen ikasturterako eta maila horretarako."));

        egiaztatuKoadernoSorreraBaimena(moduloa, egutegia, irakaslea);

        List<Irakaslea> irakasleak = prestatuIrakasleZerrenda(dto, irakaslea);
        Optional<Koadernoa> existitzenDenKoadernoa = koadernoaRepository
                .findFirstByModuloa_IdAndEgutegia_IdOrderByIdAsc(moduloa.getId(), egutegia.getId());
        if (existitzenDenKoadernoa.isPresent()) {
            Koadernoa koadernoa = existitzenDenKoadernoa.get();
            if (koadernoa.getJabea() == null) {
                koadernoa.setJabea(irakaslea);
                irakasleak.forEach(i -> gehituIrakasleaBeharBada(koadernoa, i));
                eguneratuOrdutegiakKoadernoan(koadernoa, cells);
                Koadernoa gordeta = koadernoaRepository.save(koadernoa);
                return new KoadernoSorreraEmaitza(
                        gordeta,
                        KoadernoSorreraEmaitza.Egoera.ESLEITUA_JABE_GABEA,
                        "Koadernoa zure izenean esleitu da.");
            }
            return new KoadernoSorreraEmaitza(
                    koadernoa,
                    KoadernoSorreraEmaitza.Egoera.EXISTITZEN_DA,
                    existitzenDenKoadernoarenMezua(koadernoa, moduloa, egutegia));
        }

        Koadernoa k = new Koadernoa();
        k.setModuloa(moduloa);
        k.setEgutegia(egutegia);
        k.setIrakasleak(irakasleak);
        k.setJabea(irakaslea);
        k.setJarduerak(List.of());
        k.setNotaFitxategiak(List.of());
        k.setEstatistikak(new ArrayList<>());

        // 🔹 ORDUTEGIA: cells -> blokeak
        List<KoadernoOrdutegiBlokea> blok = buildBlocksFromCells(k, cells);
        k.setOrdutegiak(blok); // ziurtatu Koadernoa.entitatean cascade=ALL dagoela harreman honetan

        // Estatistikak
        List<EstatistikaEbaluazioan> estatistikak = sortuEstatistikak(k);
        k.setEstatistikak(estatistikak);

        Koadernoa gordeta = koadernoaRepository.save(k);
        return new KoadernoSorreraEmaitza(gordeta, KoadernoSorreraEmaitza.Egoera.SORTUA, "Koadernoa sortu da.");
    }


    private void eguneratuOrdutegiakKoadernoan(Koadernoa koadernoa, List<String> cells) {
        List<KoadernoOrdutegiBlokea> blokeak = buildBlocksFromCells(koadernoa, cells);
        if (koadernoa.getOrdutegiak() == null) {
            koadernoa.setOrdutegiak(blokeak);
            return;
        }
        koadernoa.getOrdutegiak().clear();
        koadernoa.getOrdutegiak().addAll(blokeak);
    }

    private void egiaztatuKoadernoSorreraBaimena(Moduloa moduloa, Egutegia egutegia, Irakaslea irakaslea) {
        boolean besteMintegiaBaimendu = aplikazioAukeraService.getBool(
                AplikazioAukeraService.KOADERNO_BESTE_MINTEGIA_BAIMENDU, false);
        boolean bereMintegikoa = moduloa.getTaldea().getZikloa().getFamilia().equals(irakaslea.getMintegia());
        boolean eeiKodezBaimendua = irakaslea.getMintegia() != null
                && moduloa.getEeiKodea() != null
                && mintegiModuluBaimenaRepository.existsByFamilia_IdAndEeiKodeaIgnoreCaseAndAktiboTrue(
                        irakaslea.getMintegia().getId(), moduloa.getEeiKodea().trim());
        if (!besteMintegiaBaimendu && !bereMintegikoa && !eeiKodezBaimendua) {
            throw new AccessDeniedException("Beste familiako modulua aukeratu da.");
        }
        if (!java.util.Objects.equals(moduloa.getMaila().getId(), egutegia.getMaila().getId())) {
            throw new IllegalArgumentException("Moduluaren eta egutegiaren maila ez datoz bat.");
        }
    }

    private List<Irakaslea> prestatuIrakasleZerrenda(KoadernoaSortuDto dto, Irakaslea irakaslea) {
        List<Irakaslea> irakasleak = new ArrayList<>();
        irakasleak.add(irakaslea);

        if (dto.getIrakasleIdZerrenda() != null && !dto.getIrakasleIdZerrenda().isEmpty()) {
            List<Irakaslea> besteIrakasleak = irakasleaRepository.findAllById(dto.getIrakasleIdZerrenda());
            for (Irakaslea i : besteIrakasleak) {
                if (!i.getId().equals(irakaslea.getId())
                        && irakasleak.stream().noneMatch(existing -> existing.getId().equals(i.getId()))) {
                    irakasleak.add(i);
                }
            }
        }
        return irakasleak;
    }

    private void gehituIrakasleaBeharBada(Koadernoa koadernoa, Irakaslea irakaslea) {
        if (koadernoa.getIrakasleak() == null) {
            koadernoa.setIrakasleak(new ArrayList<>());
        } else if (!(koadernoa.getIrakasleak() instanceof ArrayList)) {
            koadernoa.setIrakasleak(new ArrayList<>(koadernoa.getIrakasleak()));
        }
        boolean badago = koadernoa.getIrakasleak().stream()
                .anyMatch(i -> i.getId() != null && i.getId().equals(irakaslea.getId()));
        if (!badago) {
            koadernoa.getIrakasleak().add(irakaslea);
        }
    }

    private String existitzenDenKoadernoarenMezua(Koadernoa koadernoa, Moduloa moduloa, Egutegia egutegia) {
        String ikasturtea = egutegia.getIkasturtea() != null ? egutegia.getIkasturtea().getIzena() : "ikasturte honetan";
        String oinarria = ikasturtea + "rako " + moduloa.getIzena() + " irakasgairako badago koadernoa sortuta.";
        if (koadernoa.getJabea() == null) {
            return oinarria + " Koaderno horrek ez dauka jaberik.";
        }
        String jabea = koadernoa.getJabea().getIzena() != null && !koadernoa.getJabea().getIzena().isBlank()
                ? koadernoa.getJabea().getIzena()
                : koadernoa.getJabea().getEmaila();
        return oinarria + " Jabea: " + jabea + ".";
    }

    @Transactional
    private List<EstatistikaEbaluazioan> sortuEstatistikak(Koadernoa koadernoa) {
        Egutegia eg = koadernoa.getEgutegia();
        if (eg == null || eg.getMaila() == null) {
            return List.of();
        }

        var momentuak = ebaluazioMomentuaRepository
                .findByMailaAndAktiboTrueOrderByOrdenaAsc(eg.getMaila());

        return momentuak.stream().map(m -> {
            EstatistikaEbaluazioan e = new EstatistikaEbaluazioan();
            e.setEbaluazioMomentua(m);
            e.setKoadernoa(koadernoa);
            return e;
        }).toList();
    }

    /** INSERT segurua: Koadernoa referentziaz (managed) lotu */
    @Transactional
    public void gordeJarduera(Koadernoa koadernoa, JardueraSortuDto dto) {
        if (koadernoa == null || koadernoa.getId() == null) {
            throw new IllegalStateException("Koaderno aktiborik gabe ezin da jarduera gorde");
        }
        Jarduera j = new Jarduera();

        // REFERENTZIAZ: ez pasatu objektua transiente/detached egoeran
        Koadernoa ref = koadernoaRepository.getReferenceById(koadernoa.getId());
        j.setKoadernoa(ref);

        j.setTitulua(dto.getTitulua());
        j.setDeskribapena(dto.getDeskribapena());
        j.setData(dto.getData());
        j.setOrduak(dto.getOrduak());
        j.setMota(dto.getMota());
        ezarriJarduerarenUnitatea(j, dto.getUnitateaId(), koadernoa.getId());

        jardueraRepository.save(j);
    }

    /** UPDATE segurua: koadernoaren jabetza egiaztatu eta gorde */
    @Transactional
    public void eguneratuJarduera(Koadernoa koadernoa, Long id, JardueraSortuDto dto) {
        if (koadernoa == null || koadernoa.getId() == null) {
            throw new IllegalStateException("Koaderno aktiborik gabe ezin da jarduera eguneratu");
        }
        Jarduera j = jardueraRepository.findByIdAndKoadernoaId(id, koadernoa.getId());
        if (j == null) {
            throw new IllegalStateException("Ez da jarduera aurkitu edo ez dagokizu");
        }

        j.setTitulua(dto.getTitulua());
        j.setDeskribapena(dto.getDeskribapena());
        j.setData(dto.getData());
        j.setOrduak(dto.getOrduak());
        j.setMota(dto.getMota());
        ezarriJarduerarenUnitatea(j, dto.getUnitateaId(), koadernoa.getId());

        jardueraRepository.save(j);
    }


    private void ezarriJarduerarenUnitatea(Jarduera jarduera, Long unitateaId, Long koadernoId) {
        if (unitateaId == null) {
            jarduera.setUnitatea(null);
            return;
        }
        var unitatea = unitateDidaktikoaRepository.findById(unitateaId)
                .orElseThrow(() -> new IllegalArgumentException("UDa ez da existitzen."));
        Long unitatearenKoadernoId = unitatea.getProgramazioa() != null
                && unitatea.getProgramazioa().getKoadernoa() != null
                ? unitatea.getProgramazioa().getKoadernoa().getId()
                : null;
        if (!java.util.Objects.equals(unitatearenKoadernoId, koadernoId)) {
            throw new IllegalArgumentException("UDa ez dagokio koaderno honi.");
        }
        jarduera.setUnitatea(unitatea);
    }

    /** QUERY-ak ID bidez, Detached/Transient saihesteko */
    public List<Jarduera> lortuJarduerakDataTartean(Koadernoa koadernoa, LocalDate hasiera, LocalDate amaiera) {
        if (koadernoa == null || koadernoa.getId() == null) return List.of();
        return jardueraRepository.findByKoadernoaIdAndDataBetweenOrderByDataAscIdAsc(
            koadernoa.getId(), hasiera, amaiera
        );
    }

    public Jarduera lortuJardueraKoadernoan(Koadernoa koadernoa, Long id) {
        if (koadernoa == null || koadernoa.getId() == null) return null;
        return jardueraRepository.findByIdAndKoadernoaId(id, koadernoa.getId());
    }
    
    @Transactional
    public void ezabatuJarduera(Koadernoa koadernoa, Long jardueraId) {
        if (koadernoa == null || koadernoa.getId() == null) {
            throw new IllegalArgumentException("Koaderno aktiborik ez.");
        }

        // Zure sinadurarekin: entity edo null
        Jarduera jarduera = jardueraRepository.findByIdAndKoadernoaId(jardueraId, koadernoa.getId());
        if (jarduera == null) {
            throw new IllegalArgumentException("Jarduera ez da existitzen edo ez dagokio koaderno honi.");
        }

        jardueraRepository.delete(jarduera);
    }
    
    @Transactional(readOnly = true)
    public boolean irakasleakBadaukaSarbidea(Irakaslea irakaslea, Koadernoa koadernoa) {

        if (koadernoa == null || koadernoa.getId() == null) return false;

        // 1) ADMIN / KUDEATZAILEA → sarbidea beti
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) ||
                "ROLE_KUDEATZAILEA".equals(a.getAuthority())
        )) {
            return true;
        }

        // 2) Bestela, irakaslea koadernoaren irakasleen artean dagoen ala ez
        if (irakaslea == null || irakaslea.getId() == null) return false;

        return koadernoaRepository.existsByIdAndIrakasleak_Id(
                koadernoa.getId(),
                irakaslea.getId()
        );
    }
    
    @Transactional(readOnly = true)
    public boolean irakasleakBadaukaSarbidea(Irakaslea irakaslea, Long koadernoId) {
        if (irakaslea == null || irakaslea.getId() == null) return false;
        if (koadernoId == null) return false;
        
        // 1) ADMIN / KUDEATZAILEA → sarbidea beti
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) ||
                "ROLE_KUDEATZAILEA".equals(a.getAuthority())
        )) {
            return true;
        }
        
        return koadernoaRepository.existsByIdAndIrakasleak_Id(koadernoId, irakaslea.getId());
    }
    
    
    /** "col-row" (adib. "3-7") zerrendatik blokelista eraikitzen du. */
    private List<KoadernoOrdutegiBlokea> buildBlocksFromCells(Koadernoa k, List<String> cells) {
        if (cells == null || cells.isEmpty()) return new ArrayList<>();

        // bikoiztuak kendu/iragazi
        Set<String> unique = cells.stream()
                .filter(s -> s != null && s.matches("\\d+-\\d+"))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Egun bakoitzeko slot multzo ordenatua
        Map<Integer, SortedSet<Integer>> byDay = new HashMap<>();
        for (String c : unique) {
            String[] p = c.split("-");
            int col = Integer.parseInt(p[0]); // 1..5
            int row = Integer.parseInt(p[1]); // 1..12
            byDay.computeIfAbsent(col, __ -> new TreeSet<>()).add(row);
        }

        List<KoadernoOrdutegiBlokea> out = new ArrayList<>();
        for (var e : byDay.entrySet()) {
            int col = e.getKey();
            SortedSet<Integer> set = e.getValue();
            if (set.isEmpty()) continue;

            int start = -1, prev = -1;
            for (int r : set) {
                if (start == -1) { start = prev = r; continue; }
                if (r == prev + 1) { prev = r; continue; }
                out.add(newBlock(k, col, start, prev));
                start = prev = r;
            }
            out.add(newBlock(k, col, start, prev));
        }
        return out;
    }

    private KoadernoOrdutegiBlokea newBlock(Koadernoa k, int col, int start, int end) {
        KoadernoOrdutegiBlokea b = new KoadernoOrdutegiBlokea();
        b.setKoadernoa(k);
        b.setAsteguna(ASTE_ORDENA.get(col - 1)); // 1->ASTELEHENA ... 5->OSTIRALA
        b.setHasieraSlot(start);
        b.setIraupenaSlot(end - start + 1);
        if (k.getEgutegia() != null) {
            b.setHasieraData(k.getEgutegia().getHasieraData());
        }
        return b;
    }
    
    @Transactional
    public void setSlotSelected(Long koadernoId, int col, int row, boolean selected, LocalDate scheduleStartDate){
        if (col < 1 || col > ASTE_ORDENA.size()) throw new IllegalArgumentException("Egun baliogabea");
        if (row < 1 || row > 12) throw new IllegalArgumentException("Slot baliogabea");

        Koadernoa k = koadernoaRepository.findWithOrdutegiaById(koadernoId).orElseThrow();
        Astegunak eguna = ASTE_ORDENA.get(col - 1);
        LocalDate activeFrom = scheduleStartDate;
        if (activeFrom == null) {
            activeFrom = k.getEgutegia() != null ? k.getEgutegia().getHasieraData() : LocalDate.now();
        }
        final LocalDate activeFromFinal = activeFrom;

        for (KoadernoOrdutegiBlokea b : k.getOrdutegiak()) {
            if (b.getHasieraData() == null && k.getEgutegia() != null) {
                b.setHasieraData(k.getEgutegia().getHasieraData());
            }
        }
        garbituOrdutegiBlokeBaliogabeak(k.getOrdutegiak(), activeFromFinal);

        boolean dualOrdutegia = java.util.Optional.ofNullable(k.getOrdutegiak()).orElse(List.of()).stream()
                .anyMatch(b -> java.util.Objects.equals(b.getHasieraData(), activeFromFinal) && b.isDualOrdutegia());
        if (dualOrdutegia) {
            throw new IllegalStateException("DUAL ordutegian ezin dira blokeak editatu.");
        }

        // Egun honetako blokeak
        List<KoadernoOrdutegiBlokea> dayBlocks = new ArrayList<>();
        for (KoadernoOrdutegiBlokea b : new ArrayList<>(k.getOrdutegiak())) {
            if (b.getAsteguna() == eguna && java.util.Objects.equals(b.getHasieraData(), activeFrom)) dayBlocks.add(b);
        }
        // sort by start
        dayBlocks.sort(Comparator.comparingInt(KoadernoOrdutegiBlokea::getHasieraSlot));

        if (selected) {
            // jada estalita badago, ezer ez
            KoadernoOrdutegiBlokea covering = findCovering(dayBlocks, row);
            if (covering != null) return;
            kenduTarteHutsaMarkatzailea(k.getOrdutegiak(), activeFrom);

            KoadernoOrdutegiBlokea left  = findEndingAt(dayBlocks, row - 1);
            KoadernoOrdutegiBlokea right = findStartingAt(dayBlocks, row + 1);

            if (left != null && right != null) {
                // bi bloke batu -> left zabaldu + right ezabatu
                left.setIraupenaSlot(left.getIraupenaSlot() + 1 + right.getIraupenaSlot());
                k.getOrdutegiak().remove(right);
            } else if (left != null) {
                left.setIraupenaSlot(left.getIraupenaSlot() + 1);
            } else if (right != null) {
                right.setHasieraSlot(right.getHasieraSlot() - 1);
                right.setIraupenaSlot(right.getIraupenaSlot() + 1);
            } else {
                KoadernoOrdutegiBlokea nb = new KoadernoOrdutegiBlokea();
                nb.setKoadernoa(k);
                nb.setAsteguna(eguna);
                nb.setHasieraSlot(row);
                nb.setIraupenaSlot(1);
                nb.setHasieraData(activeFrom);
                nb.setTarteHutsa(false);
                k.getOrdutegiak().add(nb);
            }
        } else {
            KoadernoOrdutegiBlokea covering = findCovering(dayBlocks, row);
            if (covering == null) return; // ez zegoen hautatuta

            int start = covering.getHasieraSlot();
            int end   = covering.bukaeraSlot();
            int len   = covering.getIraupenaSlot();

            if (len == 1) {
                k.getOrdutegiak().remove(covering);
                ensureTarteHutsaIfScheduleHasNoBlocks(k, activeFrom);
            } else if (row == start) {
                covering.setHasieraSlot(start + 1);
                covering.setIraupenaSlot(len - 1);
            } else if (row == end) {
                covering.setIraupenaSlot(len - 1);
            } else {
                // erdian: zatitu bi bloketan
                int leftStart = start;
                int leftEnd   = row - 1;
                int rightStart= row + 1;
                int rightEnd  = end;

                // ezkerra eguneratu
                covering.setHasieraSlot(leftStart);
                covering.setIraupenaSlot(leftEnd - leftStart + 1);

                // eskuina sortu
                KoadernoOrdutegiBlokea right = new KoadernoOrdutegiBlokea();
                right.setKoadernoa(k);
                right.setAsteguna(eguna);
                right.setHasieraSlot(rightStart);
                right.setIraupenaSlot(rightEnd - rightStart + 1);
                right.setHasieraData(activeFrom);
                k.getOrdutegiak().add(right);
            }
        }
        validateOrdutegiak(k.getOrdutegiak());
        // flush/commit @Transactional bidez
    }
    


    private void ensureTarteHutsaIfScheduleHasNoBlocks(Koadernoa k, LocalDate hasieraData) {
        if (k == null || k.getOrdutegiak() == null || hasieraData == null) return;
        boolean hasRegularBlock = k.getOrdutegiak().stream()
                .anyMatch(b -> java.util.Objects.equals(b.getHasieraData(), hasieraData)
                        && !b.isDualOrdutegia()
                        && !b.isTarteHutsa()
                        && b.getAsteguna() != null
                        && b.getIraupenaSlot() > 0);
        boolean hasDual = k.getOrdutegiak().stream()
                .anyMatch(b -> java.util.Objects.equals(b.getHasieraData(), hasieraData) && b.isDualOrdutegia());
        if (!hasRegularBlock && !hasDual) {
            ensureTarteHutsaMarkatzailea(k, hasieraData);
        }
    }

    private void ensureTarteHutsaMarkatzailea(Koadernoa k, LocalDate hasieraData) {
        if (k == null || k.getOrdutegiak() == null || hasieraData == null) return;
        boolean exists = k.getOrdutegiak().stream()
                .anyMatch(b -> java.util.Objects.equals(b.getHasieraData(), hasieraData) && b.isTarteHutsa());
        if (exists) return;

        KoadernoOrdutegiBlokea marker = new KoadernoOrdutegiBlokea();
        marker.setKoadernoa(k);
        marker.setHasieraData(hasieraData);
        marker.setTarteHutsa(true);
        marker.setDualOrdutegia(false);
        marker.setAsteguna(null);
        marker.setHasieraSlot(1);
        marker.setIraupenaSlot(0);
        k.getOrdutegiak().add(marker);
    }

    private void kenduTarteHutsaMarkatzailea(List<KoadernoOrdutegiBlokea> ordutegiak, LocalDate hasieraData) {
        if (ordutegiak == null || hasieraData == null) return;
        ordutegiak.removeIf(b -> java.util.Objects.equals(b.getHasieraData(), hasieraData) && b.isTarteHutsa());
    }

    private void garbituOrdutegiBlokeBaliogabeak(List<KoadernoOrdutegiBlokea> ordutegiak, LocalDate hasieraData) {
        if (ordutegiak == null || hasieraData == null) {
            return;
        }
        boolean dual = ordutegiak.stream().anyMatch(b -> java.util.Objects.equals(b.getHasieraData(), hasieraData) && b.isDualOrdutegia());
        if (dual) {
            return;
        }
        ordutegiak.removeIf(b -> java.util.Objects.equals(b.getHasieraData(), hasieraData)
                && (!b.isDualOrdutegia())
                && (!b.isTarteHutsa())
                && (b.getAsteguna() == null || b.getIraupenaSlot() <= 0));
    }

    private void validateOrdutegiak(List<KoadernoOrdutegiBlokea> ordutegiak) {
        if (ordutegiak == null || ordutegiak.isEmpty()) {
            return;
        }
        java.util.Map<LocalDate, java.util.List<KoadernoOrdutegiBlokea>> byStart = ordutegiak.stream()
                .filter(b -> b != null && b.getHasieraData() != null)
                .collect(java.util.stream.Collectors.groupingBy(KoadernoOrdutegiBlokea::getHasieraData));

        for (var entry : byStart.entrySet()) {
            boolean dual = entry.getValue().stream().anyMatch(KoadernoOrdutegiBlokea::isDualOrdutegia);
            if (dual) {
                continue;
            }
            boolean hasTarteHutsa = entry.getValue().stream().anyMatch(KoadernoOrdutegiBlokea::isTarteHutsa);
            boolean hasValidBlock = false;
            for (KoadernoOrdutegiBlokea b : entry.getValue()) {
                if (b.isTarteHutsa()) {
                    continue;
                }
                if (b.getAsteguna() == null) {
                    throw new IllegalStateException("Ordutegi-bloke arruntak asteguna behar du.");
                }
                if (b.getIraupenaSlot() <= 0) {
                    throw new IllegalStateException("Ordutegi-bloke arruntak iraupenaSlot > 0 behar du.");
                }
                hasValidBlock = true;
            }
            if (!hasValidBlock && !hasTarteHutsa) {
                throw new IllegalStateException("DUAL ez den ordutegiak gutxienez bloke bat edo tarte huts marka bat behar du.");
            }
        }
    }

    @Transactional(readOnly = true)
    public java.util.NavigableMap<LocalDate, List<KoadernoOrdutegiBlokea>> getOrdutegiakByHasieraData(Long koadernoId) {
        Koadernoa k = koadernoaRepository.findWithOrdutegiaById(koadernoId).orElseThrow();
        LocalDate first = k.getEgutegia() != null ? k.getEgutegia().getHasieraData() : LocalDate.now();

        return java.util.Optional.ofNullable(k.getOrdutegiak()).orElse(List.of()).stream()
                .peek(b -> {
                    if (b.getHasieraData() == null) {
                        b.setHasieraData(first);
                    }
                })
                .sorted(Comparator.comparing(KoadernoOrdutegiBlokea::getHasieraData)
                        .thenComparing(KoadernoOrdutegiBlokea::getAsteguna, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparingInt(KoadernoOrdutegiBlokea::getHasieraSlot))
                .collect(Collectors.groupingBy(KoadernoOrdutegiBlokea::getHasieraData,
                        java.util.TreeMap::new,
                        Collectors.toList()));
    }

    @Transactional
    public LocalDate sortuOrdutegiBerria(Long koadernoId, LocalDate hasieraData, boolean dualOrdutegia) {
        Koadernoa k = koadernoaRepository.findWithOrdutegiaById(koadernoId).orElseThrow();
        LocalDate ikastHas = k.getEgutegia() != null ? k.getEgutegia().getHasieraData() : null;
        LocalDate ikastBuk = k.getEgutegia() != null ? k.getEgutegia().getBukaeraData() : null;
        if (hasieraData == null) throw new IllegalArgumentException("Hasiera data beharrezkoa da");
        if (ikastHas != null && hasieraData.isBefore(ikastHas)) throw new IllegalArgumentException("Hasiera data ikasturtea baino lehenago da");
        if (ikastBuk != null && hasieraData.isAfter(ikastBuk)) throw new IllegalArgumentException("Hasiera data ikasturtea baino beranduago da");

        for (KoadernoOrdutegiBlokea b : java.util.Optional.ofNullable(k.getOrdutegiak()).orElse(List.of())) {
            if (b.getHasieraData() == null) b.setHasieraData(ikastHas);
            if (java.util.Objects.equals(b.getHasieraData(), hasieraData)) {
                return hasieraData;
            }
        }
        KoadernoOrdutegiBlokea initial = new KoadernoOrdutegiBlokea();
        initial.setKoadernoa(k);
        initial.setHasieraData(hasieraData);
        initial.setDualOrdutegia(dualOrdutegia);
        initial.setTarteHutsa(!dualOrdutegia);
        initial.setAsteguna(null);
        initial.setHasieraSlot(1);
        initial.setIraupenaSlot(0);
        k.getOrdutegiak().add(initial);
        validateOrdutegiak(k.getOrdutegiak());
        koadernoaRepository.save(k);
        return hasieraData;
    }

    @Transactional
    public boolean ezabatuOrdutegia(Long koadernoId, LocalDate hasieraData) {
        Koadernoa k = koadernoaRepository.findWithOrdutegiaById(koadernoId).orElseThrow();
        if (hasieraData == null) return false;
        int before = k.getOrdutegiak() != null ? k.getOrdutegiak().size() : 0;
        if (k.getOrdutegiak() != null) {
            k.getOrdutegiak().removeIf(b -> java.util.Objects.equals(b.getHasieraData(), hasieraData));
        }
        int after = k.getOrdutegiak() != null ? k.getOrdutegiak().size() : 0;
        return after < before;
    }

    private KoadernoOrdutegiBlokea findCovering(List<KoadernoOrdutegiBlokea> blocks, int slot){
        for (KoadernoOrdutegiBlokea b : blocks){
            if (slot >= b.getHasieraSlot() && slot <= b.bukaeraSlot()) return b;
        }
        return null;
    }
    private KoadernoOrdutegiBlokea findEndingAt(List<KoadernoOrdutegiBlokea> blocks, int slot){
        for (KoadernoOrdutegiBlokea b : blocks){
            if (b.bukaeraSlot() == slot) return b;
        }
        return null;
    }
    private KoadernoOrdutegiBlokea findStartingAt(List<KoadernoOrdutegiBlokea> blocks, int slot){
        for (KoadernoOrdutegiBlokea b : blocks){
            if (b.getHasieraSlot() == slot) return b;
        }
        return null;
    }
    
    public Optional<Koadernoa> findByIdWithOrdutegiaAndEgutegia(Long id) {
	  return koadernoaRepository.findByIdWithOrdutegiaAndEgutegia(id);
	}
    
    public List<Koadernoa> findInportatzekoKoadernoak(Koadernoa koadernoAktiboa) {
        String eei = koadernoAktiboa.getModuloa().getEeiKodea();
        if (eei == null || eei.isBlank()) {
            return List.of(); // ez dago loturarik
        }
        return koadernoaRepository.findByModuloa_EeiKodeaAndIdNot(eei, koadernoAktiboa.getId());
    }
    
    @Transactional
    public void ezabatuKoadernoa(Koadernoa koadernoa, Irakaslea ezabatzailea) {
        if (koadernoa == null || koadernoa.getId() == null) return;
        Long id = koadernoa.getId();

        // 1) SAIOAK + ASISTENTZIAK (KoadernoOrdutegiBlokea-ren lotura)
        List<Saioa> saioak = saioaRepository.findByKoadernoa_Id(id);
        if (!saioak.isEmpty()) {
            List<Long> saioaIds = saioak.stream()
                    .map(Saioa::getId)
                    .toList();

            // Lehenengo asistentziak
            asistentziaRepository.deleteBySaioa_IdIn(saioaIds);
            // Ondoren saioak (hemendik aurrera inork ez du apuntatzen KoadernoOrdutegiBlokea-ra)
            saioaRepository.deleteAll(saioak);
        }

        // 2) Ebaluazio notak (matrikulen bidez)
        ebaluazioNotaRepository.deleteByMatrikula_Koadernoa_Id(id);

        // 3) Matrikulak
        matrikulaRepository.deleteByKoadernoa_Id(id);

        // 4) Jarduerak
        jardueraRepository.deleteByKoadernoa_Id(id);

        // 5) Estatistikak
        estatistikaRepository.deleteByKoadernoa_Id(id);

        // 6) Nota fitxategiak
        notaFitxategiaRepository.deleteByKoadernoa_Id(id);

        // 7) Ordutegiko blokeak
        koadernoOrdutegiBlokeaRepository.deleteByKoadernoa_Id(id);
        // (edo soilik cascade-ri utzi, baina horrela esplizitu doa)

        // 8) Programazioa
        programazioaRepository.deleteByKoadernoaId(id);

        // 9) Ezabaketa loga (orokorra)
        String deskribapena = "Koadernoa ezabatuta: " + koadernoa.getIzena()
                + " | modulua=" + (koadernoa.getModuloa() != null ? koadernoa.getModuloa().getKodea() : "-")
                + " | ikasturtea=" + (koadernoa.getEgutegia() != null && koadernoa.getEgutegia().getIkasturtea() != null
                        ? koadernoa.getEgutegia().getIkasturtea().getIzena()
                        : "-");
        logService.gorde(LogMota.KOADERNO_EZABAKETA, ezabatzailea, "Koadernoa", id, deskribapena);

        // 10) Koadernoa bera
        koadernoaRepository.delete(koadernoa);
    }
    
    @Transactional
    public void aldatuJardueraData(Koadernoa koadernoa, Long jardueraId, LocalDate dataBerria) {
        Jarduera j = lortuJardueraKoadernoan(koadernoa, jardueraId);
        if (j == null) {
            throw new IllegalArgumentException("Jarduera ez da aurkitu edo ez dagokio koaderno honi.");
        }
        j.setData(dataBerria);
        // Normalean jardueraRepository.save(j) egingo zenuke;
        // baina zure service-ak jada kudeatzen badu, bertan
        // dagoen mekanismoa erabili.
    }
    
    @Transactional(readOnly = true)
    public List<Irakaslea> lortuFamiliaBerekoIrakasleak(Irakaslea logeatua, Long familiaId) {

        // familiaId ez bada bidali, logeatuaren mintegia hartu
        Long fid = (familiaId != null)
                ? familiaId
                : (logeatua.getMintegia() != null ? logeatua.getMintegia().getId() : null);

        if (fid == null) return List.of();

        // familiako irakasleak eta logeatua kanpoan utzi (checkboxetan ez agertzeko)
        return irakasleaRepository.findAllByMintegia_IdOrderByIzenaAsc(fid).stream()
                .filter(i -> logeatua.getId() == null || !logeatua.getId().equals(i.getId()))
                .toList();
    }
    
}
