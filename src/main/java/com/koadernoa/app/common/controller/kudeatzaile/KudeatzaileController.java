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

import com.koadernoa.app.zikloak.entitateak.ZikloMaila;
import com.koadernoa.app.modulua.entitateak.Moduloa;
import com.koadernoa.app.modulua.service.ModuloaService;
import com.koadernoa.app.zikloak.entitateak.Familia;
import com.koadernoa.app.zikloak.entitateak.Taldea;
import com.koadernoa.app.zikloak.entitateak.Zikloa;
import com.koadernoa.app.zikloak.service.TaldeaService;
import com.koadernoa.app.zikloak.service.ZikloaService;

import lombok.RequiredArgsConstructor;


@Controller
@RequestMapping("/kudeatzaile")
@RequiredArgsConstructor
public class KudeatzaileController {
	
	private final ZikloaService zikloaService;
	private final TaldeaService taldeaService;
    private final ModuloaService moduloaService;

	@GetMapping
    public String kudeatzaileDashboard(Model model) {
        return "kudeatzaile_dashboard";
    }
//----ZIKLOAK
	@GetMapping("/zikloak")
    public String erakutsiZikloak(Model model) {
        List<Zikloa> zikloak = zikloaService.getAll();
        model.addAttribute("zikloak", zikloak);
        return "kudeatzaile/zikloak/index";
    }
	
	@GetMapping("/zikloak/{id}")
	public String zikloarenFitxa(@PathVariable("id") Long id, Model model) {
	    Zikloa zikloa = zikloaService.getById(id).orElseThrow();
	    model.addAttribute("zikloa", zikloa);
	    model.addAttribute("taldeak", zikloa.getTaldeak());
	    return "kudeatzaile/zikloak/ziklo-fitxa";
	}
	
	@GetMapping("/zikloak/sortu")
    public String sortuZikloaForm(Model model) {
        model.addAttribute("zikloa", new Zikloa());
        model.addAttribute("mailak", ZikloMaila.values());
        model.addAttribute("familiak", Familia.values());
        return "kudeatzaile/zikloak/zikloa-form";
    }
	
	@GetMapping("/zikloak/editatu/{id}")
	public String editatuZikloa(@PathVariable("id") Long id, Model model) {
		Zikloa zikloa = zikloaService.getById(id)
			    .orElseThrow(() -> new IllegalArgumentException("Zikloa ez da aurkitu: " + id));
	    model.addAttribute("zikloa", zikloa);
	    model.addAttribute("mailak", ZikloMaila.values());
	    model.addAttribute("familiak", Familia.values());
	    return "kudeatzaile/zikloak/zikloa-form";
	}
	
	@PostMapping("/zikloak/gorde")
	public String gordeZikloa(@ModelAttribute Zikloa zikloa) {
	    zikloaService.save(zikloa);
	    return "redirect:/kudeatzaile/zikloak";
	}
	
//----TALDEAK
	@GetMapping("/taldeak")
    public String taldeZerrenda(Model model) {
        List<Taldea> taldeak = taldeaService.getAll();
        model.addAttribute("taldeak", taldeak);
        return "kudeatzaile/taldeak/index";
    }

    @GetMapping("/taldeak/sortu")
    public String taldeaSortuForm(Model model) {
        model.addAttribute("taldea", new Taldea());
        model.addAttribute("zikloak", zikloaService.getAll());
        return "kudeatzaile/taldeak/taldea-form";
    }

    @GetMapping("/taldeak/editatu/{id}")
    public String taldeaEditatuForm(@PathVariable("id") Long id, Model model) {
        Taldea taldea = taldeaService.getById(id).orElseThrow();
        model.addAttribute("taldea", taldea);
        model.addAttribute("zikloak", zikloaService.getAll());
        return "kudeatzaile/taldeak/taldea-form";
    }

    @PostMapping("/taldeak/gorde")
    public String gorde(@ModelAttribute Taldea taldea) {
        taldeaService.save(taldea);
        return "redirect:/kudeatzaile/taldeak";
    }

    @PostMapping("/taldeak/ezabatu/{id}")
    public String ezabatuTaldea(@PathVariable("id") Long id) {
        taldeaService.deleteById(id);
        return "redirect:/kudeatzaile/taldeak";
    }

	
//----MODULOAK
    @GetMapping("/moduloak")
    public String moduloZerrenda(@RequestParam(name = "taldeaId", required = false) Long taldeaId, Model model) {
    	List<Moduloa> moduluak;
        if (taldeaId != null) {
            moduluak = moduloaService.getByTaldeaId(taldeaId);
        } else {
            moduluak = moduloaService.getAll();
        }
        model.addAttribute("taldeaId", taldeaId);
        model.addAttribute("moduluak", moduluak);
        model.addAttribute("taldeak", taldeaService.getAll());
        return "kudeatzaile/moduloak/index";
    }
    
    @PostMapping("/moduloak/gorde")
    public String gordeModuloa(@ModelAttribute Moduloa moduloa) {
        moduloaService.save(moduloa);
        return "redirect:/kudeatzaile/moduloak";
    }

    @GetMapping("/moduloak/sortu")
    public String sortuModuloaForm(@RequestParam(name = "taldeaId", required = false) Long taldeaId, Model model) {
        Moduloa moduloa = new Moduloa();

        if (taldeaId != null) {
            Taldea taldea = taldeaService.getById(taldeaId).orElse(null);
            moduloa.setTaldea(taldea);
        }

        model.addAttribute("moduloa", moduloa);
        model.addAttribute("taldeak", taldeaService.getAll());
        return "kudeatzaile/moduloak/moduloa-form";
    }

    @GetMapping("/moduloak/editatu/{id}")
    public String editatuModuloa(@PathVariable("id") Long id, Model model) {
        Moduloa moduloa = moduloaService.getById(id).orElseThrow();
        model.addAttribute("moduloa", moduloa);
        model.addAttribute("taldeak", taldeaService.getAll());
        return "kudeatzaile/moduloak/moduloa-form";
    }

    @GetMapping("/moduloak/ezabatu/{id}")
    public String moduloaEzabatu(@PathVariable("id") Long id) {
        moduloaService.delete(id);
        return "redirect:/kudeatzaile/moduloak";
    }
}
