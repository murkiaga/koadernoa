package com.koadernoa.app.ethazi.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.ethazi.entitateak.gaitasunak.GaitasunMota;
import com.koadernoa.app.ethazi.service.EthaziService;
import com.koadernoa.app.ethazi.service.KinielaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/ethazi/kiniela")
@RequiredArgsConstructor
public class KinielaController {
    private final KinielaService service;
    private final EthaziService ethazi;

    @ModelAttribute
    void aukerak(Model model) {
        model.addAttribute("zikloak", ethazi.zikloak());
        model.addAttribute("eu", com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza.EUSKARA);
        model.addAttribute("es", com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza.GAZTELERA);
        model.addAttribute("en", com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza.INGELERA);
    }

    @GetMapping({"", "/"})
    String kiniela(@RequestParam(required = false) Long zikloaId,
            @RequestParam(required = false) Hizkuntza hizkuntza, Model model) {
        model.addAttribute("zikloaId", zikloaId);
        model.addAttribute("hizkuntza", hizkuntza);
        model.addAttribute("moduluak", service.kiniela(zikloaId, hizkuntza));
        model.addAttribute("errubrikak", Arrays.stream(GaitasunMota.values())
                .map(m -> ethazi.errubrika(zikloaId, m)).toList());
        return "Ethazi/kinielak/index";
    }

    @PostMapping("/{ieId}/loturak")
    String loturak(@PathVariable Long ieId, @RequestParam Long zikloaId, @RequestParam(required=false) Long moduloaId,
            @RequestParam(required = false) Hizkuntza hizkuntza,
            @RequestParam(required = false) Set<Long> adierazleaIds, RedirectAttributes flash) {
        try {
            service.gordeLoturak(zikloaId, ieId, adierazleaIds == null ? Set.of() : adierazleaIds);
            flash.addFlashAttribute("success", "Loturak gorde dira.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/ethazi/kiniela?zikloaId=" + zikloaId
                + (hizkuntza == null ? "" : "&hizkuntza=" + hizkuntza)
                + (moduloaId == null ? "" : "#ie-" + moduloaId + "-" + ieId);
    }

    @PostMapping("/{ieId}/pisua")
    @ResponseBody
    ResponseEntity<Map<String, String>> pisua(@PathVariable Long ieId,
            @RequestParam Long zikloaId, @RequestParam Long adierazleaId, @RequestParam BigDecimal pisua) {
        try {
            service.gordePisua(zikloaId, ieId, adierazleaId, pisua);
            return ResponseEntity.ok(Map.of("message", "Pisua gorde da."));
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Ezin izan da pisua gorde. Berrikusi lotura eta pisua (0–100, bi hamartar)."));
        }
    }

    @PostMapping("/adierazleak/{id}/erronka")
    @ResponseBody
    ResponseEntity<Map<String, String>> erronka(@PathVariable Long id, @RequestParam Long zikloaId,
            @RequestParam Long ieId, @RequestParam Long moduloaId, @RequestParam Long erronkaId, @RequestParam boolean landuta) {
        try {
            service.gordeErronkaLotura(zikloaId, id, ieId, moduloaId, erronkaId, landuta);
            return ResponseEntity.ok(Map.of("message", "Hautaketa gorde da."));
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ezin izan da erronkaren hautaketa gorde."));
        }
    }

    @PostMapping("/adierazleak/{id}/oharra")
    @ResponseBody
    ResponseEntity<Map<String, String>> oharra(@PathVariable Long id, @RequestParam Long zikloaId,
            @RequestParam Long ieId, @RequestParam Long moduloaId, @RequestParam String oharra) {
        try {
            service.gordeOharra(zikloaId, id, ieId, moduloaId, oharra);
            return ResponseEntity.ok(Map.of("message", "Oharra gorde da."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    String errorea(IllegalArgumentException ex, RedirectAttributes flash) {
        flash.addFlashAttribute("error", ex.getMessage());
        return "redirect:/ethazi/kiniela";
    }
}
