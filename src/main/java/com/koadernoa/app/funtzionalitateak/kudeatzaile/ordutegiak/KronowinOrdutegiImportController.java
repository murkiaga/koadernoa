package com.koadernoa.app.funtzionalitateak.kudeatzaile.ordutegiak;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.ordutegiak.service.KronowinOrdutegiImportService;
import com.koadernoa.app.objektuak.ordutegiak.service.OrdutegiImportResult;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/kudeatzaile/ordutegiak")
public class KronowinOrdutegiImportController {
    private final KronowinOrdutegiImportService importService;
    private final IkasturteaRepository ikasturteaRepository;

    @GetMapping("/inportatu")
    public String inportatuForm(@RequestParam(name = "ikasturteaId", required = false) Long ikasturteaId,
                                Model model) {
        model.addAttribute("ikasturteak", ikasturteaRepository.findAllByOrderByIzenaDesc());
        if (ikasturteaId != null) {
            model.addAttribute("aukeratutakoIkasturteaId", ikasturteaId);
        } else {
            ikasturteaRepository.findFirstByAktiboaTrueOrderByIdDesc()
                    .ifPresent(ikasturtea -> model.addAttribute("aukeratutakoIkasturteaId", ikasturtea.getId()));
        }
        return "kudeatzaile/ordutegiak/inportatu";
    }

    @PostMapping("/inportatu")
    public String inportatu(@RequestParam("fitxategia") MultipartFile fitxategia,
                            @RequestParam("ikasturteaId") Long ikasturteaId,
                            Model model) {
        try {
            OrdutegiImportResult result = importService.inportatu(fitxategia, ikasturteaId);
            model.addAttribute("emaitza", result);
            model.addAttribute("mezua", "KRONOWIN ordutegia ondo inportatu da");
        } catch (Exception e) {
            model.addAttribute("errorea", e.getMessage());
        }
        model.addAttribute("ikasturteak", ikasturteaRepository.findAllByOrderByIzenaDesc());
        model.addAttribute("aukeratutakoIkasturteaId", ikasturteaId);
        return "kudeatzaile/ordutegiak/inportatu";
    }
}
