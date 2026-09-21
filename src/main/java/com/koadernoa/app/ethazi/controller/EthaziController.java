package com.koadernoa.app.ethazi.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.ethazi.dto.EthaziForms.*;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.GaitasunMota;
import com.koadernoa.app.ethazi.service.EthaziService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/ethazi")
@RequiredArgsConstructor
public class EthaziController {
    private final EthaziService service;

    @ModelAttribute
    void aukerak(Model model) {
        model.addAttribute("zikloak", service.zikloak());
        model.addAttribute("motak", GaitasunMota.values());
    }
    @GetMapping({"", "/"})
    public String index() { return "redirect:/ethazi/gaitasunak"; }

    @GetMapping("/gaitasunak")
    public String gaitasunak(@RequestParam(required = false) Long zikloaId,
            @RequestParam(defaultValue = "TEKNIKOA") GaitasunMota mota, Model model) {
        model.addAttribute("zikloaId", zikloaId); model.addAttribute("mota", mota);
        model.addAttribute("errubrika", service.errubrika(zikloaId, mota));
        return "Ethazi/gaitasunak/index";
    }
    @GetMapping("/gaitasunak/berria")
    public String gaitasunBerria(@RequestParam(required = false) Long zikloaId,
            @RequestParam(defaultValue = "TEKNIKOA") GaitasunMota mota, Model model) {
        return gaitasunForm(null, service.gaitasunaForm(null, zikloaId, mota), model);
    }
    @GetMapping("/gaitasunak/{id}/editatu")
    public String gaitasunEditatu(@PathVariable Long id, Model model) {
        return gaitasunForm(id, service.gaitasunaForm(id, null, null), model);
    }
    private String gaitasunForm(Long id, GaitasunaForm form, Model model) {
        model.addAttribute("id", id); model.addAttribute("form", form);
        model.addAttribute("eredua", service.eredua(form.getZikloaId(), form.getMota()));
        model.addAttribute("gaitasuna", id == null ? null : service.gaitasuna(id));
        model.addAttribute("curriculum", service.curriculum(form.getZikloaId()));
        model.addAttribute("mailaIzenak", service.mailaIzenak(form.getZikloaId(), form.getMota()));
        return "Ethazi/gaitasunak/form";
    }
    @PostMapping({"/gaitasunak/berria", "/gaitasunak/{id}/editatu"})
    public String gaitasunGorde(@PathVariable(required = false) Long id, @ModelAttribute("form") GaitasunaForm form,
            BindingResult binding, Model model, RedirectAttributes flash) {
        try {
            balidatu(binding);
            Long saved = service.gordeGaitasuna(id, form);
            flash.addFlashAttribute("success", "Gaitasuna gorde da.");
            return "redirect:/ethazi/gaitasunak/" + saved + "/editatu";
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            model.addAttribute("error", mezua(ex)); return gaitasunForm(id, form, model);
        }
    }
    @PostMapping("/gaitasunak/{id}/ezabatu")
    public String gaitasunEzabatu(@PathVariable Long id, RedirectAttributes flash) {
        return egin(() -> service.ezabatuGaitasuna(id), "Gaitasuna ezabatu da.", "/gaitasunak", flash);
    }
    @PostMapping({"/gaitasunak/{id}/mailak/{mailaId}/adierazleak/berria",
            "/gaitasunak/{id}/mailak/{mailaId}/adierazleak/{adierazleaId}/editatu"})
    public String adierazleGorde(@PathVariable Long id, @PathVariable Long mailaId,
            @PathVariable(required = false) Long adierazleaId, @ModelAttribute AdierazleaForm form,
            BindingResult binding, Model model, RedirectAttributes flash) {
        try {
            balidatu(binding); service.gordeAdierazlea(id, mailaId, adierazleaId, form);
            flash.addFlashAttribute("success", "Lorpen-adierazlea gorde da.");
            return "redirect:/ethazi/gaitasunak/" + id + "/editatu#adierazleak";
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            model.addAttribute("error", mezua(ex)); model.addAttribute("failedForm", form);
            model.addAttribute("failedMailaId", mailaId); model.addAttribute("failedAdierazleaId", adierazleaId);
            return gaitasunForm(id, service.gaitasunaForm(id, null, null), model);
        }
    }
    @PostMapping("/gaitasunak/{id}/mailak/{mailaId}/adierazleak/{adierazleaId}/ezabatu")
    public String adierazleEzabatu(@PathVariable Long id, @PathVariable Long mailaId,
            @PathVariable Long adierazleaId, RedirectAttributes flash) {
        return egin(() -> service.ezabatuAdierazlea(id, mailaId, adierazleaId), "Lorpen-adierazlea ezabatu da.",
                "/gaitasunak/" + id + "/editatu#adierazleak", flash);
    }

