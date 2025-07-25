package com.koadernoa.app.funtzionalitateak.irakasle;

import java.security.Principal;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import com.koadernoa.app.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.irakasleak.service.IrakasleaService;
import com.koadernoa.app.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.koadernoak.service.KoadernoaService;
import com.koadernoa.app.modulua.service.ModuloaService;
import com.koadernoa.app.zikloak.service.TaldeaService;
import com.koadernoa.app.zikloak.service.ZikloaService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle")
@RequiredArgsConstructor
@SessionAttributes("koadernoAktiboa") //<- KOADERNOA SESIOAN GORDE
public class IrakasleController {

	private final IrakasleaService irakasleaService;
	private final KoadernoaService koadernoaService;
	
	@ModelAttribute("koadernoAktiboa")
	public Koadernoa getKoadernoAktiboa(Authentication auth) {
	    String emaila = null;
	    if (auth.getPrincipal() instanceof OAuth2User oAuth2User) {
	        emaila = oAuth2User.getAttribute("email");
	    } else {
	        emaila = auth.getName(); // fallback
	    }
	    Irakaslea irakaslea = irakasleaService.findByEmaila(emaila);
	    //Hemen, irakasleak ez baditu koadernoak, null itzuli (ez aktiborik)
	    return irakaslea.getKoadernoak().stream().findFirst().orElse(null);
	}
	
	@GetMapping("/koaderno/{id}")
    public String hautatuKoadernoa(@PathVariable Long id, Model model) {
        Koadernoa koadernoa = koadernoaService.findById(id);
        model.addAttribute("koadernoAktiboa", koadernoa); // eguneratu sesioa
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
}
