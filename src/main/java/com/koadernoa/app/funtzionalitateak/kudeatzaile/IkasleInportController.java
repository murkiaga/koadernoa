package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import java.util.ArrayList;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.objektuak.egutegia.repository.MailaRepository;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.modulua.service.IkasleaService;
import com.koadernoa.app.objektuak.modulua.service.ModuloaService;
import com.koadernoa.app.objektuak.zikloak.entitateak.InportazioTxostena;
import com.koadernoa.app.objektuak.zikloak.repository.FamiliaRepository;
import com.koadernoa.app.objektuak.zikloak.service.InportazioZerbitzua;
import com.koadernoa.app.objektuak.zikloak.service.TaldeaService;
import com.koadernoa.app.objektuak.zikloak.service.ZikloaService;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/kudeatzaile/taldeak/")
@RequiredArgsConstructor
public class IkasleInportController {
	
	private final InportazioZerbitzua inportazioZerbitzua;
	private final IkasleaService ikasleaService;
	
	@PostMapping("{taldeaId}/inportatu-ikasleak")
	public String inportatuIkasleak(@PathVariable Long taldeaId,
            @RequestParam("fitxategia") MultipartFile fitxategia,
            RedirectAttributes ra) {
		try {
			InportazioTxostena tx = inportazioZerbitzua.inportatuTaldekoXlsx(taldeaId, fitxategia);
			
			// INPORTAZIOAREN ONDOREN: koaderno guztiak sinkronizatu
			IkasleaService.ImportResult syncRes = ikasleaService.syncKoadernoakTalderako(taldeaId);
			
			ra.addFlashAttribute("msg",
			"Inportazioa ondo: berriak=" + tx.getSortuak() +
			", eguneratuak=" + tx.getEguneratuak() +
			", baztertuak=" + tx.getBaztertuak() +
			(tx.getOharrak().isEmpty() ? "" : " (oharrak: " + String.join("; ", tx.getOharrak()) + ")") +
			" | Koadernoen sinkronizazioa: gehitu=" + syncRes.sortuak()
			);
		
		} catch (IllegalArgumentException e) {
			ra.addFlashAttribute("errorea", "Fitxategi baliogabea: " + e.getMessage());
		} catch (Exception e) {
			ra.addFlashAttribute("errorea", "Ezin izan da inportatu: " + e.getMessage());
		}
		return "redirect:/kudeatzaile/taldeak";
	}
	
}