    @GetMapping("/mailakatzeak")
    public String mailakatzeak(Model model) { model.addAttribute("ereduak", service.ereduak()); return "Ethazi/mailakatzeak/index"; }
    @GetMapping("/mailakatzeak/berria")
    public String ereduBerria(Model model) { return ereduForm(null, new EreduaForm(), model); }
    @GetMapping("/mailakatzeak/{id}/editatu")
    public String ereduEditatu(@PathVariable Long id, Model model) {
        var e = service.eredua(id); var form = new EreduaForm();
        form.setZikloaId(e.getZikloa().getId()); form.setMota(e.getMota()); form.setIzena(e.getIzena());
        return ereduForm(id, form, model);
    }
    private String ereduForm(Long id, EreduaForm form, Model model) {
        model.addAttribute("id", id); model.addAttribute("form", form);
        model.addAttribute("eredua", id == null ? null : service.eredua(id));
        return "Ethazi/mailakatzeak/form";
    }
    @PostMapping({"/mailakatzeak/berria", "/mailakatzeak/{id}/editatu"})
    public String ereduGorde(@PathVariable(required = false) Long id, @ModelAttribute("form") EreduaForm form,
            BindingResult binding, Model model, RedirectAttributes flash) {
        try {
            balidatu(binding); Long saved = service.gordeEredua(id, form);
            flash.addFlashAttribute("success", "Mailakatze eredua gorde da.");
            return "redirect:/ethazi/mailakatzeak/" + saved + "/editatu";
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            model.addAttribute("error", mezua(ex)); return ereduForm(id, form, model);
        }
    }
    @PostMapping("/mailakatzeak/{id}/ezabatu")
    public String ereduEzabatu(@PathVariable Long id, RedirectAttributes flash) {
        return egin(() -> service.ezabatuEredua(id), "Mailakatze eredua ezabatu da.", "/mailakatzeak", flash);
    }
    @PostMapping({"/mailakatzeak/{id}/mailak/berria", "/mailakatzeak/{id}/mailak/{mailaId}/editatu"})
    public String mailaGorde(@PathVariable Long id, @PathVariable(required = false) Long mailaId,
            @ModelAttribute MailaForm form, BindingResult binding, Model model, RedirectAttributes flash) {
        try {
            balidatu(binding); service.gordeMaila(id, mailaId, form);
            flash.addFlashAttribute("success", "Maila gorde da.");
            return "redirect:/ethazi/mailakatzeak/" + id + "/editatu";
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            model.addAttribute("error", mezua(ex)); model.addAttribute("failedMaila", form);
            model.addAttribute("failedMailaId", mailaId); return ereduEditatu(id, model);
        }
    }
    @PostMapping("/mailakatzeak/{id}/mailak/{mailaId}/ezabatu")
    public String mailaEzabatu(@PathVariable Long id, @PathVariable Long mailaId, RedirectAttributes flash) {
        return egin(() -> service.ezabatuMaila(id, mailaId), "Maila ezabatu da.", "/mailakatzeak/" + id + "/editatu", flash);
    }
    @PostMapping("/mailakatzeak/{id}/mailak/{mailaId}/mugitu")
    public String mailaMugitu(@PathVariable Long id, @PathVariable Long mailaId,
            @RequestParam int norabidea, RedirectAttributes flash) {
        return egin(() -> service.mugituMaila(id, mailaId, norabidea), "Mailen ordena aldatu da.", "/mailakatzeak/" + id + "/editatu", flash);
    }

