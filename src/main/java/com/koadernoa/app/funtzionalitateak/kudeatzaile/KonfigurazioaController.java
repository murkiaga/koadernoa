package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioMomentua;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioMomentuaRepository;
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

        model.addAttribute("mailak", mailak);
        model.addAttribute("activeForm", mailaForm);
        model.addAttribute("sortuForm", new SortuMailaForm());

        model.addAttribute("familiak", familiak);
        model.addAttribute("activeFamiliaForm", familiaForm);
        model.addAttribute("sortuFamiliaForm", new SortuFamiliaForm());

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
        // azentu/diakritikoak kendu (oina sinplea; hobe Normalizer erabiliz)
        s = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // ez-alfanumerikoak '-' bihurtu
        s = s.replaceAll("[^a-z0-9]+", "-");
        // hasiera/amaiera '-' kendu
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

    // ===== DTO txikiak =====
    @Data public static class ActiveMailakForm { private List<Long> aktiboIds; }
    @Data public static class SortuMailaForm { @NotBlank private String kodea; private String izena; @NotNull private Integer ordena; }

    @Data public static class ActiveFamiliakForm { private List<Long> aktiboIds; }
    @Data public static class SortuFamiliaForm { @NotBlank private String izena; }
    
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

        List<EbaluazioMomentua> momentuak =
                ebaluazioMomentuaRepository.findByMailaAndAktiboTrueOrderByOrdenaAsc(maila);
        // Oharra: hemen soilik aktiboak kargatzen ari gara; nahi baduzu "guztiak" erakutsi,
        // egin beste metodo bat repository-n, edo aldatu hau.

        model.addAttribute("maila", maila);
        model.addAttribute("momentuak", momentuak);

        return "kudeatzaile/konfigurazioa/mailak-ebaluazioak";
    }

    // POST: bulk update + berriak gehitu + aukeratutakoen ezabaketa
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
        String[] idParamArray = request.getParameterValues("ids");
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

                // Ezabatzeko markatuta badago → delete eta kitto
                if (deleteIds.contains(id)) {
                    ebaluazioMomentuaRepository.delete(em);
                    continue;
                }

                String kodea = request.getParameter("kodea_" + id);
                String izena = request.getParameter("izena_" + id);
                String ordenaStr = request.getParameter("ordena_" + id);
                boolean aktibo = request.getParameter("aktibo_" + id) != null;

                em.setKodea(kodea != null ? kodea.trim() : null);
                em.setIzena(izena != null ? izena.trim() : null);
                em.setAktibo(aktibo);

                Integer ordena = em.getOrdena();
                if (ordenaStr != null && !ordenaStr.isBlank()) {
                    try {
                        ordena = Integer.valueOf(ordenaStr);
                    } catch (NumberFormatException ignored) {}
                }
                em.setOrdena(ordena);

                ebaluazioMomentuaRepository.save(em);
            }
        }

        // ======== BERRIA SORTU (baldin eta kode/izena ez badaude hutsik) =========
        String newKodea = request.getParameter("newKodea");
        String newIzena = request.getParameter("newIzena");
        String newOrdenaStr = request.getParameter("newOrdena");

        if (newKodea != null) newKodea = newKodea.trim();
        if (newIzena != null) newIzena = newIzena.trim();

        if (newKodea != null && !newKodea.isEmpty() &&
            newIzena != null && !newIzena.isEmpty()) {

            EbaluazioMomentua berria = new EbaluazioMomentua();
            berria.setMaila(maila);
            berria.setKodea(newKodea);
            berria.setIzena(newIzena);
            berria.setAktibo(true);

            Integer ordena = null;
            if (newOrdenaStr != null && !newOrdenaStr.isBlank()) {
                try {
                    ordena = Integer.valueOf(newOrdenaStr);
                } catch (NumberFormatException ignored) {}
            }
            berria.setOrdena(ordena);

            ebaluazioMomentuaRepository.save(berria);
        }

        redirectAttributes.addFlashAttribute("success", "Ebaluazio momentuak eguneratu dira.");
        return "redirect:/kudeatzaile/konfigurazioa/mailak/" + mailaId + "/ebaluazioak";
    }
}
