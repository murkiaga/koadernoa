package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.mezuak.service.MezuaService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/kudeatzaile/mezuak")
@RequiredArgsConstructor
public class MezuakController {

    private final MezuaService mezuaService;
    private final IrakasleaService irakasleaService;

    @GetMapping
    public String index(Authentication auth, Model model) {
        Irakaslea ir = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        model.addAttribute("mezuak", mezuaService.bidaliEtaJasotakoak(ir.getId()));
        return "kudeatzaile/mezuak/index";
    }
}
