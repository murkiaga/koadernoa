package com.koadernoa.app.ethazi.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.ethazi.dto.KronogramaJardueraForm;
import com.koadernoa.app.ethazi.service.KronogramaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/ethazi/erronkak/{erronkaId}/kronograma")
@RequiredArgsConstructor
public class KronogramaController {
    private final KronogramaService service;

    @GetMapping
    String kronograma(@PathVariable Long erronkaId,
            @RequestParam(required = false) Long zikloaId,
            @RequestParam(required = false) Long mailaId,
            @RequestParam(required = false) Hizkuntza hizkuntza,
            @RequestParam(required = false) Long ikasturteaId,
            Authentication auth, Model model) {
        model.addAttribute("kronograma", service.ikuspegia(erronkaId, auth));
        model.addAttribute("jardueraForm", new KronogramaJardueraForm());
        model.addAttribute("itzuliUrl", zerrendara(zikloaId, mailaId, hizkuntza, ikasturteaId));
        model.addAttribute("zikloaId", zikloaId);
        model.addAttribute("mailaId", mailaId);
        model.addAttribute("hizkuntza", hizkuntza);
        model.addAttribute("ikasturteaId", ikasturteaId);
        return "Ethazi/erronkak/kronograma";
    }

    @PostMapping("/jarduerak/gorde")
    String gorde(@PathVariable Long erronkaId,
            @ModelAttribute KronogramaJardueraForm jardueraForm,
            @RequestParam(required = false) Long zikloaId,
            @RequestParam(required = false) Long mailaId,
            @RequestParam(required = false) Hizkuntza hizkuntza,
            @RequestParam(required = false) Long ikasturteaId,
            Authentication auth, RedirectAttributes flash) {
        try {
            service.gorde(erronkaId, jardueraForm, auth);
            flash.addFlashAttribute("success", jardueraForm.getId() == null
                ? "Jarduera sortu da." : "Jarduera eguneratu da.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return kronogramara(erronkaId, zikloaId, mailaId, hizkuntza, ikasturteaId);
    }

    @PostMapping("/jarduerak/{jardueraId}/ezabatu")
    String ezabatu(@PathVariable Long erronkaId, @PathVariable Long jardueraId,
            @RequestParam(required = false) Long zikloaId,
            @RequestParam(required = false) Long mailaId,
            @RequestParam(required = false) Hizkuntza hizkuntza,
            @RequestParam(required = false) Long ikasturteaId,
            Authentication auth, RedirectAttributes flash) {
        try {
            service.ezabatu(erronkaId, jardueraId, auth);
            flash.addFlashAttribute("success", "Jarduera ezabatu da.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return kronogramara(erronkaId, zikloaId, mailaId, hizkuntza, ikasturteaId);
    }

    private String kronogramara(Long erronkaId, Long zikloaId, Long mailaId,
            Hizkuntza hizkuntza, Long ikasturteaId) {
        return "redirect:/ethazi/erronkak/" + erronkaId + "/kronograma"
            + query(zikloaId, mailaId, hizkuntza, ikasturteaId);
    }

    private String zerrendara(Long zikloaId, Long mailaId, Hizkuntza hizkuntza, Long ikasturteaId) {
        return "/ethazi/erronkak" + query(zikloaId, mailaId, hizkuntza, ikasturteaId);
    }

    private String query(Long zikloaId, Long mailaId, Hizkuntza hizkuntza, Long ikasturteaId) {
        java.util.List<String> params = new java.util.ArrayList<>();
        if (zikloaId != null) params.add("zikloaId=" + zikloaId);
        if (mailaId != null) params.add("mailaId=" + mailaId);
        if (hizkuntza != null) params.add("hizkuntza=" + hizkuntza.name());
        if (ikasturteaId != null) params.add("ikasturteaId=" + ikasturteaId);
        return params.isEmpty() ? "" : "?" + String.join("&", params);
    }
}
