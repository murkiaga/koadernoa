package com.koadernoa.app.security;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.koadernoa.app.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.zikloak.entitateak.Familia;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class LoginController {
	
	private final IrakasleaRepository irakasleaRepository;

    @GetMapping({"/", "/login"})
    public String loginPage() {
        return "login"; // --> resources/templates/login.html
    }
    
    @GetMapping("/aukeratu-mintegia")
    public String aukeratuMintegiaForm(HttpSession session, Model model) {
        Long irakasleaId = (Long) session.getAttribute("irakasleaId");
        if (irakasleaId == null) {
            return "redirect:/irakasle"; //jadanik aukeratuta
        }

        model.addAttribute("familiaGuztiak", Familia.values());
        return "irakasleak/aukeratu-mintegia";
    }
    
    @PostMapping("/aukeratu-mintegia")
    public String gordeMintegia(@RequestParam("mintegia") Familia mintegia, HttpSession session) {
        Long irakasleaId = (Long) session.getAttribute("irakasleaId");
        if (irakasleaId != null) {
            Irakaslea irakaslea = irakasleaRepository.findById(irakasleaId).orElseThrow();
            irakaslea.setMintegia(mintegia);
            irakasleaRepository.save(irakaslea);
            session.removeAttribute("irakasleaId");
        }
        return "redirect:/irakasle";
    }
    
    
}
