package com.koadernoa.app.funtzionalitateak.irakasle;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.koadernoa.app.ethazi.controller.ErronkaController;
import com.koadernoa.app.ethazi.entitateak.EthaziKoadernoa;
import com.koadernoa.app.ethazi.service.EthaziKoadernoaService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/** ETHAZI koadernotik irekitako errubriken irakasle-navbarra elikatzen du. */
@ControllerAdvice(assignableTypes = ErronkaController.class)
@RequiredArgsConstructor
public class EthaziEbaluazioaModelAttributes {

    private final IrakasleaService irakasleaService;
    private final EthaziKoadernoaService ethaziKoadernoaService;

    @ModelAttribute
    @Transactional(readOnly = true)
    public void prestatuKoadernoEbaluazioa(HttpServletRequest request, Authentication auth,
            @SessionAttribute(value = "ethaziKoadernoAktiboa", required = false) EthaziKoadernoa sessionKoadernoa,
            Model model) {
        if (!"true".equalsIgnoreCase(request.getParameter("koadernoEbaluazioa"))
                || auth == null || sessionKoadernoa == null || sessionKoadernoa.getId() == null) {
            model.addAttribute("koadernoEbaluazioa", false);
            return;
        }
        EthaziKoadernoa koadernoa = ethaziKoadernoaService.findById(sessionKoadernoa.getId());
        Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        if (koadernoa == null || !ethaziKoadernoaService.irakasleakBadaukaSarbidea(irakaslea, koadernoa)) {
            model.addAttribute("koadernoEbaluazioa", false);
            return;
        }

        model.addAttribute("koadernoEbaluazioa", true);
        model.addAttribute("ethaziKoadernoAktiboa", koadernoa);
        model.addAttribute("irakasleEthaziKoadernoAktiboak",
                ethaziKoadernoaService.lortuIrakaslearenKoadernoak(irakaslea));
        model.addAttribute("irakasleKoadernoAktiboak", irakaslea.getKoadernoak() == null ? List.of()
                : irakaslea.getKoadernoak().stream()
                        .filter(k -> k.getEgutegia() != null && k.getEgutegia().getIkasturtea() != null
                                && k.getEgutegia().getIkasturtea().isAktiboa())
                        .toList());
        model.addAttribute("ethaziEbaluazioErronkak",
                ethaziKoadernoaService.programazioaIkuspegia(koadernoa, null, irakaslea).erronkak().stream()
                        .filter(erronka -> !erronka.moduluAukerak().isEmpty())
                        .toList());
    }
}
