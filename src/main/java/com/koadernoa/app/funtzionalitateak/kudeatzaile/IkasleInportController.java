package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.objektuak.modulua.service.IkasleaService;
import com.koadernoa.app.objektuak.zikloak.entitateak.InportazioTxostena;
import com.koadernoa.app.objektuak.zikloak.service.InportazioZerbitzua;
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
            @RequestParam(name = "zikloaId", required = false) Long zikloaId,
            @RequestParam(name = "anchor", required = false) String anchor,
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
		StringBuilder target = new StringBuilder("redirect:/kudeatzaile/taldeak");
		if (zikloaId != null) {
			target.append("?zikloaId=").append(zikloaId);
		}
		if (anchor != null && !anchor.isBlank()) {
			target.append("#").append(UriUtils.encodePathSegment(anchor, java.nio.charset.StandardCharsets.UTF_8));
		}
		return target.toString();
	}
	
}
