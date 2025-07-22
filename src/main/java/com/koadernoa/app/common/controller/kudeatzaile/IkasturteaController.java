package com.koadernoa.app.common.controller.kudeatzaile;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.koadernoa.app.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.egutegia.service.EgutegiaService;
import com.koadernoa.app.egutegia.service.IkasturteaService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/kudeatzaile/ikasturteak")
public class IkasturteaController {

	private final IkasturteaService ikasturteaService;

    // 1. Zerrenda
    @GetMapping
    public String ikasturteZerrenda(Model model) {
        List<Ikasturtea> ikasturteak = ikasturteaService.getAllOrderedDesc();
        model.addAttribute("ikasturteak", ikasturteak);
        return "kudeatzaile/ikasturtea/index";
    }

    // 2. Ikasturte fitxa eta egutegiak
    @GetMapping("/{id}")
    public String ikasturteFitxa(@PathVariable("id") Long id, Model model) {
        Ikasturtea ikasturtea = ikasturteaService.getById(id);
        model.addAttribute("ikasturtea", ikasturtea);
        return "kudeatzaile/ikasturtea/ikasturtea-fitxa";
    }

    // 3. Sortu form
    @GetMapping("/sortu")
    public String sortuForm(Model model) {
        model.addAttribute("ikasturtea", new Ikasturtea());
        return "kudeatzaile/ikasturtea/ikasturtea-form";
    }

    // 4. Gorde (sortu edo editatu)
    @PostMapping("/gorde")
    public String gorde(@ModelAttribute Ikasturtea ikasturtea) {
        ikasturteaService.save(ikasturtea);
        return "redirect:/kudeatzaile/ikasturteak";
    }
    
    @PostMapping("/aldatu-aktiboa")
    public String aldatuAktiboa(@RequestParam("ikasturteaId") Long ikasturteaId) {
        Ikasturtea ikasturtea = ikasturteaService.getById(ikasturteaId);
        boolean berria = !ikasturtea.isAktiboa();

        if (berria) {
            ikasturteaService.desaktibatuDenak(); // Beste guztiak desaktibatu
        }

        ikasturtea.setAktiboa(berria);
        ikasturteaService.save(ikasturtea);

        return "redirect:/kudeatzaile/ikasturteak";
    }
}
