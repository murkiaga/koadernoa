package com.koadernoa.app.ethazi.controller;

import org.springframework.stereotype.Controller;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.ethazi.dto.EthaziForms.ErronkaForm;
import com.koadernoa.app.ethazi.dto.EthaziForms.ErronkaErrubrikaForm;
import com.koadernoa.app.ethazi.dto.EthaziForms.ErronkaTaldeEsleipenForm;
import com.koadernoa.app.ethazi.service.EthaziService;
import com.koadernoa.app.ethazi.service.KinielaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/ethazi/erronkak")
@RequiredArgsConstructor
public class ErronkaController {
    private final KinielaService service;
    private final EthaziService ethazi;

    @ModelAttribute
    void aukerak(Model model) {
        model.addAttribute("zikloak", ethazi.zikloak());
        model.addAttribute("mailak", service.mailak());
        model.addAttribute("hizkuntzak", Hizkuntza.values());
        model.addAttribute("ikasturteak", service.ikasturteak());
        model.addAttribute("ikasturteAktiboaId", service.ikasturteAktiboaId());
    }

    @GetMapping({"", "/"})
    String erronkak(@RequestParam(required = false) Long zikloaId,
            @RequestParam(required = false) Long mailaId,
            @RequestParam(required = false) Hizkuntza hizkuntza,
            @RequestParam(required = false) Long ikasturteaId, Model model) {
        if (ikasturteaId == null) ikasturteaId = service.ikasturteAktiboaId();
        model.addAttribute("zikloaId", zikloaId);
        model.addAttribute("mailaId", mailaId);
        model.addAttribute("hizkuntza", hizkuntza);
        model.addAttribute("ikasturteaId", ikasturteaId);
        model.addAttribute("erronkak", service.erronkak(zikloaId, mailaId, hizkuntza, ikasturteaId));
        return "Ethazi/erronkak/index";
    }

    @GetMapping("/berria")
    String berria(@RequestParam(required = false) Long zikloaId, Model model) {
        var f = new ErronkaForm();
        f.setZikloaId(zikloaId);
        return form(null, f, model);
    }

    @GetMapping("/{id}/editatu")
    String editatu(@PathVariable Long id, Model model) {
        return form(id, service.form(id), model);
    }

    private String form(Long id, ErronkaForm f, Model model) {
        model.addAttribute("bertsioak", id == null ? java.util.List.of() : service.bertsioak(id));
        model.addAttribute("id", id);
        model.addAttribute("form", f);
        model.addAttribute("formIkasturtea", id == null
            ? service.ikasturteak().stream().filter(i -> i.isAktiboa()).findFirst().orElse(null)
            : service.erronka(id).getIkasturtea());
        model.addAttribute("mailak", service.mailak());
        model.addAttribute("moduluak", ethazi.zikloak().stream().flatMap(z -> ethazi.moduluak(z.getId()).stream()).toList());
        return "Ethazi/erronkak/form";
    }

    @PostMapping({"/berria", "/{id}/editatu"})
    String gorde(@PathVariable(required = false) Long id,
            @ModelAttribute("form") ErronkaForm f, BindingResult binding, Model model, RedirectAttributes flash) {
        try {
            if (binding.hasErrors()) throw new IllegalArgumentException("Berrikusi datak eta aukerak.");
            Long ikasturteaId = id == null ? service.ikasturteAktiboaId() : service.erronka(id).getIkasturtea().getId();
            service.gordeErronka(id, f);
            flash.addFlashAttribute("success", "Erronka gorde da.");
            return "redirect:/ethazi/erronkak?zikloaId=" + f.getZikloaId() + "&ikasturteaId=" + ikasturteaId;
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return form(id, f, model);
        }
    }

    @PostMapping("/{id}/bertsioa")
    String bertsioa(@PathVariable Long id, @RequestParam Hizkuntza hizkuntza, RedirectAttributes flash) {
        try {
            Long target = service.sortuBertsioa(id, hizkuntza);
            flash.addFlashAttribute("success", "Bertsioa prest dago. Itzuli izena eta deskribapena; datak eta moduluak bertsioen artean partekatzen dira.");
            return "redirect:/ethazi/erronkak/" + target + "/editatu";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/ethazi/erronkak/" + id + "/editatu";
        }
    }

    @PostMapping("/{id}/ezabatu")
    String ezabatu(@PathVariable Long id, RedirectAttributes flash) {
        var challenge = service.erronka(id);
        Long z = challenge.getZikloa().getId();
        Long year = challenge.getIkasturtea() == null ? null : challenge.getIkasturtea().getId();
        service.ezabatuErronka(id);
        flash.addFlashAttribute("success", "Erronka ezabatu da.");
        return "redirect:/ethazi/erronkak?zikloaId=" + z + (year == null ? "" : "&ikasturteaId=" + year);
    }

    @PostMapping("/{id}/inportatu")
    String inportatu(@PathVariable Long id, RedirectAttributes flash) {
        try {
            Long berria = service.inportatuErronka(id);
            flash.addFlashAttribute("success", "Erronka eta errubrikak uneko ikasturtera kopiatu dira. Taldeak ez dira kopiatu.");
            return "redirect:/ethazi/erronkak/" + berria + "/editatu";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/ethazi/erronkak";
        }
    }

