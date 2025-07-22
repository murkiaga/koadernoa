package com.koadernoa.app.common.controller.kudeatzaile;

import java.util.List;
import java.time.LocalDate;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.koadernoa.app.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.egutegia.entitateak.Astegunak;
import com.koadernoa.app.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.egutegia.entitateak.EgunMota;
import com.koadernoa.app.egutegia.entitateak.Egutegia;
import com.koadernoa.app.egutegia.service.EgutegiaService;
import com.koadernoa.app.egutegia.service.IkasturteaService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/kudeatzaile/egutegia")
public class EgutegiaController {

	private final IkasturteaService ikasturteaService;
	private final EgutegiaService egutegiaService;

    
	@GetMapping({"", "/"})
	public String erakutsiIkasturteAktibokoEgutegiak(Model model) {
	    Optional<Ikasturtea> aktiboaOpt = ikasturteaService.getAktiboa();

	    if (aktiboaOpt.isEmpty()) {
	        model.addAttribute("errorea", "Ez dago ikasturte aktiborik");
	        return "error/404";
	    }

	    Ikasturtea aktiboa = aktiboaOpt.get();
	    List<Egutegia> egutegiak = aktiboa.getEgutegiak();

	    model.addAttribute("ikasturtea", aktiboa);
	    model.addAttribute("egutegiak", egutegiak);

	    return "kudeatzaile/egutegia/zerrenda";
	}
	
	@GetMapping("/{egutegiaId}")
	public String erakutsiEgutegiBat(@PathVariable("egutegiaId") Long egutegiaId, Model model) {
	    Egutegia egutegia = egutegiaService.getById(egutegiaId);
	    if (egutegia == null) {
	        model.addAttribute("errorea", "Ez da egutegia aurkitu.");
	        return "error/404";
	    }

	    Map<String, List<List<LocalDate>>> hilabeteka = egutegiaService.prestatuHilabetekoEgutegiak(egutegia);
	    Map<String, String> klaseak = egutegiaService.kalkulatuKlaseak(egutegia);
	    Map<String, String> deskribapenaMap = egutegia.getEgunBereziak().stream()
	        .collect(Collectors.toMap(
	            eb -> eb.getData().toString(),
	            EgunBerezi::getDeskribapena,
	            (a, b) -> a
	        ));

	    model.addAttribute("egutegia", egutegia);
	    model.addAttribute("ikasturtea", egutegia.getIkasturtea());
	    model.addAttribute("hilabeteka", hilabeteka);
	    model.addAttribute("klaseMap", klaseak);
	    model.addAttribute("deskribapenaMap", deskribapenaMap);

	    return "kudeatzaile/egutegia/egutegi-fitxa";
	}

    
	@PostMapping("/aldatu")
	public String aldatuEgunMota(
	        @RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
	        @RequestParam("mota") EgunMota mota,
	        @RequestParam(value = "ordezkatua", required = false) Astegunak ordezkatua,
	        @RequestParam("egutegiaId") Long egutegiaId) {

	    Egutegia egutegia = egutegiaService.getById(egutegiaId);
	    egutegiaService.aldatuEgunMota(egutegia, data, mota, ordezkatua);

	    return "redirect:/kudeatzaile/egutegia/" + egutegiaId;
	}
	
	@GetMapping("/sortu")
	public String sortuForm(Model model) {
	    Ikasturtea aktiboa = ikasturteaService.getAktiboa().orElse(null);
	    if (aktiboa == null) {
	        model.addAttribute("errorea", "Ez dago ikasturte aktiborik");
	        return "error/404";
	    }

	    Egutegia egutegia = new Egutegia();
	    egutegia.setIkasturtea(aktiboa);

	    model.addAttribute("egutegia", egutegia);
	    return "kudeatzaile/egutegia/form";
	}

	@PostMapping("/gorde")
	public String gordeEgutegia(@ModelAttribute Egutegia egutegia) {
	    egutegiaService.sortuLektiboEgunak(egutegia); // datak eta lektiboak sortu
	    return "redirect:/kudeatzaile/egutegia?egutegiaId=" + egutegia.getId();
	}

}
