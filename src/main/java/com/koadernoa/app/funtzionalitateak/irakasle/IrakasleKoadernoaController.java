package com.koadernoa.app.funtzionalitateak.irakasle;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import com.koadernoa.app.irakasleak.entitateak.IrakasleUserDetails;
import com.koadernoa.app.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.irakasleak.service.IrakasleaService;
import com.koadernoa.app.koadernoak.entitateak.KoadernoaSortuDto;
import com.koadernoa.app.koadernoak.service.KoadernoaService;

import java.util.List;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle/koadernoa")
@RequiredArgsConstructor
@SessionAttributes("koadernoAktiboa") //<-KOADERNOA SESIOAN GORDE
public class IrakasleKoadernoaController {
	
	private final IrakasleaService irakasleaService;
	private final KoadernoaService koadernoaService;

	@GetMapping("/berria")
	public String erakutsiFormularioa(Authentication auth, Model model) {
		Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        model.addAttribute("moduluak", koadernoaService.lortuErabilgarriDaudenModuluak(irakaslea));
        model.addAttribute("irakasleAukeragarriak", koadernoaService.lortuFamiliaBerekoIrakasleak(irakaslea));
        model.addAttribute("irakasleLogeatua", irakaslea);
        model.addAttribute("koadernoaDto", new KoadernoaSortuDto());
        return "irakasleak/koadernoa-sortu";
    }

	@PostMapping("/berria")
	public String sortuKoadernoa(@ModelAttribute KoadernoaSortuDto dto, Authentication auth) {
		Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
	    koadernoaService.sortuKoadernoa(dto, irakaslea);
	    return "redirect:/irakasle";
	}

}
