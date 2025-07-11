package com.koadernoa.app.common.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.koadernoa.app.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.irakasleak.repository.IrakasleaRepository;

@Controller
public class HomeController {

	@Autowired
	private IrakasleaRepository irakasleaRepository;
	
    @GetMapping("/irakasle")
    public String dashboard(@AuthenticationPrincipal OAuth2User oauthUser, Model model) {
    	String email = oauthUser.getAttribute("email");

        // DB-tik irakaslea lortu
        Irakaslea irakaslea = irakasleaRepository.findByEmaila(email).orElseThrow();

        model.addAttribute("irakaslea", irakaslea);
        return "dashboard"; //dashboard.html orria renderizatzeko
    }
}
