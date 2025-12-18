package com.koadernoa.app.funtzionalitateak.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
	
	private final AplikazioAukeraService aukService;

	@GetMapping({"","/"})
    public String editForm(Model model) {
        model.addAttribute("ebal1Kolore", aukService.get(AplikazioAukeraService.EBAL1_KOLORE, "#b3d9ff"));
        model.addAttribute("ebal2Kolore", aukService.get(AplikazioAukeraService.EBAL2_KOLORE, "#ffd699"));
        model.addAttribute("ebal3Kolore", aukService.get(AplikazioAukeraService.EBAL3_KOLORE, "#b2f2bb"));
        return "admin/index";
    }

    @PostMapping("/ebalu-koloreak")
    public String save(
            @RequestParam String ebal1Kolore,
            @RequestParam String ebal2Kolore,
            @RequestParam String ebal3Kolore) {

        aukService.set(AplikazioAukeraService.EBAL1_KOLORE, ebal1Kolore);
        aukService.set(AplikazioAukeraService.EBAL2_KOLORE, ebal2Kolore);
        aukService.set(AplikazioAukeraService.EBAL3_KOLORE, ebal3Kolore);

        return "redirect:/admin/?success";
    }
}
