package com.koadernoa.app.funtzionalitateak.irakasle;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.security.core.Authentication;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunMota;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunaBista;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.service.EgutegiaService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Jarduera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.JardueraEditDto;
import com.koadernoa.app.objektuak.koadernoak.entitateak.JardueraSortuDto;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.denboralizazioa.FaltakBistaDTO;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoOrdutegiBlokeaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.AsistentziaService;
import com.koadernoa.app.objektuak.koadernoak.service.DenboralizazioFaltaService;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoaService;
import com.koadernoa.app.objektuak.koadernoak.service.ProgramazioTxantiloiService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle/denboralizazioa")
@RequiredArgsConstructor
@SessionAttributes("koadernoAktiboa") //<-KOADERNOA SESIOAN GORDE
public class DenboralizazioaController {
	
	private final EgutegiaService egutegiaService;
	private final AsistentziaService asistentziaService;
	private final KoadernoOrdutegiBlokeaRepository koadernoOrdutegiBlokeaRepository;
	private final KoadernoaService koadernoaService;
	private final DenboralizazioFaltaService denboralizazioFaltaService;
	private final ProgramazioTxantiloiService programazioTxantiloiService;
	private final IrakasleaService irakasleaService;

	@GetMapping({"",""})
	public String erakutsiHilabetekoDenboralizazioa(
	    @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoa,
	    @RequestParam(name="urtea", required=false) Integer urtea,
	    @RequestParam(name="hilabetea", required=false) Integer hilabetea,
	    @RequestParam(name="bista", required=false, defaultValue = "egutegia") String bista,
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
	    
	    Map<String, String> egunMap = egutegiaService.kalkulatuKlaseak(egutegia);
	    Map<String, String> deskribapenaMap = egutegia.getEgunBereziak().stream()
	            .collect(Collectors.toMap(
	                    eb -> eb.getData().toString(),
	                    EgunBerezi::getDeskribapena,
	                    (a, b) -> a
	            ));

	    // Hilabete + urtea euskaraz formatuta
	    String hilabeteUrtea = LocalDate.of(unekoUrtea, unekoHilabetea, 1)
	            .format(java.time.format.DateTimeFormatter
	            		.ofPattern("LLLL yyyy")
	            		.withLocale(new java.util.Locale("eu", "ES")));

	    // Hilabetearen muga-datak
	    LocalDate hasiera = LocalDate.of(unekoUrtea, unekoHilabetea, 1);
	    LocalDate amaiera = hasiera.withDayOfMonth(hasiera.lengthOfMonth());

	    // Lortu jarduerak
	    List<Jarduera> jarduerak = koadernoaService.lortuJarduerakDataTartean(koadernoa, hasiera, amaiera);

	    // Lortu asistentzia egunak
	    List<Astegunak> astegunak = koadernoOrdutegiBlokeaRepository
	            .findAstegunakByKoadernoaId(koadernoa.getId());
	    Set<Astegunak> blokAstegunak = new HashSet<>(astegunak);
	    Map<String, Boolean> asistentziaEgunMap =
	    		asistentziaService.kalkulatuAsistentziaEgunak(blokAstegunak, egutegia, unekoUrtea, unekoHilabetea);
	    
	    Map<LocalDate, List<Jarduera>> jardueraMap = jarduerak.stream()
	        .collect(Collectors.groupingBy(Jarduera::getData));

	    // Model atributuak (bi bistentzat komunak)
	    model.addAttribute("jardueraMap", jardueraMap);
	    model.addAttribute("egutegia", egutegia);
	    model.addAttribute("ikasturtea", egutegia.getIkasturtea());
	    model.addAttribute("hilabeteUrtea", hilabeteUrtea);
	    model.addAttribute("urtea", unekoUrtea);
	    model.addAttribute("hilabetea", unekoHilabetea);
	    model.addAttribute("asteak", asteak);
	    model.addAttribute("egunMap", egunMap);
	    model.addAttribute("asistentziaEgunMap", asistentziaEgunMap);
	    model.addAttribute("deskribapenaMap", deskribapenaMap);
	    model.addAttribute("bista", bista);
	    model.addAttribute("txantiloiAldia", dagoTxantiloiAldia(egutegia));

	    // FALTEN BISTA-rako datuak soilik bista == "faltak" denean
	    if ("faltak".equalsIgnoreCase(bista)) {
	        FaltakBistaDTO faltak = denboralizazioFaltaService
	                .kalkulatuFaltenBista(koadernoa, unekoHilabetea, unekoUrtea);

	        model.addAttribute("programaOrduak", faltak.getProgramaOrduak());
	        model.addAttribute("faltaEgunak", faltak.getEgunak());
	        model.addAttribute("faltaEgunOrduak", faltak.getEgunekoOrduak());
	        model.addAttribute("faltakIkasleak", faltak.getIkasleRows());
	    }
	    
	    //Egutegiko oharrak hartzeko:
	    Map<String, String> oharraMap = new HashMap<>();
	    if (egutegia.getEgunBereziak() != null) {
	        for (EgunBerezi eb : egutegia.getEgunBereziak()) {
	            if (eb.getData() == null) continue;
	            if (eb.getDeskribapena() == null || eb.getDeskribapena().isBlank()) continue;
	            oharraMap.put(eb.getData().toString(), eb.getDeskribapena().trim());
	        }
	    }
	    model.addAttribute("oharraMap", oharraMap);

	    return "irakasleak/denboralizazioa/denboralizazioa";
	}
	
