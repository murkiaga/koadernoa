package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.koadernoa.app.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.egutegia.service.IkasturteaService;
import com.koadernoa.app.koadernoak.service.KoadernoaService;
import com.koadernoa.app.modulua.service.ModuloaService;
import com.koadernoa.app.zikloak.service.TaldeaService;
import com.koadernoa.app.zikloak.service.ZikloaService;

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
