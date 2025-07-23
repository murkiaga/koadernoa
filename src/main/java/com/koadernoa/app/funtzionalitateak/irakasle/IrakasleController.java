package com.koadernoa.app.funtzionalitateak.irakasle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.koadernoa.app.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.modulua.service.ModuloaService;
import com.koadernoa.app.zikloak.service.TaldeaService;
import com.koadernoa.app.zikloak.service.ZikloaService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle")
@RequiredArgsConstructor
public class IrakasleController {

	@Autowired
	private IrakasleaRepository irakasleaRepository;
	
    @GetMapping({"/", ""})
    public String dashboard(@AuthenticationPrincipal OAuth2User oauthUser, Model model) {
    	String email = oauthUser.getAttribute("email");

        //DB-tik irakaslea lortu
        Irakaslea irakaslea = irakasleaRepository.findByEmaila(email).orElseThrow();

        model.addAttribute("irakaslea", irakaslea);
        return "dashboard"; //dashboard.html orria renderizatzeko
    }
}