    @GetMapping("/ikaskuntza-emaitzak")
    public String emaitzak(@RequestParam(required = false) Long zikloaId, @RequestParam(required = false) Long moduloaId, Model model) {
        model.addAttribute("zikloaId", zikloaId); model.addAttribute("moduloaId", moduloaId);
        model.addAttribute("moduluak", service.moduluak(zikloaId));
        model.addAttribute("emaitzak", service.emaitzak(zikloaId, moduloaId));
        return "Ethazi/ikaskuntza-emaitzak/index";
    }
    @GetMapping("/ikaskuntza-emaitzak/berria")
    public String emaitzaBerria(@RequestParam(required = false) Long zikloaId,
            @RequestParam(required = false) Long moduloaId, Model model) {
        var f = new EmaitzaForm(); f.setZikloaId(zikloaId); f.setModuloaId(moduloaId);
        return emaitzaForm(null, f, model);
    }
    @GetMapping("/ikaskuntza-emaitzak/{id}/editatu")
    public String emaitzaEditatu(@PathVariable Long id, Model model) { return emaitzaForm(id, service.emaitzaForm(id), model); }
    private String emaitzaForm(Long id, EmaitzaForm form, Model model) {
        model.addAttribute("id", id); model.addAttribute("form", form);
        model.addAttribute("moduluak", service.moduluak(form.getZikloaId()));
        return "Ethazi/ikaskuntza-emaitzak/form";
    }
    @PostMapping({"/ikaskuntza-emaitzak/berria", "/ikaskuntza-emaitzak/{id}/editatu"})
    public String emaitzaGorde(@PathVariable(required = false) Long id, @ModelAttribute("form") EmaitzaForm form,
            BindingResult binding, Model model, RedirectAttributes flash) {
        try {
            balidatu(binding); service.gordeEmaitza(id, form);
            flash.addFlashAttribute("success", "Ikaskuntza-emaitza gorde da.");
            return "redirect:/ethazi/ikaskuntza-emaitzak?zikloaId=" + form.getZikloaId() + "&moduloaId=" + form.getModuloaId();
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            model.addAttribute("error", mezua(ex)); return emaitzaForm(id, form, model);
        }
    }
    @PostMapping("/ikaskuntza-emaitzak/{id}/ezabatu")
    public String emaitzaEzabatu(@PathVariable Long id, RedirectAttributes flash) {
        var f = service.emaitzaForm(id);
        return egin(() -> service.ezabatuEmaitza(id), "Ikaskuntza-emaitza ezabatu da.",
                "/ikaskuntza-emaitzak?zikloaId=" + f.getZikloaId() + "&moduloaId=" + f.getModuloaId(), flash);
    }
    private void balidatu(BindingResult binding) {
        if (binding.hasErrors()) throw new IllegalArgumentException("Berrikusi formularioa: zenbaki edo aukera baliogabea.");
    }
    private String egin(Runnable action, String success, String destination, RedirectAttributes flash) {
        try { action.run(); flash.addFlashAttribute("success", success); }
        catch (IllegalArgumentException | DataIntegrityViolationException ex) { flash.addFlashAttribute("error", mezua(ex)); }
        return "redirect:/ethazi" + destination;
    }
    private String mezua(Exception ex) {
        return ex instanceof DataIntegrityViolationException
                ? "Ezin da eragiketa burutu: datuak erabiltzen ari dira edo erregistro bikoiztua dago." : ex.getMessage();
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public String ezDaAurkitu(IllegalArgumentException ex, RedirectAttributes flash) {
        flash.addFlashAttribute("error", ex.getMessage()); return "redirect:/ethazi/gaitasunak";
    }
}
