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

import com.koadernoa.app.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.modulua.service.ModuloaService;
import com.koadernoa.app.zikloak.entitateak.Familia;
import com.koadernoa.app.zikloak.service.TaldeaService;
import com.koadernoa.app.zikloak.service.ZikloaService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/kudeatzaile/irakasleak")
public class IrakasleKudeatzaileController {
	
	private final IrakasleaRepository irakasleaRepository;
	
	@GetMapping({"","/"})
	public String zerrenda(Model model) {
	    List<Irakaslea> irakasleak = irakasleaRepository.findAll();
	    model.addAttribute("irakasleak", irakasleak);
	    model.addAttribute("familiaGuztiak", Familia.values()); //enum guztiak
	    return "kudeatzaile/irakasleak/index";
	}
	
	@PostMapping("/{id}/mintegia")
	public String aldatuMintegia(@PathVariable("id") Long id, @RequestParam("mintegia") Familia mintegia) {
	    Optional<Irakaslea> optional = irakasleaRepository.findById(id);
	    if (optional.isPresent()) {
	        Irakaslea irakaslea = optional.get();
	        irakaslea.setMintegia(mintegia);
	        irakasleaRepository.save(irakaslea);
	    }
	    return "redirect:/kudeatzaile/irakasleak";
	}
}
