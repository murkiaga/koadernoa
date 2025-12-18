package com.koadernoa.app.common.advice;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;

import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class KoloreakAdvice {

	private final AplikazioAukeraService aukService;
	
	@ModelAttribute
    public void gehituEbaluKoloreak(Model model) {
        String e1 = aukService.get(AplikazioAukeraService.EBAL1_KOLORE, "#b3d9ff");
        String e2 = aukService.get(AplikazioAukeraService.EBAL2_KOLORE, "#ffd699");
        String e3 = aukService.get(AplikazioAukeraService.EBAL3_KOLORE, "#b2f2bb");

        model.addAttribute("ebal1Kolore", e1);
        model.addAttribute("ebal2Kolore", e2);
        model.addAttribute("ebal3Kolore", e3);
    }
}
