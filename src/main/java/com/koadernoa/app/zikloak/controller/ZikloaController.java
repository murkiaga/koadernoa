package com.koadernoa.app.zikloak.controller;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.koadernoa.app.zikloak.service.ZikloaService;
import com.koadernoa.app.zikloak.entitateak.ZikloMaila;
import com.koadernoa.app.zikloak.entitateak.Zikloa;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/zikloak")
public class ZikloaController {

    private final ZikloaService zikloaService;

    @GetMapping
    public String zikloakZerrenda(Model model) {
        model.addAttribute("zikloak", zikloaService.getAll());
        return "zikloak/zerrenda"; // resources/templates/zikloak/zerrenda.html
    }

    @GetMapping("/berria")
    public String zikloBerriaForm(Model model) {
        model.addAttribute("zikloa", new Zikloa());
        model.addAttribute("mailak", ZikloMaila.values());
        return "zikloak/berria"; // resources/templates/zikloak/berria.html
    }

    @PostMapping("/gorde")
    public String zikloaGorde(@ModelAttribute Zikloa zikloa) {
        zikloaService.save(zikloa);
        return "redirect:/zikloak";
    }

    @GetMapping("/editatu/{id}")
    public String zikloaEditatuForm(@PathVariable Long id, Model model) {
    	Optional<Zikloa> optionalZikloa = zikloaService.getById(id);
        if (optionalZikloa.isPresent()) {
            model.addAttribute("zikloa", optionalZikloa.get());
            model.addAttribute("mailak", ZikloMaila.values());
            return "zikloak/berria";
        } else {
            return "redirect:/zikloak?errorea=aurkitu_ez";
        }
    }

    @PostMapping("/ezabatu/{id}")
    public String zikloaEzabatu(@PathVariable Long id) {
        zikloaService.delete(id);
        return "redirect:/zikloak";
    }
}
