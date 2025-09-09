package com.koadernoa.app.funtzionalitateak.irakasle;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;

import com.koadernoa.app.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.egutegia.entitateak.EgunaBista;
import com.koadernoa.app.egutegia.entitateak.Egutegia;
import com.koadernoa.app.egutegia.service.EgutegiaService;
import com.koadernoa.app.irakasleak.entitateak.IrakasleUserDetails;
import com.koadernoa.app.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.irakasleak.service.IrakasleaService;
import com.koadernoa.app.koadernoak.entitateak.Jarduera;
import com.koadernoa.app.koadernoak.entitateak.JardueraEditDto;
import com.koadernoa.app.koadernoak.entitateak.JardueraSortuDto;
import com.koadernoa.app.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.koadernoak.entitateak.KoadernoaSortuDto;
import com.koadernoa.app.koadernoak.service.KoadernoaService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle")
@RequiredArgsConstructor
@SessionAttributes("koadernoAktiboa") //<-KOADERNOA SESIOAN GORDE
public class IrakasleKoadernoaController {
	
	private final IrakasleaService irakasleaService;
	private final KoadernoaService koadernoaService;
	private final EgutegiaService egutegiaService;

	@GetMapping("/koadernoa/berria")
	public String erakutsiFormularioa(Authentication auth, Model model) {
		Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        model.addAttribute("moduluak", koadernoaService.lortuErabilgarriDaudenModuluak(irakaslea));
        model.addAttribute("irakasleAukeragarriak", koadernoaService.lortuFamiliaBerekoIrakasleak(irakaslea));
        model.addAttribute("irakasleLogeatua", irakaslea);
        model.addAttribute("koadernoaDto", new KoadernoaSortuDto());
        return "irakasleak/koadernoa-sortu";
    }

	@PostMapping("/koadernoa/berria")
	public String sortuKoadernoa(@ModelAttribute KoadernoaSortuDto dto, Authentication auth) {
		Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
	    koadernoaService.sortuKoadernoa(dto, irakaslea);
	    return "redirect:/irakasle";
	}
	
	@GetMapping("/denboralizazioa")
	public String erakutsiHilabetekoDenboralizazioa(
	    @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoa,
	    @RequestParam(name="urtea", required=false) Integer urtea,
	    @RequestParam(name="hilabetea", required=false) Integer hilabetea,
	    Model model) {

	    if (koadernoa == null || koadernoa.getId() == null) {
	        model.addAttribute("errorea", "Ez dago koaderno aktiborik aukeratuta.");
	        return "error/404";
	    }

	    LocalDate orain = LocalDate.now();
	    int unekoUrtea = (urtea != null) ? urtea : orain.getYear();
	    int unekoHilabetea = (hilabetea != null) ? hilabetea : orain.getMonthValue();

	    Egutegia egutegia = koadernoa.getEgutegia();

	    List<LocalDate> egunGuztiak = egutegiaService.getEgunakHilabetekoBista(unekoUrtea, unekoHilabetea, egutegia);
		 // Asteen zerrenda prestatu (astelehenetik ostiralera)
	    List<List<EgunaBista>> asteak = new ArrayList<>();
	    List<EgunaBista> astea = new ArrayList<>();

	    for (LocalDate eguna : egunGuztiak) {
	        if (eguna.getDayOfWeek().getValue() >= 1 && eguna.getDayOfWeek().getValue() <= 5) {
	            boolean aktiboa = eguna.getMonthValue() == unekoHilabetea;
	            astea.add(new EgunaBista(eguna, aktiboa));
	            if (astea.size() == 5) {
	                asteak.add(new ArrayList<>(astea));
	                astea.clear();
	            }
	        } else if (eguna.getDayOfWeek().getValue() == 7) {
	            if (!astea.isEmpty()) {
	                while (astea.size() < 5) {
	                    astea.add(new EgunaBista(null, false));
	                }
	                asteak.add(new ArrayList<>(astea));
	                astea.clear();
	            }
	        }
	    }
	    if (!astea.isEmpty()) {
	        while (astea.size() < 5) {
	            astea.add(new EgunaBista(null, false));
	        }
	        asteak.add(new ArrayList<>(astea));
	    }
	    
	    Map<String, String> klaseMap = egutegiaService.kalkulatuKlaseak(egutegia);
	    Map<String, String> deskribapenaMap = egutegia.getEgunBereziak().stream()
	            .collect(Collectors.toMap(
	                    eb -> eb.getData().toString(),
	                    EgunBerezi::getDeskribapena,
	                    (a, b) -> a
	            ));

	    // Hilabete + urtea euskaraz formatuta
	    String hilabeteUrtea = LocalDate.of(unekoUrtea, unekoHilabetea, 1)
	            .format(java.time.format.DateTimeFormatter.ofPattern("LLLL yyyy").withLocale(new java.util.Locale("eu", "ES")));

	    
	    // Hilabetearen muga-datak
	    LocalDate hasiera = LocalDate.of(unekoUrtea, unekoHilabetea, 1);
	    LocalDate amaiera = hasiera.withDayOfMonth(hasiera.lengthOfMonth());

	    // Lortu jarduerak (gehitu metodo hau servicean)
	    List<Jarduera> jarduerak = koadernoaService.lortuJarduerakDataTartean(koadernoa, hasiera, amaiera);

	    Map<LocalDate, List<Jarduera>> jardueraMap = jarduerak.stream()
	        .collect(Collectors.groupingBy(Jarduera::getData));

	    model.addAttribute("jardueraMap", jardueraMap);
	    
	    model.addAttribute("egutegia", egutegia);
	    model.addAttribute("ikasturtea", egutegia.getIkasturtea());
	    model.addAttribute("hilabeteUrtea", hilabeteUrtea);
	    model.addAttribute("urtea", unekoUrtea);
	    model.addAttribute("hilabetea", unekoHilabetea);
	    model.addAttribute("asteak", asteak);
	    model.addAttribute("klaseMap", klaseMap);
	    model.addAttribute("deskribapenaMap", deskribapenaMap);

	    return "irakasleak/denboralizazioa";
	}
	