	@PostMapping("/jarduera")
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

	private boolean dagoTxantiloiAldia(Egutegia egutegia) {
	    if (egutegia == null || egutegia.getEgunBereziak() == null) return false;
	    List<LocalDate> lektiboak = egutegia.getEgunBereziak().stream()
	        .filter(eb -> eb.getData() != null)
	        .filter(eb -> eb.getMota() == EgunMota.LEKTIBOA || eb.getMota() == EgunMota.ORDEZKATUA)
	        .map(EgunBerezi::getData)
	        .distinct()
	        .sorted()
	        .toList();
	    if (lektiboak.isEmpty()) return false;
	    int index = Math.max(0, lektiboak.size() - 10);
	    LocalDate muga = lektiboak.get(index);
	    return !LocalDate.now().isBefore(muga);
	}

	@PostMapping("/txantiloiak/sortu")
	public String sortuTxantiloia(
	    @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoa,
	    @RequestParam("urtea") int urtea,
	    @RequestParam("hilabetea") int hilabetea,
	    @RequestParam(value = "izena", required = false) String izena,
	    Authentication auth,
	    RedirectAttributes ra) {

	    if (koadernoa == null || koadernoa.getId() == null) {
	        ra.addFlashAttribute("error", "Ez dago koaderno aktiborik aukeratuta.");
	        return "redirect:/irakasle";
	    }

	    Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
	    if (!koadernoaService.irakasleakBadaukaSarbidea(irakaslea, koadernoa)) {
	        ra.addFlashAttribute("error", "Ez duzu baimenik txantiloia sortzeko.");
	        return "redirect:/irakasle/denboralizazioa?urtea=" + urtea + "&hilabetea=" + hilabetea;
	    }

	    try {
	        var txantiloi = programazioTxantiloiService.sortuTxantiloiDenboralizaziotik(koadernoa, irakaslea, izena);
	        ra.addFlashAttribute("success", "Txantiloia gorde da: " + txantiloi.getIzena());
	    } catch (IllegalArgumentException ex) {
	        ra.addFlashAttribute("error", "Ezin izan da txantiloia sortu: " + ex.getMessage());
	    }

	    return "redirect:/irakasle/denboralizazioa?urtea=" + urtea + "&hilabetea=" + hilabetea;
	}
	
	
	@GetMapping(path="/jarduera/{id}",
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
	
	@PostMapping("/jarduera/{id}/mugitu")
	@ResponseBody
	public ResponseEntity<?> mugituJarduera(
	        @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoa,
	        @PathVariable("id") Long id,
	        @RequestParam("data")
	        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataBerria) {

	    if (koadernoa == null || koadernoa.getId() == null) {
	        return ResponseEntity.status(HttpStatus.CONFLICT)
	                .body(Map.of("error", "Koaderno aktiboa falta da"));
	    }

	    try {
	        koadernoaService.aldatuJardueraData(koadernoa, id, dataBerria);
	        return ResponseEntity.ok(Map.of("ok", true));
	    } catch (IllegalArgumentException ex) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                .body(Map.of("error", ex.getMessage()));
	    }
	}
	
	
	@PostMapping("/jarduera/{id}/ezabatu")
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
