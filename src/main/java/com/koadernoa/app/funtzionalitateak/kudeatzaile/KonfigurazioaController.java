package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioEgoera;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioMomentua;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EzadostasunKonfig;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioEgoeraRepository;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioMomentuaRepository;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioNotaRepository;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EzadostasunKonfigRepository;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.egutegia.repository.MailaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;
import com.koadernoa.app.objektuak.zikloak.repository.FamiliaRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/kudeatzaile/konfigurazioa")
@RequiredArgsConstructor
public class KonfigurazioaController {

    private final MailaRepository mailaRepository;
    private final FamiliaRepository familiaRepository;
    private final EbaluazioMomentuaRepository ebaluazioMomentuaRepository;
    private final EbaluazioEgoeraRepository ebaluazioEgoeraRepository;
    private final EbaluazioNotaRepository ebaluazioNotaRepository;
    private final EzadostasunKonfigRepository ezadostasunKonfigRepository;

    // ---- GET: orria ----
    @GetMapping
    public String index(Model model) {
        // MAILAK
        List<Maila> mailak = mailaRepository.findAll(
            Sort.by(Sort.Order.asc("ordena"), Sort.Order.asc("izena"))
        );
        ActiveMailakForm mailaForm = new ActiveMailakForm();
        mailaForm.setAktiboIds(mailak.stream().filter(Maila::getAktibo).map(Maila::getId).toList());

        // FAMILIAK
        List<Familia> familiak = familiaRepository.findAll(Sort.by(Sort.Order.asc("izena")));
        ActiveFamiliakForm familiaForm = new ActiveFamiliakForm();
        familiaForm.setAktiboIds(familiak.stream().filter(Familia::isAktibo).map(Familia::getId).toList());

        // EBALUAZIO EGOERAK
        List<EbaluazioEgoera> egoerak = ebaluazioEgoeraRepository.findAllByOrderByKodeaAsc();
        
        // EZADOSTASUN KONFIGURAZIOAK ====
        List<EzadostasunKonfig> ezadKonfigList =
                ezadostasunKonfigRepository.findAll(org.springframework.data.domain.Sort.by("kodea").ascending());

        model.addAttribute("mailak", mailak);
        model.addAttribute("activeForm", mailaForm);
        model.addAttribute("sortuForm", new SortuMailaForm());

        model.addAttribute("familiak", familiak);
        model.addAttribute("activeFamiliaForm", familiaForm);
        model.addAttribute("sortuFamiliaForm", new SortuFamiliaForm());

        model.addAttribute("egoerak", egoerak);
        model.addAttribute("sortuEgoeraForm", new SortuEbaluazioEgoeraForm());
        
        model.addAttribute("ezadostasunKonfigList", ezadKonfigList);

        return "kudeatzaile/konfigurazioa/index";
    }

    // ---- MAILAK: aktibo/desaktibo bulk ----
    @PostMapping("/mailak/aktiboak")
    public String gordeAktiboak(@ModelAttribute("activeForm") ActiveMailakForm form) {
        List<Long> aktiboIds = form.getAktiboIds() != null ? form.getAktiboIds() : List.of();
        List<Maila> denak = mailaRepository.findAll();
        for (Maila m : denak) {
            boolean aktibo = aktiboIds.contains(m.getId());
            if (Boolean.TRUE.equals(m.getAktibo()) != aktibo) m.setAktibo(aktibo);
        }
        mailaRepository.saveAll(denak);
        return "redirect:/kudeatzaile/konfigurazioa#mailak";
    }