    @GetMapping("/{id}/taldeak")
    String taldeak(@PathVariable Long id, @RequestParam(required = false) Long zikloaId,
            @RequestParam(required = false) Long mailaId, @RequestParam(required = false) Hizkuntza hizkuntza,
            @RequestParam(required = false) Long ikasturteaId, Model model) {
        model.addAttribute("erronka", service.erronka(id));
        model.addAttribute("taldeak", service.taldeak(id));
        model.addAttribute("ikasleak", service.taldeHautagaiak(id));
        model.addAttribute("form", service.taldeEsleipenForm(id));
        model.addAttribute("itzuliZikloaId", zikloaId); model.addAttribute("itzuliMailaId", mailaId);
        model.addAttribute("itzuliHizkuntza", hizkuntza); model.addAttribute("itzuliIkasturteaId", ikasturteaId);
        return "Ethazi/erronkak/taldeak";
    }

    @PostMapping("/{id}/taldeak/berria")
    String gehituTaldea(@PathVariable Long id, @RequestParam(required = false) Long zikloaId,
            @RequestParam(required = false) Long mailaId, @RequestParam(required = false) Hizkuntza hizkuntza,
            @RequestParam(required = false) Long ikasturteaId, RedirectAttributes flash) {
        service.gehituTaldea(id); flash.addFlashAttribute("success", "Talde berria sortu da.");
        return taldeetara(id, zikloaId, mailaId, hizkuntza, ikasturteaId);
    }

