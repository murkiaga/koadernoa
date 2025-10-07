package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.service.IkasturteaService;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoaService;
import com.koadernoa.app.objektuak.modulua.service.ModuloaService;
import com.koadernoa.app.objektuak.zikloak.service.TaldeaService;
import com.koadernoa.app.objektuak.zikloak.service.ZikloaService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/kudeatzaile/koadernoa")
@RequiredArgsConstructor
public class KoadernoaController {

	private final IkasturteaService ikasturteaService;
	private final KoadernoaService koadernoaService;
	
	@PostMapping("/sortu")
	public String sortuKoadernoak(@RequestParam("ikasturteaId") Long ikasturteaId) {
	    Ikasturtea ikasturtea = ikasturteaService.getById(ikasturteaId);
	    koadernoaService.sortuKoadernoakIkasturteBerrirako(ikasturtea);
	    return "redirect:/kudeatzaile/egutegia?ikasturteaId=" + ikasturteaId;
	}
}
