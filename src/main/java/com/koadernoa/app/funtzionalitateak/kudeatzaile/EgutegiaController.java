package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import java.util.List;
import java.time.LocalDate;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunMota;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.repository.MailaRepository;
import com.koadernoa.app.objektuak.egutegia.service.EgutegiaService;
import com.koadernoa.app.objektuak.egutegia.service.IkasturteaService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/kudeatzaile/egutegia")
public class EgutegiaController {

	private final IkasturteaService ikasturteaService;
	private final EgutegiaService egutegiaService;
	
	private final MailaRepository mailaRepository;
	
	@ModelAttribute("mailak")
    public List<com.koadernoa.app.objektuak.egutegia.entitateak.Maila> loadMailak() {
        // Aukeratu zure repo metodoa; adib. aktibo + ordena:
        return mailaRepository.findAllByAktiboTrueOrderByOrdenaAscIzenaAsc();
        // edo, besterik ezean:
        // return mailaRepository.findAll(Sort.by("ordena").ascending().and(Sort.by("izena")));
    }

    
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

	    // NEW: mapak popup/JS-rako eta UI-rako
	    Map<String, String> motaMap = new java.util.HashMap<>();
	    Map<String, String> ordezkatuaMap = new java.util.HashMap<>();
	    Map<String, String> oharraMap = new java.util.HashMap<>();

	    // Tooltip-erako (zure template-ak th:title-n erabiltzen duena)
	    Map<String, String> deskribapenaMap = new java.util.HashMap<>();

	    if (egutegia.getEgunBereziak() != null) {
	        for (EgunBerezi eb : egutegia.getEgunBereziak()) {
	            if (eb.getData() == null) continue;

	            String key = eb.getData().toString();

	            if (eb.getMota() != null) {
	                motaMap.put(key, eb.getMota().name());
	            }
	            if (eb.getOrdezkatua() != null) {
	                ordezkatuaMap.put(key, eb.getOrdezkatua().name());
	            }

	            String oharra = eb.getDeskribapena();
	            if (oharra != null && !oharra.isBlank()) {
	                oharraMap.put(key, oharra.trim());
	            }

	            // Tooltip testua: mota (+ ordezkatua) + " — " + oharra
	            String base = "";
	            if (eb.getMota() != null) {
	                switch (eb.getMota()) {
	                    case JAIEGUNA -> base = "Jaieguna";
	                    case EZ_LEKTIBOA -> base = "Ez-lektiboa";
	                    case LEKTIBOA -> base = "Lektiboa";
	                    case ORDEZKATUA -> {
	                        base = "Ordezkatua";
	                        if (eb.getOrdezkatua() != null) base += " (" + eb.getOrdezkatua().name() + ")";
	                    }
	                }
	            }

	            String tooltip = base;
	            String oh = oharraMap.get(key);
	            if (oh != null) {
	                if (!tooltip.isBlank()) tooltip += " — ";
	                tooltip += oh;
	            }
	            if (!tooltip.isBlank()) {
	                deskribapenaMap.put(key, tooltip);
	            }
	        }
	    }

	    model.addAttribute("egutegia", egutegia);
	    model.addAttribute("ikasturtea", egutegia.getIkasturtea());
	    model.addAttribute("hilabeteka", hilabeteka);

	    model.addAttribute("klaseMap", klaseak);
	    model.addAttribute("motaMap", motaMap);
	    model.addAttribute("ordezkatuaMap", ordezkatuaMap);
	    model.addAttribute("oharraMap", oharraMap);
	    model.addAttribute("deskribapenaMap", deskribapenaMap);

	    return "kudeatzaile/egutegia/egutegi-fitxa";
	}

    
	@PostMapping("/aldatu")
	public String aldatuEgunMota(
	        @RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
	        @RequestParam("mota") EgunMota mota,
	        @RequestParam(value = "ordezkatua", required = false) Astegunak ordezkatua,
	        @RequestParam("egutegiaId") Long egutegiaId,
	        @RequestParam(required = false) String oharra,
	        @RequestParam(required = false) String anchor) {

	    Egutegia egutegia = egutegiaService.getById(egutegiaId);
	    egutegiaService.aldatuEgunMota(egutegia, data, mota, ordezkatua, oharra);

	    if (anchor != null && !anchor.isBlank()) {
	        return "redirect:/kudeatzaile/egutegia/" + egutegiaId + "#" + anchor;
	    }
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
	    model.addAttribute("mailak", mailaRepository.findAllByAktiboTrueOrderByOrdenaAscIzenaAsc());
	    return "kudeatzaile/egutegia/form";
	}

	@PostMapping("/gorde")
	public String gordeEgutegia(@ModelAttribute Egutegia egutegia) {
	    egutegiaService.sortuLektiboEgunak(egutegia); // datak eta lektiboak sortu
	    return "redirect:/kudeatzaile/egutegia?egutegiaId=" + egutegia.getId();
	}

}
