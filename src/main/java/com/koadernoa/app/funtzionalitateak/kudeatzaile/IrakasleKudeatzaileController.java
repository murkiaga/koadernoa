package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.modulua.service.ModuloaService;
import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;
import com.koadernoa.app.objektuak.zikloak.repository.FamiliaRepository;
import com.koadernoa.app.objektuak.zikloak.service.TaldeaService;
import com.koadernoa.app.objektuak.zikloak.service.ZikloaService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/kudeatzaile/irakasleak")
public class IrakasleKudeatzaileController {
	
	private final IrakasleaRepository irakasleaRepository;
	private final FamiliaRepository familiaRepository;
	
	@GetMapping({"","/"})
	public String zerrenda(Model model) {
	    List<Irakaslea> irakasleak = irakasleaRepository.findAll();
	    model.addAttribute("irakasleak", irakasleak);
	    model.addAttribute("familiaGuztiak", familiaRepository.findAll());
	    return "kudeatzaile/irakasleak/index";
	}
	
	@PostMapping("/{id}/mintegia")
    public String aldatuMintegia(@PathVariable("id") Long id,
                                 @RequestParam("mintegia") Long familiaId) {
        Irakaslea irakaslea = irakasleaRepository.findById(id).orElseThrow();
        Familia familia = familiaRepository.findById(familiaId).orElseThrow();
        irakaslea.setMintegia(familia);
        irakasleaRepository.save(irakaslea);
        return "redirect:/kudeatzaile/irakasleak";
    }
}
