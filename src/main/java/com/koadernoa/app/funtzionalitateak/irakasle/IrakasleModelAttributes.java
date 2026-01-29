package com.koadernoa.app.funtzionalitateak.irakasle;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoaService;

import lombok.RequiredArgsConstructor;

@ControllerAdvice(basePackages = "com.koadernoa.app.funtzionalitateak.irakasle")
@RequiredArgsConstructor
public class IrakasleModelAttributes {

    private final IrakasleaService irakasleaService;
    private final KoadernoaService koadernoaService;

    // Lehenetsi: sesioan badago koadernoAktiboa, hura erabili; bestela lehen aktiboa
    @ModelAttribute("koadernoAktiboa")
    @Transactional(readOnly = true)
    public Koadernoa getKoadernoAktiboa(
            Authentication auth,
            @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa sessionKoadernoa) {

        if (sessionKoadernoa != null && sessionKoadernoa.getId() != null) {
            return sessionKoadernoa;
        }

        String emaila = (auth != null && auth.getPrincipal() instanceof OAuth2User oAuth2User)
                ? oAuth2User.getAttribute("email")
                : (auth != null ? auth.getName() : null);

        if (emaila == null) {
            return null;
        }

        var irakasleaOpt = irakasleaService.findOptionalByEmaila(emaila);
        if (irakasleaOpt.isEmpty()) {
            return null;
        }

        Irakaslea ir = irakasleaOpt.get();
        if (ir.getKoadernoak() == null) {
            return null;
        }

        return ir.getKoadernoak().stream()
                .filter(k -> k.getEgutegia() != null
                          && k.getEgutegia().getIkasturtea() != null
                          && k.getEgutegia().getIkasturtea().isAktiboa())
                .findFirst()
                .orElse(null);
    }

    @ModelAttribute("irakasleKoadernoAktiboak")
    @Transactional(readOnly = true)
    public List<Koadernoa> getKoadernoAktiboak(Authentication auth) {
        String emaila = (auth != null && auth.getPrincipal() instanceof OAuth2User oAuth2User)
                ? oAuth2User.getAttribute("email")
                : (auth != null ? auth.getName() : null);

        if (emaila == null) {
            return List.of();
        }

        var irakasleaOpt = irakasleaService.findOptionalByEmaila(emaila);
        if (irakasleaOpt.isEmpty()) {
            return List.of();
        }

        Irakaslea ir = irakasleaOpt.get();
        if (ir.getKoadernoak() == null) {
            return List.of();
        }

        return ir.getKoadernoak().stream()
                .filter(k -> k.getEgutegia() != null
                          && k.getEgutegia().getIkasturtea() != null
                          && k.getEgutegia().getIkasturtea().isAktiboa())
                .toList();
    }
    
    @ModelAttribute("navbarKontsultaModua")
    public boolean navbarKontsultaModua(
            @SessionAttribute(name = "koadernoAktiboa", required = false)
            Koadernoa koadernoAktiboa,
            Authentication auth) {

        // Koaderno aktiborik edo autentifikaziorik ez → ez gaude kontsulta moduan
        if (auth == null || koadernoAktiboa == null || koadernoAktiboa.getId() == null) {
            return false;
        }

        // Lehendik daukazun metodoa berrerabili, ez service berriro deitu:
        List<Koadernoa> aktiboak = getKoadernoAktiboak(auth);

        if (aktiboak == null || aktiboak.isEmpty()) {
            // Irakasle honek ez dauka ikasturte aktiboko koadernorik → 
            // kasu honetan, edozein koaderno "kanpokoa" izango litzateke → kontsulta modua = true
            return true;
        }

        Long currentId = koadernoAktiboa.getId();

        // true → koadernoAktiboa irakaslearen koaderno aktiboen artean EZ badago
        return aktiboak.stream().noneMatch(k -> currentId.equals(k.getId()));
    }
}
