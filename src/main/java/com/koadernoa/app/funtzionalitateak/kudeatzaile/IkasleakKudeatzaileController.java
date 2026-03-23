package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.repository.IkasleaRepository;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;
import com.koadernoa.app.objektuak.zikloak.repository.ZikloaRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/kudeatzaile/ikasleak")
@RequiredArgsConstructor
public class IkasleakKudeatzaileController {

    private final IkasleaRepository ikasleaRepository;
    private final ZikloaRepository zikloaRepository;
    private final TaldeaRepository taldeaRepository;

    @GetMapping
    public String ikasleZerrenda(@RequestParam(name = "zikloaId", required = false) Long zikloaId,
                                 @RequestParam(name = "taldeaId", required = false) Long taldeaId,
                                 @RequestParam(name = "page", defaultValue = "0") int page,
                                 @RequestParam(name = "size", defaultValue = "20") int size,
                                 Model model) {
        int tamaina = Math.min(100, Math.max(20, size));
        int orria = Math.max(0, page);
        PageRequest pageable = PageRequest.of(orria, tamaina, Sort.by("abizena1", "abizena2", "izena").ascending());

        Page<Ikaslea> ikasleak = ikasleaRepository.bilatuKudeatzaile(zikloaId, taldeaId, pageable);

        model.addAttribute("ikasleak", ikasleak.getContent());
        model.addAttribute("zikloak", zikloaRepository.findAllByOrderByIzenaAsc());
        model.addAttribute("taldeak", zikloaId != null
                ? taldeaRepository.findByZikloa_IdOrderByIzenaAsc(zikloaId)
                : taldeaRepository.findAllByOrderByIzenaAsc());
        model.addAttribute("zikloaId", zikloaId);
        model.addAttribute("taldeaId", taldeaId);
        model.addAttribute("currentPage", ikasleak.getNumber());
        model.addAttribute("totalPages", ikasleak.getTotalPages());
        model.addAttribute("pageSize", tamaina);
        model.addAttribute("totalItems", ikasleak.getTotalElements());
        model.addAttribute("pageSizes", List.of(20, 40, 60, 80, 100));

        return "kudeatzaile/ikasleak/index";
    }

    @GetMapping("/bilatu")
    @ResponseBody
    public List<Map<String, Object>> bilatuIkasleak(@RequestParam(name = "q", required = false) String q) {
        if (q == null || q.trim().length() < 3) {
            return List.of();
        }
        return ikasleaRepository.bilatuAutocomplete(q.trim(), PageRequest.of(0, 10)).stream()
                .map(i -> Map.<String, Object>of(
                        "id", i.getId(),
                        "izena", i.getIzenOsoa(),
                        "hna", i.getHna() != null ? i.getHna() : "",
                        "taldea", i.getTaldea() != null ? i.getTaldea().getIzena() : ""
                ))
                .toList();
    }
}
