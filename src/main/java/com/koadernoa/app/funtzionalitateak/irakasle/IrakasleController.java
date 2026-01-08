package com.koadernoa.app.funtzionalitateak.irakasle;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.service.EgutegiaService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoOrdutegiBlokea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle")
@RequiredArgsConstructor
@SessionAttributes("koadernoAktiboa") //<- KOADERNOA SESIOAN GORDE
public class IrakasleController {

	private final IrakasleaService irakasleaService;
	private final KoadernoaRepository koadernoaRepository;
	private final EgutegiaService egutegiaService;
	private final IrakasleModelAttributes irakasleModelAttributes;
	
	private static final List<Astegunak> ASTE_ORDENA = List.of(
            Astegunak.ASTELEHENA, Astegunak.ASTEARTEA, Astegunak.ASTEAZKENA,
            Astegunak.OSTEGUNA, Astegunak.OSTIRALA
    );


	@GetMapping({"/", ""})
	public String index(Model model,
	                    Authentication auth,
	                    @SessionAttribute(name = "koadernoAktiboa", required = false)
	                    Koadernoa koadernoAktiboa) {

	    // 1) Irakaslearen emaila lortu
	    String emaila;
	    if (auth.getPrincipal() instanceof OAuth2User oAuth2User) {
	        emaila = oAuth2User.getAttribute("email");
	    } else {
	        emaila = auth.getName();
	    }

	    // 2) Irakaslea eta bere koaderno-zerrenda
	    Irakaslea irakaslea = irakasleaService.findByEmaila(emaila);
	    model.addAttribute("irakaslea", irakaslea);

	    // ⇒ HAU EGOKITU ZURE SERVICERA:
	    // navbarreko select-ean erakusten dituzun koaderno berdinak
	    List<Koadernoa> koadernoZerrenda =
	    		irakasleModelAttributes.getKoadernoAktiboak(auth);
	    model.addAttribute("irakasleKoadernoAktiboak", koadernoZerrenda);

	    // 3) Ez badago koaderno aktiborik, baina badago gutxienez bat zerrendan → lehenengora redirect
	    boolean sesioanDago = (koadernoAktiboa != null && koadernoAktiboa.getId() != null);

	    if (!sesioanDago) {
	        if (!koadernoZerrenda.isEmpty()) {
	            Long lehenId = koadernoZerrenda.get(0).getId();
	            // KoadernoaController-ek sartuko du sesioan, eta gero berriz etorriko gara /irakasle-ra
	            return "redirect:/irakasle/koadernoa/" + lehenId + "?next=/irakasle";
	        } else {
	            // benetan ez dauka koadernorik
	            model.addAttribute("koadernoAktiboDago", false);
	            model.addAttribute("koadernoAktiboa", null);
	            return "irakasleak/index";
	        }
	    }

	    // 4) Sesioan badago ID bat → DBtik kargatu ordutegiarekin
	    boolean koadernoAktiboDago = true;

	    var optKoaderno = koadernoaRepository.findWithOrdutegiaById(koadernoAktiboa.getId());
	    if (optKoaderno.isEmpty()) {
	        // Baliteke koaderno hau ezabatuta egotea; saiatu berriz lehenengora joaten
	        if (!koadernoZerrenda.isEmpty()) {
	            Long lehenId = koadernoZerrenda.get(0).getId();
	            return "redirect:/irakasle/koadernoa/" + lehenId + "?next=/irakasle";
	        } else {
	            koadernoAktiboDago = false;
	            model.addAttribute("koadernoAktiboa", null);
	            model.addAttribute("koadernoAktiboDago", false);
	            return "irakasleak/index";
	        }
	    }

	    Koadernoa koadernoa = optKoaderno.get();
	    model.addAttribute("koadernoAktiboa", koadernoa);

	    // 5) Ikasturtearen izena
	    String ikasturteaIzena =
	            (koadernoa.getEgutegia() != null &&
	             koadernoa.getEgutegia().getIkasturtea() != null)
	                    ? koadernoa.getEgutegia().getIkasturtea().getIzena()
	                    : "(ikasturterik ez)";
	    model.addAttribute("ikasturteaIzena", ikasturteaIzena);

	    // 6) Ordutegia (rows/cols/selected) — zure jatorrizko logika
	    model.addAttribute("rows", IntStream.rangeClosed(1, 12).boxed().toList());
	    model.addAttribute("cols", ASTE_ORDENA);

	    Set<String> selected = new HashSet<>();
	    if (koadernoa.getOrdutegiak() != null) {
	        for (KoadernoOrdutegiBlokea b : koadernoa.getOrdutegiak()) {
	            int col = ASTE_ORDENA.indexOf(b.getAsteguna()) + 1;
	            for (int s = b.getHasieraSlot(); s <= b.bukaeraSlot(); s++) {
	                selected.add(col + "-" + s);
	            }
	        }
	    }
	    model.addAttribute("selected", selected);
	    model.addAttribute("editable", false); // defektuz blokeatuta

	    model.addAttribute("koadernoAktiboDago", koadernoAktiboDago);
	    return "irakasleak/index";
	}
    
	@GetMapping("/egutegia")
	public String koadernoarenEgutegia(@ModelAttribute("koadernoAktiboa") Koadernoa koadernoa, Model model) {

	    if (koadernoa == null) {
	        model.addAttribute("errorea", "Ez dago koaderno aktiborik aukeratuta.");
	        return "error/404";
	    }

	    Egutegia egutegia = koadernoa.getEgutegia();

	    Map<String, List<List<LocalDate>>> hilabeteka = egutegiaService.prestatuHilabetekoEgutegiak(egutegia);
	    Map<String, String> klaseak = egutegiaService.kalkulatuKlaseak(egutegia);

	    Map<String, String> oharraMap = new java.util.HashMap<>();
	    Map<String, String> deskribapenaMap = new java.util.HashMap<>();

	    if (egutegia.getEgunBereziak() != null) {
	        for (EgunBerezi eb : egutegia.getEgunBereziak()) {
	            if (eb.getData() == null) continue;
	            String key = eb.getData().toString();

	            String oharra = eb.getDeskribapena();
	            if (oharra != null && !oharra.isBlank()) {
	                oharraMap.put(key, oharra.trim());
	            }

	            // Tooltip-a: mota + (ordezkatua) + — + oharra
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
	            if (oharraMap.containsKey(key)) {
	                if (!tooltip.isBlank()) tooltip += " — ";
	                tooltip += oharraMap.get(key);
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

	    // NEW
	    model.addAttribute("oharraMap", oharraMap);
	    model.addAttribute("deskribapenaMap", deskribapenaMap);

	    return "irakasleak/egutegia/egutegi-fitxa";
	}

}
