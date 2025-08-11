package com.koadernoa.app.funtzionalitateak.irakasle;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.koadernoa.app.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.irakasleak.service.IrakasleaService;
import com.koadernoa.app.koadernoak.entitateak.Koadernoa;

import lombok.RequiredArgsConstructor;

@ControllerAdvice(assignableTypes = {
    com.koadernoa.app.funtzionalitateak.irakasle.IrakasleController.class,
    com.koadernoa.app.funtzionalitateak.irakasle.IrakasleKoadernoaController.class
})
@RequiredArgsConstructor
public class IrakasleModelAttributes {
	private final IrakasleaService irakasleaService;

    @ModelAttribute("koadernoAktiboa")
    public Koadernoa getKoadernoAktiboa(Authentication auth) {
        String emaila = null;
        if (auth.getPrincipal() instanceof OAuth2User oAuth2User) {
            emaila = oAuth2User.getAttribute("email");
        } else {
            emaila = auth.getName();
        }
        Irakaslea irakaslea = irakasleaService.findByEmaila(emaila);
        return irakaslea.getKoadernoak().stream()
                .filter(k -> k.getEgutegia().getIkasturtea().isAktiboa())
                .findFirst()
                .orElse(null);
    }

    @ModelAttribute("irakasleKoadernoAktiboak")
    public List<Koadernoa> getKoadernoAktiboak(Authentication auth) {
        String emaila = null;
        if (auth.getPrincipal() instanceof OAuth2User oAuth2User) {
            emaila = oAuth2User.getAttribute("email");
        } else {
            emaila = auth.getName();
        }
        Irakaslea irakaslea = irakasleaService.findByEmaila(emaila);
        return irakaslea.getKoadernoak().stream()
                .filter(k -> k.getEgutegia().getIkasturtea().isAktiboa())
                .toList();
    }

}
