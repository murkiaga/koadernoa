package com.koadernoa.app.funtzionalitateak.irakasle;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.objektuak.koadernoak.entitateak.EstatistikaEbaluazioan;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.service.EstatistikaService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle/estatistikak")
@RequiredArgsConstructor
@SessionAttributes("koadernoAktiboa") //<-KOADERNOA SESIOAN GORDE
public class EstatistikakController {
	
	private final EstatistikaService estatistikaService;

	
	@GetMapping({"/",""})
    public String estatistikakPantaila(
            @SessionAttribute(name = "koadernoAktiboa", required = false) Koadernoa koadernoAktiboa,
            Model model) {

        if (koadernoAktiboa == null || koadernoAktiboa.getId() == null) {
            model.addAttribute("koadernoAktiboDago", false);
            return "irakasleak/estatistikak/index";
        }

        List<EstatistikaEbaluazioan> estatistikak =
                estatistikaService.lortuKoadernoarenEstatistikak(koadernoAktiboa);

        model.addAttribute("koadernoAktiboDago", true);
        model.addAttribute("koadernoAktiboa", koadernoAktiboa);
        model.addAttribute("estatistikak", estatistikak);

        return "irakasleak/estatistikak/index";
    }
	
	@PostMapping("/{estatId}/berkalkulatu")
    @ResponseBody
    public Map<String, Object> berkalkulatuEstatistika(
            @PathVariable Long estatId,
            @SessionAttribute("koadernoAktiboa") Koadernoa koadernoAktiboa) {

        estatistikaService.berkalkulatuEstatistika(koadernoAktiboa, estatId);

        // fetch bidez deituko dugu; JSON txiki bat bueltatu
        Map<String, Object> resp = new HashMap<>();
        resp.put("ok", true);
        return resp;
    }
	
	@PostMapping("/eguneratu")
    @Transactional
    public String eguneratuEstatistikak(
            @SessionAttribute("koadernoAktiboa") Koadernoa koadernoAktiboa,
            HttpServletRequest request,
            RedirectAttributes ra) {

        if (koadernoAktiboa == null || koadernoAktiboa.getId() == null) {
            ra.addFlashAttribute("error", "Ez dago koaderno aktiborik.");
            return "redirect:/irakasle/estatistikak";
        }

        List<EstatistikaEbaluazioan> estatistikak =
                estatistikaService.lortuKoadernoarenEstatistikak(koadernoAktiboa);

        for (EstatistikaEbaluazioan est : estatistikak) {
            Long id = est.getId();
            String udEmandaStr   = request.getParameter("unitateakEmanda_" + id);
            String orduEmandaStr = request.getParameter("orduakEmanda_" + id);

            if (udEmandaStr != null && !udEmandaStr.isBlank()) {
                try {
                    est.setUnitateakEmanda(Integer.parseInt(udEmandaStr));
                } catch (NumberFormatException ignored) {}
            }
            if (orduEmandaStr != null && !orduEmandaStr.isBlank()) {
                try {
                    est.setOrduakEmanda(Integer.parseInt(orduEmandaStr));
                } catch (NumberFormatException ignored) {}
            }
            est.setAzkenKalkulua(LocalDateTime.now());
        }

        // batch gordeta
        // (estatRepo.saveAll(estatistikak) edo @Transactional + entity-managed nahikoa)
        ra.addFlashAttribute("success", "Estatistikak eguneratu dira.");
        return "redirect:/irakasle/estatistikak";
    }
}
