package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.egutegia.repository.MailaRepository;
import com.koadernoa.app.objektuak.egutegia.service.IkasturteaService;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoAutomatikoSorreraService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/kudeatzaile/ikasturteak")
public class IkasturteaController {

	private final IkasturteaService ikasturteaService;
    private final MailaRepository mailaRepository;
    private final KoadernoAutomatikoSorreraService koadernoAutomatikoSorreraService;

    // 1. Zerrenda
    @GetMapping
    public String ikasturteZerrenda(Model model) {
        List<Ikasturtea> ikasturteak = ikasturteaService.getAllOrderedDesc();
        model.addAttribute("ikasturteak", ikasturteak);
        return "kudeatzaile/ikasturtea/index";
    }

    // 2. Ikasturte fitxa eta egutegiak
    @GetMapping("/{id}")
    public String ikasturteFitxa(@PathVariable("id") Long id, Model model) {
        Ikasturtea ikasturtea = ikasturteaService.getById(id);
        model.addAttribute("ikasturtea", ikasturtea);
        model.addAttribute("erabiliGabekoMailak", kalkulatuErabiliGabekoMailak(ikasturtea));
        return "kudeatzaile/ikasturtea/ikasturtea-fitxa";
    }

    private List<Maila> kalkulatuErabiliGabekoMailak(Ikasturtea ikasturtea) {
        Set<Long> erabilitakoMailaIdak = (ikasturtea.getEgutegiak() == null)
                ? Set.of()
                : ikasturtea.getEgutegiak().stream()
                .filter(e -> e.getMaila() != null)
                .map(e -> e.getMaila().getId())
                .collect(Collectors.toSet());
        return mailaRepository.findAllByAktiboTrueOrderByOrdenaAscIzenaAsc().stream()
                .filter(m -> !erabilitakoMailaIdak.contains(m.getId()))
                .toList();
    }

    // 3. Sortu form
    @GetMapping("/sortu")
    public String sortuForm(Model model) {
        model.addAttribute("ikasturtea", new Ikasturtea());
        return "kudeatzaile/ikasturtea/ikasturtea-form";
    }

    // 4. Gorde (sortu edo editatu)
    @PostMapping("/gorde")
    public String gorde(@ModelAttribute Ikasturtea ikasturtea) {
        ikasturteaService.save(ikasturtea);
        return "redirect:/kudeatzaile/ikasturteak";
    }
    
    @PostMapping("/aldatu-aktiboa")
    public String aldatuAktiboa(@RequestParam("ikasturteaId") Long ikasturteaId) {
        Ikasturtea ikasturtea = ikasturteaService.getById(ikasturteaId);
        boolean berria = !ikasturtea.isAktiboa();

        if (berria) {
            ikasturteaService.desaktibatuDenak(); // Beste guztiak desaktibatu
        }

        ikasturtea.setAktiboa(berria);
        ikasturteaService.save(ikasturtea);

        return "redirect:/kudeatzaile/ikasturteak";
    }

    @PostMapping("/{ikasturteaId}/koadernoak/sortu-faltan")
    public String sortuFaltaDirenKoadernoak(@PathVariable Long ikasturteaId, RedirectAttributes ra) {
        try {
            KoadernoAutomatikoSorreraService.Emaitza emaitza =
                    koadernoAutomatikoSorreraService.sortuFaltaDirenKoadernoak(ikasturteaId);
            ra.addFlashAttribute("success",
                    emaitza.sortutakoak() + " koaderno berri sortu dira. "
                            + emaitza.lehendikZeudenak() + " koaderno jada existitzen ziren. "
                            + emaitza.estatistikakSortuta() + " koadernori estatistikak sortu zaizkie.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/kudeatzaile/ikasturteak";
    }
}
