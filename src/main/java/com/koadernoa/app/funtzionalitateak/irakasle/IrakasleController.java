package com.koadernoa.app.funtzionalitateak.irakasle;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

import com.koadernoa.app.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.egutegia.entitateak.Egutegia;
import com.koadernoa.app.egutegia.service.EgutegiaService;
import com.koadernoa.app.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.irakasleak.service.IrakasleaService;
import com.koadernoa.app.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.koadernoak.service.KoadernoaService;
import com.koadernoa.app.modulua.service.ModuloaService;
import com.koadernoa.app.zikloak.service.TaldeaService;
import com.koadernoa.app.zikloak.service.ZikloaService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle")
@RequiredArgsConstructor
@SessionAttributes("koadernoAktiboa") //<- KOADERNOA SESIOAN GORDE
public class IrakasleController {

	private final IrakasleaService irakasleaService;
	private final KoadernoaService koadernoaService;
	private final EgutegiaService egutegiaService;
	
	
	@GetMapping("/koaderno/{id}")
	public String hautatuKoadernoa(
	        @PathVariable Long id,
	        @RequestParam(value = "next", required = false) String next,
	        Model model) {
	    Koadernoa koadernoa = koadernoaService.findById(id);
	    if (koadernoa == null) {
	        return "redirect:/irakasle";
	    }

	    // Sesioan eguneratu
	    model.addAttribute("koadernoAktiboa", koadernoa);

	    // Open-redirect saihestu: barneko bideak bakarrik
	    if (next != null && next.startsWith("/irakasle") && !next.startsWith("/irakasle/koaderno/")) {
	        return "redirect:" + next;
	    }
	    return "redirect:/irakasle";
	}

	
    @GetMapping({"/old"})
    public String dashboard(@AuthenticationPrincipal OAuth2User oauthUser, Model model) {
    	String email = oauthUser.getAttribute("email");

        //DB-tik irakaslea lortu
    	Irakaslea irakaslea = irakasleaService.findByEmaila(email);

        model.addAttribute("irakaslea", irakaslea);
        return "dashboard"; //dashboard.html orria renderizatzeko
    }
    
    @GetMapping({"/", ""})
    public String index(Model model, Authentication auth, @ModelAttribute("koadernoAktiboa") Koadernoa koadernoAktiboa) {
    	String emaila = null;

        if (auth.getPrincipal() instanceof OAuth2User oAuth2User) {
            emaila = oAuth2User.getAttribute("email");
        } else {
            emaila = auth.getName(); // fallback, adib. test lokaletan
        }

        Irakaslea irakaslea = irakasleaService.findByEmaila(emaila);
        model.addAttribute("irakaslea", irakaslea);
        model.addAttribute("koadernoAktiboDago", koadernoAktiboa != null);

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

        return "irakasleak/egutegia/egutegi-fitxa";
    }
}
