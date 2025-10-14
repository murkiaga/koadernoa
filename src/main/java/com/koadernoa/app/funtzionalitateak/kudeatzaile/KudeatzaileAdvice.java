package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@ControllerAdvice(basePackages = "com.koadernoa.app.funtzionalitateak.kudeatzaile")
public class KudeatzaileAdvice {

    private final IkasturteaRepository ikasturteaRepository;

    /** Kudeatzaileko orri GUZTIETAN egongo da erabilgarri */
    @ModelAttribute("ikasturteAktiboa")
    public Ikasturtea ikasturteAktiboa() {
        return ikasturteaRepository.findByAktiboaTrue().orElse(null);
    }

    /** Navbar-eko aktibo klaseetarako */
    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest req) {
        return req.getRequestURI();
    }
}
