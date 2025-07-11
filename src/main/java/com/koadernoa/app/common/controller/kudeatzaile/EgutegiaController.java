package com.koadernoa.app.common.controller.kudeatzaile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.time.LocalDate;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.koadernoa.app.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.egutegia.entitateak.EgunMota;
import com.koadernoa.app.egutegia.service.IkasturteaService;

@Controller
@RequestMapping("/kudeatzaile/egutegia")
public class EgutegiaController {

	private final IkasturteaService ikasturteaService;

    public EgutegiaController(IkasturteaService ikasturteaService) {
        this.ikasturteaService = ikasturteaService;
    }
    
    @GetMapping({"","/"})
    public String erakutsiEgutegiaNagusia(Model model) {
        Ikasturtea ikasturtea = ikasturteaService.getAktiboakByMaila(1).stream().findFirst().orElse(null);
        if (ikasturtea == null) {
            return "kudeatzaile/egutegia/hutsik";
        }

        Map<String, List<List<LocalDate>>> hilabeteka = ikasturteaService.prestatuHilabetekoEgutegiak(ikasturtea);
        Map<String, String> klaseak = ikasturteaService.kalkulatuKlaseak(ikasturtea);

        model.addAttribute("ikasturtea", ikasturtea);
        model.addAttribute("hilabeteka", hilabeteka);
        model.addAttribute("klaseMap", klaseak);

        return "kudeatzaile/egutegia/index";
    }
    
    @PostMapping("/aldatu")
    public String aldatuEgunMota(@RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                 @RequestParam("mota") EgunMota mota) {
        Ikasturtea ikasturtea = ikasturteaService.getAktiboakByMaila(1).stream().findFirst().orElse(null);
        if (ikasturtea != null) {
            ikasturteaService.aldatuEgunMota(ikasturtea, data, mota);
        }
        return "redirect:/kudeatzaile/egutegia";
    }


    @GetMapping("/sortu")
    public String sortuEgutegia(Model model) {
    	Ikasturtea berria = new Ikasturtea();
        model.addAttribute("ikasturtea", berria);
        model.addAttribute("egunMotak", EgunMota.values());
        return "kudeatzaile/egutegia/egutegia-sortu-form";
    }

    @PostMapping("/gorde")
    public String gordeIkasturtea(@ModelAttribute Ikasturtea ikasturtea) {
        //Egunak sortu (astebururik gabe)
        List<EgunBerezi> lektiboak = new ArrayList<>();
        LocalDate hasiera = ikasturtea.getHasieraData();
        LocalDate bukaera = ikasturtea.getBukaeraData();

        for (LocalDate date = hasiera; !date.isAfter(bukaera); date = date.plusDays(1)) {
            if (date.getDayOfWeek().getValue() < 6) { // 1-5: astelehena-ostirala
                EgunBerezi eb = new EgunBerezi();
                eb.setData(date);
                eb.setDeskribapena("Automatikoki sortua");
                eb.setMota(EgunMota.LEKTIBOA);
                eb.setIkasturtea(ikasturtea);
                lektiboak.add(eb);
            }
        }

        ikasturtea.setEgunBereziak(lektiboak);
        ikasturteaService.gordeIkasturtea(ikasturtea);

        return "redirect:/kudeatzaile/egutegia";
    }
}