	@PostMapping("/denboralizazioa/jarduera")
	public String gordeEdoEguneratu(
	    @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoa,
	    @ModelAttribute JardueraSortuDto dto,
	    @RequestParam("urtea") int urtea,
	    @RequestParam("hilabetea") int hilabetea,
	    @RequestParam(value = "id", required = false) Long id) {

	    if (koadernoa == null || koadernoa.getId() == null) {
	        throw new IllegalStateException("Koaderno aktiborik ez");
	    }

	    if (id == null) koadernoaService.gordeJarduera(koadernoa, dto);
	    else koadernoaService.eguneratuJarduera(koadernoa, id, dto);

	    return "redirect:/irakasle/denboralizazioa?urtea=" + urtea + "&hilabetea=" + hilabetea;
	}
	
	
	@GetMapping(path="/denboralizazioa/jarduera/{id}",
	        produces=org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public org.springframework.http.ResponseEntity<?> lortuJarduera(
	    @PathVariable("id") Long id,
	    @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoa) {

	    if (koadernoa == null || koadernoa.getId() == null) {
	        return org.springframework.http.ResponseEntity.status(409)
	            .body(java.util.Map.of("error", "Koaderno aktiboa falta da"));
	    }

	    Jarduera j = koadernoaService.lortuJardueraKoadernoan(koadernoa, id);
	    if (j == null) {
	        return org.springframework.http.ResponseEntity.status(404)
	            .body(java.util.Map.of("error","Not found"));
	    }
	    return org.springframework.http.ResponseEntity.ok(JardueraEditDto.from(j));
	}
	
	
	@PostMapping("/denboralizazioa/jarduera/{id}/ezabatu")
	public String ezabatuJarduera(
	        @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoa,
	        @PathVariable("id") Long id,
	        @RequestParam("urtea") int urtea,
	        @RequestParam("hilabetea") int hilabetea) {

	    if (koadernoa == null || koadernoa.getId() == null) {
	        throw new IllegalStateException("Koaderno aktiborik ez");
	    }

	    koadernoaService.ezabatuJarduera(koadernoa, id); // id koaderno horren barrukoa dela balidatu!
	    return "redirect:/irakasle/denboralizazioa?urtea=" + urtea + "&hilabetea=" + hilabetea;
	}

}