    @PostMapping("/{id}/taldeak/{taldeaId}/ezabatu")
    String ezabatuTaldea(@PathVariable Long id, @PathVariable Long taldeaId,
            @RequestParam(required = false) Long zikloaId, @RequestParam(required = false) Long mailaId,
            @RequestParam(required = false) Hizkuntza hizkuntza, @RequestParam(required = false) Long ikasturteaId,
            RedirectAttributes flash) {
        try { service.ezabatuTaldea(id, taldeaId); flash.addFlashAttribute("success", "Taldea ezabatu da."); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
        return taldeetara(id, zikloaId, mailaId, hizkuntza, ikasturteaId);
    }

    @PostMapping("/{id}/taldeak/gorde")
    String gordeTaldeak(@PathVariable Long id, @ModelAttribute("form") ErronkaTaldeEsleipenForm form,
            @RequestParam(required = false) Long zikloaId, @RequestParam(required = false) Long mailaId,
            @RequestParam(required = false) Hizkuntza hizkuntza, @RequestParam(required = false) Long ikasturteaId,
            RedirectAttributes flash) {
        try { service.gordeTaldeEsleipenak(id, form); flash.addFlashAttribute("success", "Ikasleen taldeak gorde dira."); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
        return taldeetara(id, zikloaId, mailaId, hizkuntza, ikasturteaId);
    }

    private String taldeetara(Long id, Long zikloaId, Long mailaId, Hizkuntza hizkuntza, Long ikasturteaId) {
        StringBuilder url = new StringBuilder("redirect:/ethazi/erronkak/").append(id).append("/taldeak?");
        if (zikloaId != null) url.append("zikloaId=").append(zikloaId).append('&');
        if (mailaId != null) url.append("mailaId=").append(mailaId).append('&');
        if (hizkuntza != null) url.append("hizkuntza=").append(hizkuntza).append('&');
        if (ikasturteaId != null) url.append("ikasturteaId=").append(ikasturteaId);
        return url.toString().replaceAll("[&?]$", "");
    }

    @GetMapping("/{id}/errubrikak")
    String errubrikak(@PathVariable Long id, @RequestParam(required = false) Long moduloaId,
            Authentication auth, Model model) {
        var erronka = service.erronka(id);
        var moduluak = erronka.getModuluak().stream()
            .filter(m -> service.erronkaKalifikatuDezake(auth, id, m.getId()))
            .sorted(java.util.Comparator.comparing(m -> m.getKodea() == null ? "" : m.getKodea(), String.CASE_INSENSITIVE_ORDER)).toList();
        if (moduluak.isEmpty()) throw new AccessDeniedException("Ez duzu erronka honetako moduluak kalifikatzeko baimenik.");
        if (moduloaId == null && !moduluak.isEmpty()) moduloaId = moduluak.get(0).getId();
        final Long hautatutakoModuloaId = moduloaId;
        if (moduluak.stream().noneMatch(m -> m.getId().equals(hautatutakoModuloaId))) {
            throw new AccessDeniedException("Ez duzu modulu hau ikusteko baimenik.");
        }
        model.addAttribute("erronka", erronka); model.addAttribute("moduluak", moduluak); model.addAttribute("moduloaId", moduloaId);
        model.addAttribute("errubrikaEditatuDezake", service.errubrikaEditatuDezake(auth));
        if (moduloaId != null) {
            var errubrika = service.errubrika(id, moduloaId);
            model.addAttribute("errubrika", errubrika);
            model.addAttribute("form", service.errubrikaForm(id, moduloaId));
            model.addAttribute("adierazleak", service.modulukoAdierazleak(id, moduloaId));
            model.addAttribute("taldeak", service.taldeak(id));
            model.addAttribute("errubrikaIturriak", service.errubrikaEditatuDezake(auth)
                && errubrika.getMailak().isEmpty() && errubrika.getEbidentziak().isEmpty()
                    ? service.errubrikaIturriak(id, moduloaId) : java.util.List.of());
        }
        return "Ethazi/erronkak/errubrikak";
    }

    @PostMapping("/{id}/errubrikak/{moduloaId}/gorde")
    String gordeErrubrika(@PathVariable Long id, @PathVariable Long moduloaId,
            @ModelAttribute("form") ErronkaErrubrikaForm form, RedirectAttributes flash) {
        try { service.gordeErrubrika(id, moduloaId, form); flash.addFlashAttribute("success", "Errubrika gorde da."); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
        return errubrikara(id, moduloaId);
    }

    @PostMapping("/{id}/errubrikak/{moduloaId}/inportatu")
    String inportatuErrubrika(@PathVariable Long id, @PathVariable Long moduloaId,
            @RequestParam Long iturriErrubrikaId, RedirectAttributes flash) {
        try {
            service.inportatuErrubrika(id, moduloaId, iturriErrubrikaId);
            flash.addFlashAttribute("success", "Errubrikaren kopia inportatu da.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return errubrikara(id, moduloaId);
    }

    @PostMapping("/{id}/errubrikak/{moduloaId}/notak")
    @ResponseBody
    ResponseEntity<java.util.Map<String, String>> gordeTaldeNota(@PathVariable Long id, @PathVariable Long moduloaId,
            @RequestParam Long ebidentziaId, @RequestParam Long taldeaId,
            @RequestParam(required = false) Long mailaId, Authentication auth) {
        try {
            service.gordeTaldeNota(id, moduloaId, ebidentziaId, taldeaId, mailaId, auth);
            return ResponseEntity.ok(java.util.Map.of("message", "Gordeta"));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of("message", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/errubrikak/{moduloaId}/oharrak")
    @ResponseBody
    ResponseEntity<java.util.Map<String, String>> gordeTaldeOharrak(@PathVariable Long id, @PathVariable Long moduloaId,
            @RequestParam Long ebidentziaId, @RequestParam Long taldeaId,
            @RequestParam(required = false) String ondoEgindakoak,
            @RequestParam(required = false) String hobetuBeharrekoak, Authentication auth) {
        try {
            service.gordeTaldeOharrak(id, moduloaId, ebidentziaId, taldeaId,
                ondoEgindakoak, hobetuBeharrekoak, auth);
            return ResponseEntity.ok(java.util.Map.of("message", "Oharrak gordeta"));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of("message", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/errubrikak/{moduloaId}/mailak/berria")
    String gehituMaila(@PathVariable Long id, @PathVariable Long moduloaId, RedirectAttributes flash) {
        service.gehituErrubrikaMaila(id, moduloaId); flash.addFlashAttribute("success", "Mailakatze berria sortu da.");
        return errubrikara(id, moduloaId);
    }

    @PostMapping("/{id}/errubrikak/{moduloaId}/mailak/{mailaId}/ezabatu")
    String ezabatuMaila(@PathVariable Long id, @PathVariable Long moduloaId, @PathVariable Long mailaId, RedirectAttributes flash) {
        try { service.ezabatuErrubrikaMaila(id, moduloaId, mailaId); flash.addFlashAttribute("success", "Mailakatzea ezabatu da."); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
        return errubrikara(id, moduloaId);
    }

    @PostMapping("/{id}/errubrikak/{moduloaId}/ebidentziak/berria")
    String gehituEbidentzia(@PathVariable Long id, @PathVariable Long moduloaId, RedirectAttributes flash) {
        service.gehituEbidentzia(id, moduloaId); flash.addFlashAttribute("success", "Ebidentzia berria sortu da.");
        return errubrikara(id, moduloaId);
    }

    @PostMapping("/{id}/errubrikak/{moduloaId}/ebidentziak/{ebidentziaId}/ezabatu")
    String ezabatuEbidentzia(@PathVariable Long id, @PathVariable Long moduloaId, @PathVariable Long ebidentziaId, RedirectAttributes flash) {
        try { service.ezabatuEbidentzia(id, moduloaId, ebidentziaId); flash.addFlashAttribute("success", "Ebidentzia ezabatu da."); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
        return errubrikara(id, moduloaId);
    }

    private String errubrikara(Long id, Long moduloaId) {
        return "redirect:/ethazi/erronkak/" + id + "/errubrikak?moduloaId=" + moduloaId;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    String errorea(IllegalArgumentException ex, RedirectAttributes flash) {
        flash.addFlashAttribute("error", ex.getMessage());
        return "redirect:/ethazi/erronkak";
    }
}
