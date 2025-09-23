package com.koadernoa.app.funtzionalitateak.irakasle;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.irakasleak.service.IrakasleaService;
import com.koadernoa.app.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.koadernoak.entitateak.Programazioa;
import com.koadernoa.app.koadernoak.service.KoadernoaService;
import com.koadernoa.app.koadernoak.service.ProgramazioaService;
import org.springframework.security.core.Authentication;

import static com.koadernoa.app.security.SecurityUtils.isKudeatzailea;

import java.util.List;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle/programazioa")
@SessionAttributes("koadernoAktiboa")
@RequiredArgsConstructor
public class ProgramazioaController {

    private final ProgramazioaService programazioaService;
    private final IrakasleaService irakasleaService;
    private final KoadernoaService koadernoaService;
    

    @GetMapping
    public String index(
        @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoAktiboa,
        Authentication auth,
        Model model,
        RedirectAttributes ra
    ) {
        if (koadernoAktiboa == null || koadernoAktiboa.getId() == null) {
            ra.addFlashAttribute("error", "Ez dago koaderno aktiborik aukeratuta.");
            return "redirect:/irakasle";
        }

        var programazioa = programazioaService.getOrCreateForKoadernoa(koadernoAktiboa);
        model.addAttribute("programazioa", programazioa);
        model.addAttribute("unitateak", programazioa.getUnitateak());
        return "irakasleak/programazioa/index";
    }

    // ---------- UD CRUD ----------
    @PostMapping("/ud")
    public String sortuUd(
        @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoAktiboa,
        @RequestParam String kodea,
        @RequestParam String izenburua,
        @RequestParam(defaultValue = "0") int orduak,
        @RequestParam(defaultValue = "0") int posizioa,
        RedirectAttributes ra
    ) {
        if (koadernoAktiboa == null || koadernoAktiboa.getId() == null) {
            ra.addFlashAttribute("error", "Koaderno aktiboa falta da.");
            return "redirect:/irakasle";
        }
        Programazioa p = programazioaService.getOrCreateForKoadernoa(koadernoAktiboa);
        programazioaService.addUd(p.getId(), kodea, izenburua, orduak, posizioa);
        ra.addFlashAttribute("ok", "UD sortuta.");
        return "redirect:/irakasle/programazioa";
    }

    public String eguneratuUd(@PathVariable Long udId,
            @RequestParam String kodea,
            @RequestParam String izenburua,
            @RequestParam int orduak,
            @RequestParam int posizioa,
            @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa koadernoa,
            Authentication auth,
            RedirectAttributes ra) {
		if (koadernoa == null || koadernoa.getId() == null) {
		ra.addFlashAttribute("error", "Koaderno aktiboa falta da.");
		return "redirect:/irakasle";
		}
		Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
		if (!isKudeatzailea(auth) && !koadernoaService.irakasleakBadaukaSarbidea(irakaslea, koadernoa)) {
		ra.addFlashAttribute("error", "Ez duzu baimenik ekintza honetarako.");
		return "redirect:/irakasle";
		}
		// 🛡️ Ziurtatu UD hori koaderno aktibo honen programazioarena dela
		if (!programazioaService.udDagokioKoadernoari(udId, koadernoa.getId())) {
		ra.addFlashAttribute("error", "UD ez dator bat koaderno aktiboarekin.");
		return "redirect:/irakasle/programazioa";
		}
		
		programazioaService.updateUd(udId, kodea, izenburua, orduak, posizioa);
		ra.addFlashAttribute("ok", "UD eguneratua.");
		return "redirect:/irakasle/programazioa";
	}

    @PostMapping("/ud/{udId}/ezabatu")
    public String ezabatuUd(@PathVariable Long udId, RedirectAttributes ra) {
        programazioaService.deleteUd(udId);
        ra.addFlashAttribute("ok", "UD ezabatuta.");
        return "redirect:/irakasle/programazioa";
    }

    // ---------- JardueraPlanifikatua CRUD ----------
    @PostMapping("/ud/{udId}/planifikatua")
    public String sortuPlanifikatua(
        @PathVariable Long udId,
        @RequestParam String izenburua,
        @RequestParam(defaultValue = "0") int orduak,
        RedirectAttributes ra
    ) {
        programazioaService.addJardueraPlanifikatua(udId, izenburua, orduak);
        ra.addFlashAttribute("ok", "Jarduera planifikatua sortuta.");
        return "redirect:/irakasle/programazioa";
    }

    @PostMapping("/planifikatuak/{jpId}/eguneratu")
    public String eguneratuPlanifikatua(
        @PathVariable Long jpId,
        @RequestParam String izenburua,
        @RequestParam int orduak,
        RedirectAttributes ra
    ) {
        programazioaService.updateJardueraPlanifikatua(jpId, izenburua, orduak);
        ra.addFlashAttribute("ok", "Jarduera planifikatua eguneratua.");
        return "redirect:/irakasle/programazioa";
    }

    @PostMapping("/planifikatuak/{jpId}/ezabatu")
    public String ezabatuPlanifikatua(@PathVariable Long jpId, RedirectAttributes ra) {
        programazioaService.deleteJardueraPlanifikatua(jpId);
        ra.addFlashAttribute("ok", "Jarduera planifikatua ezabatuta.");
        return "redirect:/irakasle/programazioa";
    }
}
