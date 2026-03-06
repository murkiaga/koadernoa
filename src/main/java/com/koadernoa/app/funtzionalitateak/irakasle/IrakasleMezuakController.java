package com.koadernoa.app.funtzionalitateak.irakasle;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.mezuak.service.MezuaService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle/mezuak")
@RequiredArgsConstructor
public class IrakasleMezuakController {

    private final MezuaService mezuaService;
    private final IrakasleaService irakasleaService;

    @GetMapping
    public String index(Authentication auth, Model model) {
        Irakaslea ir = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        model.addAttribute("mezuak", mezuaService.jasotakoak(ir.getId()));
        return "irakasleak/mezuak/index";
    }


    @GetMapping("/{id}")
    public String fitxa(@PathVariable Long id,
                        Authentication auth,
                        Model model,
                        RedirectAttributes ra) {
        Irakaslea ir = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        try {
            model.addAttribute("mezua", mezuaService.markatuIrakurrita(id, ir.getId()));
            return "irakasleak/mezuak/fitxa";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/irakasle/mezuak";
        }
    }

}
