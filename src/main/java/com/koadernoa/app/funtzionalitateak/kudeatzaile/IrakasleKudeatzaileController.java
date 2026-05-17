package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.service.IkasturteaService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.IrakasleOrdutegiLerroa;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.IrakasleOrdutegia;
import com.koadernoa.app.objektuak.ordutegiak.repository.IrakasleOrdutegiaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;
import com.koadernoa.app.objektuak.zikloak.repository.FamiliaRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/kudeatzaile/irakasleak")
public class IrakasleKudeatzaileController {
	
	private final IrakasleaRepository irakasleaRepository;
	private final FamiliaRepository familiaRepository;
    private final IkasturteaService ikasturteaService;
    private final IrakasleOrdutegiaRepository irakasleOrdutegiaRepository;
    private final KoadernoaRepository koadernoaRepository;
	
	@GetMapping({"","/"})
	public String zerrenda(Model model) {
	    List<Irakaslea> irakasleak = irakasleaRepository.findAll();
	    model.addAttribute("irakasleak", irakasleak);
	    model.addAttribute("familiaGuztiak", familiaRepository.findAll());
	    return "kudeatzaile/irakasleak/index";
	}

    @GetMapping("/{id}")
    public String fitxa(@PathVariable("id") Long id,
                        @RequestParam(name = "ikasturteaId", required = false) Long ikasturteaId,
                        Model model) {
        Irakaslea irakaslea = irakasleaRepository.findById(id).orElseThrow();
        List<Ikasturtea> ikasturteak = ikasturteaService.getAllOrderedDesc();
        Long selectedIkasturteaId = ikasturteaId != null
                ? ikasturteaId
                : ikasturteaService.getAktiboa().map(Ikasturtea::getId).orElse(null);

        IrakasleOrdutegia ordutegia = selectedIkasturteaId == null ? null
                : irakasleOrdutegiaRepository.findByIrakasleaIdAndIkasturteaId(id, selectedIkasturteaId).orElse(null);
        if (ordutegia != null && ordutegia.getLerroak() != null) {
            ordutegia.getLerroak().sort(Comparator
                    .comparingInt((IrakasleOrdutegiLerroa l) -> l.getAsteguna() != null ? l.getAsteguna().ordinal() : Integer.MAX_VALUE)
                    .thenComparingInt(l -> l.getOrduZenbakia() != null ? l.getOrduZenbakia() : Integer.MAX_VALUE));
        }

        List<Koadernoa> koadernoak = koadernoaRepository.findByIrakasleaAndIkasturteaWithRelations(id, selectedIkasturteaId);

        model.addAttribute("irakaslea", irakaslea);
        model.addAttribute("familiaGuztiak", familiaRepository.findAll());
        model.addAttribute("ikasturteak", ikasturteak);
        model.addAttribute("ikasturteaId", selectedIkasturteaId);
        model.addAttribute("ordutegia", ordutegia);
        model.addAttribute("koadernoak", koadernoak);
        return "kudeatzaile/irakasleak/fitxa";
    }
	
	@PostMapping("/{id}/mintegia")
    public String aldatuMintegia(@PathVariable("id") Long id,
                                 @RequestParam("mintegia") Long familiaId,
                                 @RequestParam(name = "ordutegiKodea", required = false) String ordutegiKodea) {
        eguneratuIrakaslea(id, familiaId, ordutegiKodea);
        return "redirect:/kudeatzaile/irakasleak";
    }

    @PostMapping("/{id}/fitxa")
    public String eguneratuFitxa(@PathVariable("id") Long id,
                                 @RequestParam("mintegia") Long familiaId,
                                 @RequestParam(name = "ordutegiKodea", required = false) String ordutegiKodea,
                                 @RequestParam(name = "ikasturteaId", required = false) Long ikasturteaId,
                                 RedirectAttributes ra) {
        eguneratuIrakaslea(id, familiaId, ordutegiKodea);
        ra.addFlashAttribute("success", "Irakaslearen datuak eguneratu dira.");
        String redirect = "redirect:/kudeatzaile/irakasleak/" + id;
        return ikasturteaId != null ? redirect + "?ikasturteaId=" + ikasturteaId : redirect;
    }

    private void eguneratuIrakaslea(Long id, Long familiaId, String ordutegiKodea) {
        Irakaslea irakaslea = irakasleaRepository.findById(id).orElseThrow();
        Familia familia = familiaRepository.findById(familiaId).orElseThrow();
        irakaslea.setMintegia(familia);
        irakaslea.setOrdutegiKodea(ordutegiKodea == null || ordutegiKodea.isBlank() ? null : ordutegiKodea.trim());
        irakasleaRepository.save(irakaslea);
    }
}