    // ---- MAILAK: sortu ----
    @PostMapping("/mailak/sortu")
    public String sortuMaila(@ModelAttribute("sortuForm") SortuMailaForm form, BindingResult br, Model model) {
        if (form.getKodea() == null || form.getKodea().isBlank()) {
            br.rejectValue("kodea", "kodea.blank", "Kodea beharrezkoa da");
        }
        if (br.hasErrors()) {
            // berriz kargatu datuak
            model.addAttribute("mailak", mailaRepository.findAll(
                Sort.by(Sort.Order.asc("ordena"), Sort.Order.asc("izena"))
            ));
            model.addAttribute("activeForm", new ActiveMailakForm());

            model.addAttribute("familiak", familiaRepository.findAll(Sort.by(Sort.Order.asc("izena"))));
            model.addAttribute("activeFamiliaForm", new ActiveFamiliakForm());
            model.addAttribute("sortuFamiliaForm", new SortuFamiliaForm());

            // egoerak ere gehitu (bestela pestaña horrek petatuko du)
            model.addAttribute("egoerak", ebaluazioEgoeraRepository.findAllByOrderByKodeaAsc());
            model.addAttribute("sortuEgoeraForm", new SortuEbaluazioEgoeraForm());

            return "kudeatzaile/konfigurazioa/index";
        }

        mailaRepository.findByKodea(form.getKodea().trim()).ifPresent(m ->
            { throw new IllegalArgumentException("Kode hori dagoeneko existitzen da: " + form.getKodea()); });

        Maila m = new Maila();
        m.setKodea(form.getKodea().trim());
        m.setIzena(form.getIzena() == null ? form.getKodea().trim() : form.getIzena().trim());
        m.setOrdena(form.getOrdena() != null ? form.getOrdena() : 999);
        m.setAktibo(true);
        mailaRepository.save(m);

        return "redirect:/kudeatzaile/konfigurazioa#mailak";
    }

    // ---- FAMILIAK: aktibo/desaktibo bulk ----
    @PostMapping("/familiak/aktiboak")
    public String gordeFamiliaAktiboak(@ModelAttribute("activeFamiliaForm") ActiveFamiliakForm form) {
        List<Long> aktiboIds = form.getAktiboIds() != null ? form.getAktiboIds() : List.of();
        List<Familia> denak = familiaRepository.findAll();
        for (Familia f : denak) {
            boolean aktibo = aktiboIds.contains(f.getId());
            if (f.isAktibo() != aktibo) f.setAktibo(aktibo);
        }
        familiaRepository.saveAll(denak);
        return "redirect:/kudeatzaile/konfigurazioa#familiak";
    }

    // ---- FAMILIAK: sortu ----
    @PostMapping("/familiak/sortu")
    public String sortuFamilia(@ModelAttribute("sortuFamiliaForm") SortuFamiliaForm form,
                               BindingResult br, Model model) {
        if (form.getIzena() == null || form.getIzena().isBlank()) {
            br.rejectValue("izena", "izena.blank", "Izena beharrezkoa da");
        }
        if (br.hasErrors()) {
            // panelak berriz prestatu
            model.addAttribute("mailak", mailaRepository.findAll(
                Sort.by(Sort.Order.asc("ordena"), Sort.Order.asc("izena"))
            ));
            model.addAttribute("activeForm", new ActiveMailakForm());

            model.addAttribute("familiak", familiaRepository.findAll(Sort.by(Sort.Order.asc("izena"))));
            model.addAttribute("activeFamiliaForm", new ActiveFamiliakForm());

            // egoerak ere gehitu
            model.addAttribute("egoerak", ebaluazioEgoeraRepository.findAllByOrderByKodeaAsc());
            model.addAttribute("sortuEgoeraForm", new SortuEbaluazioEgoeraForm());

            return "kudeatzaile/konfigurazioa/index";
        }

        String izena = form.getIzena().trim();
        if (familiaRepository.existsByIzenaIgnoreCase(izena)) {
            throw new IllegalArgumentException("Izena existitzen da: " + izena);
        }

        // slug automatikoa
        String base = slugify(izena);
        String slug = base;
        int i = 2;
        // aukeran: baldin baduzu existsBySlugIgnoreCase, hobeto:
        while (familiaRepository.findByIzenaIgnoreCase(slug).isPresent()
               || (hasExistsBySlug(familiaRepository) && familiaRepository.existsBySlugIgnoreCase(slug))) {
            slug = base + "-" + i++;
        }

        Familia f = new Familia();
        f.setIzena(izena);
        f.setSlug(slug);
        f.setAktibo(true);
        familiaRepository.save(f);

        return "redirect:/kudeatzaile/konfigurazioa#familiak";
    }
    
