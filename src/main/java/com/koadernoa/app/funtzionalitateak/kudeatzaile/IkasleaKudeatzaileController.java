package com.koadernoa.app.funtzionalitateak.kudeatzaile;

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
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.repository.IkasleaRepository;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;
import com.koadernoa.app.objektuak.modulua.service.IkasleaService;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/kudeatzaile/ikaslea")
@RequiredArgsConstructor
public class IkasleaKudeatzaileController {

    private final IkasleaRepository ikasleaRepository;
    private final MatrikulaRepository matrikulaRepository;
    private final IkasturteaRepository ikasturteaRepository;
    private final TaldeaRepository taldeaRepository;
    private final IkasleaService ikasleaService;

    @GetMapping("/{id}")
    public String ikasleFitxa(@PathVariable Long id,
                              @RequestParam(name = "ikasturteaId", required = false) Long ikasturteaId,
                              Model model) {
        Ikaslea ikaslea = ikasleaRepository.findById(id).orElseThrow();
        List<Ikasturtea> ikasturteak = matrikulaRepository.findIkasturteakByIkaslea(id);

        Long hautatutakoIkasturteaId = ikasturteaId;
        if (hautatutakoIkasturteaId == null) {
            hautatutakoIkasturteaId = ikasturteaRepository.findByAktiboaTrue()
                    .map(Ikasturtea::getId)
                    .orElseGet(() -> ikasturteak.isEmpty() ? null : ikasturteak.get(0).getId());
        }

        List<Matrikula> matrikulak = matrikulaRepository.findIkaslearenMatrikulakByIkasturtea(id, hautatutakoIkasturteaId);

        model.addAttribute("ikaslea", ikaslea);
        model.addAttribute("ikasturteak", ikasturteak);
        model.addAttribute("hautatutakoIkasturteaId", hautatutakoIkasturteaId);
        model.addAttribute("matrikulak", matrikulak);
        model.addAttribute("taldeak", taldeaRepository.findAllByOrderByIzenaAsc());

        return "kudeatzaile/ikaslea/fitxa";
    }

    @PostMapping("/{id}/taldea")
    public String aldatuIkasleTaldea(@PathVariable Long id,
                                     @RequestParam("taldeaId") Long taldeaId,
                                     RedirectAttributes redirectAttributes) {
        try {
            var emaitza = ikasleaService.aldatuIkaslearenTaldea(id, taldeaId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Taldea eguneratuta. " + emaitza.kendutakoMatrikulak() + " matrikula kendu eta "
                            + emaitza.sortutakoMatrikulak() + " matrikula sortu dira ikasturte aktiboan.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/kudeatzaile/ikaslea/" + id;
    }
}
