package com.koadernoa.app.ethazi.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.ethazi.dto.EthaziForms.ErronkaForm;
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
        model.addAttribute("hizkuntzak", Hizkuntza.values());
    }

    @GetMapping({"", "/"})
    String erronkak(@RequestParam(required = false) Long zikloaId, Model model) {
        model.addAttribute("zikloaId", zikloaId);
        model.addAttribute("erronkak", service.erronkak(zikloaId));
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
        model.addAttribute("id", id);
        model.addAttribute("form", f);
        model.addAttribute("mailak", service.mailak());
        model.addAttribute("moduluak", ethazi.zikloak().stream().flatMap(z -> ethazi.moduluak(z.getId()).stream()).toList());
        return "Ethazi/erronkak/form";
    }

    @PostMapping({"/berria", "/{id}/editatu"})
    String gorde(@PathVariable(required = false) Long id,
            @ModelAttribute("form") ErronkaForm f, BindingResult binding, Model model, RedirectAttributes flash) {
        try {
            if (binding.hasErrors()) throw new IllegalArgumentException("Berrikusi datak eta aukerak.");
            service.gordeErronka(id, f);
            flash.addFlashAttribute("success", "Erronka gorde da.");
            return "redirect:/ethazi/erronkak?zikloaId=" + f.getZikloaId();
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return form(id, f, model);
        }
    }

    @PostMapping("/{id}/ezabatu")
    String ezabatu(@PathVariable Long id, RedirectAttributes flash) {
        Long z = service.erronka(id).getZikloa().getId();
        service.ezabatuErronka(id);
        flash.addFlashAttribute("success", "Erronka ezabatu da.");
        return "redirect:/ethazi/erronkak?zikloaId=" + z;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    String errorea(IllegalArgumentException ex, RedirectAttributes flash) {
        flash.addFlashAttribute("error", ex.getMessage());
        return "redirect:/ethazi/erronkak";
    }
}