    // Util txiki bat controller barruan edo Helper klase batean:
    private static String slugify(String input) {
        String s = input.toLowerCase().trim();
        s = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+|-+$", "");
        return s.isBlank() ? "item" : s;
    }
    
    // existsBySlugIgnoreCase baduzu erabili; bestela helper “false” itzuli
    private static boolean hasExistsBySlug(FamiliaRepository repo) {
        try {
            repo.getClass().getMethod("existsBySlugIgnoreCase", String.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    // ===== Ebaluazio Momentuak =====
 // GET: zerrenda + formularioa
    @GetMapping("/mailak/{mailaId}/ebaluazioak")
    public String ebaluazioMomentuakPantaila(@PathVariable Long mailaId,
                                             Model model,
                                             RedirectAttributes redirectAttributes) {

        Maila maila = mailaRepository.findById(mailaId).orElse(null);
        if (maila == null) {
            redirectAttributes.addFlashAttribute("error", "Maila ez da aurkitu.");
            return "redirect:/kudeatzaile/konfigurazioa#mailak";
        }

        // HEMEN ALDAKETA: aktibo == true ez dugu filtratzen pantaila honetan
        List<EbaluazioMomentua> momentuak =
                ebaluazioMomentuaRepository.findByMailaOrderByOrdenaAsc(maila);

        // Egoera berezi guztiak (checkbox zerrendan erakusteko)
        List<EbaluazioEgoera> egoeraGuztiak =
                ebaluazioEgoeraRepository.findAllByOrderByKodeaAsc();
        
        List<EzadostasunKonfig> ezadostasunKonfigurazioak =
                ezadostasunKonfigRepository.findAllByOrderByKodeaAsc();

        model.addAttribute("maila", maila);
        model.addAttribute("momentuak", momentuak);
        model.addAttribute("egoeraGuztiak", egoeraGuztiak);
        model.addAttribute("ezadostasunKonfigurazioak", ezadostasunKonfigurazioak);

        return "kudeatzaile/konfigurazioa/mailak-ebaluazioak";
    }

    @PostMapping("/mailak/{mailaId}/ebaluazioak")
    @Transactional
    public String gordeEbaluazioMomentuak(@PathVariable Long mailaId,
                                          HttpServletRequest request,
                                          RedirectAttributes redirectAttributes) {

        Maila maila = mailaRepository.findById(mailaId).orElse(null);
        if (maila == null) {
            redirectAttributes.addFlashAttribute("error", "Maila ez da aurkitu.");
            return "redirect:/kudeatzaile/konfigurazioa#mailak";
        }

        // ======== EXISTENTEAK EGUNERATU / EZABATU =========
        String[] idParamArray   = request.getParameterValues("ids");
        String[] deleteIdsArray = request.getParameterValues("deleteIds");

        Set<Long> deleteIds = new HashSet<>();
        if (deleteIdsArray != null) {
            Arrays.stream(deleteIdsArray).forEach(s -> {
                try { deleteIds.add(Long.valueOf(s)); } catch (NumberFormatException ignored) {}
            });
        }

        if (idParamArray != null) {
            for (String idStr : idParamArray) {
                Long id;
                try {
                    id = Long.valueOf(idStr);
                } catch (NumberFormatException ex) {
                    continue;
                }

                EbaluazioMomentua em = ebaluazioMomentuaRepository.findById(id).orElse(null);
                if (em == null) continue;

                // Ezabatzeko markatuta badago → delete
                if (deleteIds.contains(id)) {
                    ebaluazioMomentuaRepository.delete(em);
                    continue;
                }

                // ====== Oinarrizko eremuak (kodea EZ ukitu) ======
                String izenaParam  = request.getParameter("izena_" + id);
                String ordenaStr   = request.getParameter("ordena_" + id);

                boolean aktibo = request.getParameter("aktibo_" + id) != null;
                boolean onartuNotaZenbakizkoa =
                        request.getParameter("onartuNotaZenbakizkoa_" + id) != null;
                boolean urteOsoa =
                        request.getParameter("urteOsoa_" + id) != null;

                // KODEA EZ DA EGUNERATZEN
                // (em.getKodea() DBtik dator eta hor mantentzen da)

                // IZENA: inoiz ez utzi null/hutsik (nullable=false)
                if (izenaParam != null) {
                    String iTrim = izenaParam.trim();
                    if (!iTrim.isEmpty()) {
                        em.setIzena(iTrim);
                    } else if (em.getKodea() != null && !em.getKodea().isBlank()) {
                        em.setIzena(em.getKodea());
                    } else {
                        em.setIzena("EB_" + id);
                    }
                }
                if (em.getIzena() == null || em.getIzena().isBlank()) {
                    em.setIzena(
                        (em.getKodea() != null && !em.getKodea().isBlank())
                            ? em.getKodea()
                            : ("EB_" + id)
                    );
                }

                // BOOLEARRAK
                em.setAktibo(aktibo);
                em.setOnartuNotaZenbakizkoa(onartuNotaZenbakizkoa);
                em.setUrteOsoa(urteOsoa);

                // Ordena
                Integer ordena = em.getOrdena();
                if (ordenaStr != null && !ordenaStr.isBlank()) {
                    try {
                        ordena = Integer.valueOf(ordenaStr.trim());
                    } catch (NumberFormatException ignored) {}
                }
                em.setOrdena(ordena);

                // ===== Egoera onartuak =====
                String[] egoeraIdArray = request.getParameterValues("egoerak_" + id);

                Set<EbaluazioEgoera> egoeraOnartuak = new LinkedHashSet<>();
                if (egoeraIdArray != null) {
                    for (String egoeraIdStr : egoeraIdArray) {
                        try {
                            Long egoeraId = Long.valueOf(egoeraIdStr);
                            ebaluazioEgoeraRepository.findById(egoeraId)
                                    .ifPresent(egoeraOnartuak::add);
                        } catch (NumberFormatException ignored) {}
                    }
                }

                em.getEgoeraOnartuak().clear();
                em.getEgoeraOnartuak().addAll(egoeraOnartuak);
                
                // === Ezadostasun konfigurazioa ===
                String ezadCfgIdStr = request.getParameter("ezadostasunKonfigId_" + id);
                if (ezadCfgIdStr == null || ezadCfgIdStr.isBlank()) {
                    em.setEzadostasunKonfig(null); // (defektuzko logika erabiliko duzu gero)
                } else {
                    try {
                        Long cfgId = Long.valueOf(ezadCfgIdStr);
                        EzadostasunKonfig cfg = ezadostasunKonfigRepository
                                .findById(cfgId).orElse(null);
                        em.setEzadostasunKonfig(cfg);
                    } catch (NumberFormatException ignored) {
                        // ez aldatu
                    }
                }

                ebaluazioMomentuaRepository.save(em);
            }
        }

        redirectAttributes.addFlashAttribute("success", "Ebaluazio momentuak gorde dira.");
        return "redirect:/kudeatzaile/konfigurazioa/mailak/" + mailaId + "/ebaluazioak";
    }

    @PostMapping("/mailak/{mailaId}/ebaluazioak/berria")
    @Transactional
    public String sortuEbaluazioMomentua(@PathVariable Long mailaId,
                                         HttpServletRequest request,
                                         RedirectAttributes redirectAttributes) {

        Maila maila = mailaRepository.findById(mailaId).orElse(null);
        if (maila == null) {
            redirectAttributes.addFlashAttribute("error", "Maila ez da aurkitu.");
            return "redirect:/kudeatzaile/konfigurazioa#mailak";
        }

        String kodea     = request.getParameter("kodea");
        String izena     = request.getParameter("izena");
        String ordenaStr = request.getParameter("ordena");
        boolean aktibo   = request.getParameter("aktibo") != null;
        boolean onartu   = request.getParameter("onartuNotaZenbakizkoa") != null;
        boolean urteOsoa = request.getParameter("urteOsoa") != null;

        EbaluazioMomentua em = new EbaluazioMomentua();
        em.setMaila(maila);
        em.setKodea(kodea != null ? kodea.trim() : null);
        em.setIzena(izena != null ? izena.trim() : null);
        em.setAktibo(aktibo);
        em.setOnartuNotaZenbakizkoa(onartu);
        em.setUrteOsoa(urteOsoa);

        if (ordenaStr != null && !ordenaStr.isBlank()) {
            try {
                em.setOrdena(Integer.valueOf(ordenaStr));
            } catch (NumberFormatException ignored) {}
        }

        String[] egoeraIdArray = request.getParameterValues("egoerak");
        if (egoeraIdArray != null) {
            Set<EbaluazioEgoera> egoeraOnartuak = new LinkedHashSet<>();
            for (String egoeraIdStr : egoeraIdArray) {
                try {
                    Long egoeraId = Long.valueOf(egoeraIdStr);
                    ebaluazioEgoeraRepository.findById(egoeraId)
                            .ifPresent(egoeraOnartuak::add);
                } catch (NumberFormatException ignored) {}
            }
            em.setEgoeraOnartuak(egoeraOnartuak);
        }

        ebaluazioMomentuaRepository.save(em);

        redirectAttributes.addFlashAttribute("success", "Ebaluazio momentu berria gorde da.");
        return "redirect:/kudeatzaile/konfigurazioa/mailak/" + mailaId + "/ebaluazioak";
    }




    // ===== EBALUAZIO EGOERAK: existenteak eguneratu =====
    @PostMapping("/egoerak/gorde")
    @Transactional
    public String gordeEbaluazioEgoerak(HttpServletRequest request,
                                        RedirectAttributes redirectAttributes) {

        String[] idArray = request.getParameterValues("ids");
        String[] deleteIdsArray = request.getParameterValues("deleteIds");

        // Ezabaketarako set-a
        Set<Long> deleteIds = new HashSet<>();
        if (deleteIdsArray != null) {
            Arrays.stream(deleteIdsArray).forEach(s -> {
                try { deleteIds.add(Long.valueOf(s)); } catch (NumberFormatException ignored) {}
            });
        }

        if (idArray != null) {
            for (String idStr : idArray) {
                Long id;
                try {
                    id = Long.valueOf(idStr);
                } catch (NumberFormatException ex) {
                    continue;
                }

                EbaluazioEgoera egoera = ebaluazioEgoeraRepository.findById(id).orElse(null);
                if (egoera == null) continue;

                // 1) Lehenengo: ezabatu behar den ala ez
                if (deleteIds.contains(id)) {

                    // → KONTROLA: ezabatu aurretik begiratu ea erabiltzen den
                    boolean erabiltzenDaMomentuetan =
                            ebaluazioMomentuaRepository.existsByEgoeraOnartuakContains(egoera);
                    boolean erabiltzenDaNotetan =
                            ebaluazioNotaRepository.existsByEgoera(egoera);

                    if (erabiltzenDaMomentuetan || erabiltzenDaNotetan) {
                        redirectAttributes.addFlashAttribute("error",
                                "Ezin da ezabatu egoera, erabilia dagoelako (kodea: " + egoera.getKodea() + ").");
                        return "redirect:/kudeatzaile/konfigurazioa#egoerak";
                    }

                    ebaluazioEgoeraRepository.delete(egoera);
                    continue;
                }

                // 2) Bestela: eguneratu kodea, izena eta notaBeharDu
                String kodeaParam = request.getParameter("kodea_" + id);
                String izenaParam = request.getParameter("izena_" + id);
                boolean notaBeharDu = request.getParameter("notaBeharDu_" + id) != null;

                if (kodeaParam == null || kodeaParam.isBlank()
                        || izenaParam == null || izenaParam.isBlank()) {
                    redirectAttributes.addFlashAttribute("error",
                            "Kodea eta izena derrigorrezkoak dira (ID: " + id + ").");
                    return "redirect:/kudeatzaile/konfigurazioa#egoerak";
                }

                String kodea = kodeaParam.trim();
                String izena = izenaParam.trim();

                // 3) Kodearen unikotasuna (lambda erabili gabe → 'final' arazoa desagertzen da)
                EbaluazioEgoera existing =
                        ebaluazioEgoeraRepository.findByKodea(kodea).orElse(null);

                if (existing != null && !existing.getId().equals(id)) {
                    redirectAttributes.addFlashAttribute("error",
                            "Kode errepikatua: " + kodea);
                    return "redirect:/kudeatzaile/konfigurazioa#egoerak";
                }

                egoera.setKodea(kodea);
                egoera.setIzena(izena);
                egoera.setNotaBeharDu(notaBeharDu);

                ebaluazioEgoeraRepository.save(egoera);
            }
        }

        redirectAttributes.addFlashAttribute("success", "Ebaluazio egoerak eguneratu dira.");
        return "redirect:/kudeatzaile/konfigurazioa#egoerak";
    }


    // ===== EBALUAZIO EGOERAK: sortu berria =====
    @PostMapping("/egoerak/sortu")
    public String sortuEbaluazioEgoera(
            @ModelAttribute("sortuEgoeraForm") SortuEbaluazioEgoeraForm form,
            RedirectAttributes redirectAttributes) {

        String kodea = form.getKodea() != null ? form.getKodea().trim() : null;
        String izena = form.getIzena() != null ? form.getIzena().trim() : null;

        if (kodea == null || kodea.isBlank() || izena == null || izena.isBlank()) {
            redirectAttributes.addFlashAttribute("error",
                    "Kodea eta izena derrigorrezkoak dira.");
            return "redirect:/kudeatzaile/konfigurazioa#egoerak";
        }

        if (ebaluazioEgoeraRepository.existsByKodea(kodea)) {
            redirectAttributes.addFlashAttribute("error",
                    "Kode hori dagoeneko existitzen da: " + kodea);
            return "redirect:/kudeatzaile/konfigurazioa#egoerak";
        }

        EbaluazioEgoera egoera = new EbaluazioEgoera();
        egoera.setKodea(kodea);
        egoera.setIzena(izena);
        egoera.setNotaBeharDu(Boolean.TRUE.equals(form.getNotaBeharDu()));

        ebaluazioEgoeraRepository.save(egoera);

        redirectAttributes.addFlashAttribute("success", "Ebaluazio egoera berria sortu da.");
        return "redirect:/kudeatzaile/konfigurazioa#egoerak";
    }
    
    // ===== Ezadostasunak =====
    @PostMapping("/ezadostasunak/sortu")
    @Transactional
    public String sortuEzadostasunKonfig(HttpServletRequest request,
                                         RedirectAttributes ra) {

        String kodea = request.getParameter("kodea");
        String minBlokeStr       = request.getParameter("minBlokePortzentaia");
        String minOrduStr        = request.getParameter("minOrduPortzentaia");
        String minBertaratzeStr  = request.getParameter("minBertaratzePortzentaia");
        String minGaindituStr    = request.getParameter("minGaindituPortzentaia");

        if (kodea == null || kodea.trim().isEmpty()) {
            ra.addFlashAttribute("error", "Kodea beharrezkoa da.");
            return "redirect:/kudeatzaile/konfigurazioa#ezadostasunak";
        }

        // Kode bakarra dela bermatu nahi baduzu:
        if (ezadostasunKonfigRepository.existsByKodeaIgnoreCase(kodea.trim())) {
            ra.addFlashAttribute("error", "Kode hori jada existitzen da: " + kodea.trim());
            return "redirect:/kudeatzaile/konfigurazioa#ezadostasunak";
        }

        EzadostasunKonfig cfg = new EzadostasunKonfig();
        cfg.setKodea(kodea.trim());

        // Laguntzailea
        java.util.function.BiFunction<String,Integer,Integer> parseOrDefault =
                (str, def) -> {
                    if (str == null || str.isBlank()) return def;
                    try {
                        int v = Integer.parseInt(str.trim());
                        if (v < 0)   v = 0;
                        if (v > 100) v = 100;
                        return v;
                    } catch (NumberFormatException e) {
                        return def;
                    }
                };

        // Entitateko default-ak hartu eta gainidatzi POST-etik
        cfg.setMinBlokePortzentaia(parseOrDefault.apply(minBlokeStr, cfg.getMinBlokePortzentaia()));
        cfg.setMinOrduPortzentaia(parseOrDefault.apply(minOrduStr, cfg.getMinOrduPortzentaia()));
        cfg.setMinBertaratzePortzentaia(parseOrDefault.apply(minBertaratzeStr, cfg.getMinBertaratzePortzentaia()));
        cfg.setMinGaindituPortzentaia(parseOrDefault.apply(minGaindituStr, cfg.getMinGaindituPortzentaia()));

        ezadostasunKonfigRepository.save(cfg);

        ra.addFlashAttribute("success", "Ezadostasun konfigurazio berria sortu da.");
        return "redirect:/kudeatzaile/konfigurazioa#ezadostasunak";
    }
    
    @PostMapping("/ezadostasunak/gorde")
    @Transactional
    public String gordeEzadostasunKonfigurazioak(HttpServletRequest request,
                                                 RedirectAttributes ra) {

        String[] idParams = request.getParameterValues("ids");
        String[] deleteIdsParams = request.getParameterValues("deleteIds");

        // Ezabatzeko markatutakoak
        Set<Long> deleteIds = new HashSet<>();
        if (deleteIdsParams != null) {
            for (String s : deleteIdsParams) {
                try {
                    deleteIds.add(Long.valueOf(s));
                } catch (NumberFormatException ignored) {}
            }
        }

        if (idParams != null) {
            for (String idStr : idParams) {
                Long id;
                try {
                    id = Long.valueOf(idStr);
                } catch (NumberFormatException ex) {
                    continue;
                }

                EzadostasunKonfig cfg = ezadostasunKonfigRepository.findById(id).orElse(null);
                if (cfg == null) continue;

                // Ezabatzeko markatuta badago
                if (deleteIds.contains(id)) {
                    ezadostasunKonfigRepository.delete(cfg);
                    continue;
                }

                // ---- Eremu numerikoak irakurri ----
                String minBlokeStr      = request.getParameter("minBlokeak_" + id);
                String minOrduStr       = request.getParameter("minOrduak_" + id);
                String minBertaratzeStr = request.getParameter("minBertaratzea_" + id);
                String minGaindituStr   = request.getParameter("minGaindituak_" + id);

                cfg.setMinBlokePortzentaia(parseIntOrDefault(minBlokeStr, cfg.getMinBlokePortzentaia()));
                cfg.setMinOrduPortzentaia(parseIntOrDefault(minOrduStr, cfg.getMinOrduPortzentaia()));
                cfg.setMinBertaratzePortzentaia(parseIntOrDefault(minBertaratzeStr, cfg.getMinBertaratzePortzentaia()));
                cfg.setMinGaindituPortzentaia(parseIntOrDefault(minGaindituStr, cfg.getMinGaindituPortzentaia()));

                ezadostasunKonfigRepository.save(cfg);
            }
        }

        ra.addFlashAttribute("success", "Ezadostasun konfigurazioak gorde dira.");
        return "redirect:/kudeatzaile/konfigurazioa#ezadostasunak";
    }

    private int parseIntOrDefault(String val, Integer current) {
        if (val == null || val.isBlank()) {
            return current != null ? current : 0;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return current != null ? current : 0;
        }
    }

    /** 0–100 tartera mugatu, null bada default erabili. */
    private int clampPercent(Integer value, int defaultValue) {
        if (value == null) return defaultValue;
        int v = value;
        if (v < 0)   v = 0;
        if (v > 100) v = 100;
        return v;
    }

    // ===== DTO txikiak =====
    @Data
    public static class ActiveMailakForm {
        private List<Long> aktiboIds;
    }

    @Data
    public static class SortuMailaForm {
        @NotBlank private String kodea;
        private String izena;
        @NotNull private Integer ordena;
    }

    @Data
    public static class ActiveFamiliakForm {
        private List<Long> aktiboIds;
    }

    @Data
    public static class SortuFamiliaForm {
        @NotBlank private String izena;
    }

    @Data
    public static class SortuEbaluazioEgoeraForm {
        @NotBlank private String kodea;
        @NotBlank private String izena;
        private Boolean notaBeharDu = false;
    }
}
