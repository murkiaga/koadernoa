package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.service.IkasturteaService;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;
import com.koadernoa.app.objektuak.zikloak.repository.FamiliaRepository;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/kudeatzaile/koadernoak")
@RequiredArgsConstructor
public class KoadernoaControllerKudeatzaile {

    private final IkasturteaService ikasturteaService;
    private final KoadernoaService koadernoaService;
    private final FamiliaRepository familiaRepository;
    private final TaldeaRepository taldeaRepository;

    /**
     * Kudeatzaileko koaderno zerrenda:
     * - filtratu familiaz
     * - filtratu taldeaz
     */
    @GetMapping({"", "/"})
    public String index(
            @RequestParam(required = false) Long familiaId,
            @RequestParam(required = false) Long taldeaId,
            Model model) {

        // 1) Familia eta talde zerrendak (filtro dropdown-entzat)
        List<Familia> familiak = familiaRepository.findAll(); // nahi baduzu: findAllByAktiboTrueOrderByIzenaAsc()
        familiak = familiak.stream()
                .sorted((f1, f2) -> f1.getIzena().compareToIgnoreCase(f2.getIzena()))
                .collect(Collectors.toList());
        model.addAttribute("familiak", familiak);

        List<Taldea> taldeak;
        if (familiaId != null) {
            taldeak = taldeaRepository.findByZikloa_Familia_IdOrderByIzenaAsc(familiaId);
        } else {
            taldeak = taldeaRepository.findAllByOrderByIzenaAsc();
        }
        model.addAttribute("taldeak", taldeak);

        // 2) Koaderno guztiak kargatu (service-ak inplementatu dezake simpleki repo.findAll() deituz)
        List<Koadernoa> koadernoak = koadernoaService.findAll();

        // 3) Filtratu familiaz (Taldea → Zikloa → Familia)
        if (familiaId != null) {
            koadernoak = koadernoak.stream()
                    .filter(k -> {
                        Moduloa m = k.getModuloa();
                        if (m == null) return false;
                        Taldea t = m.getTaldea();
                        if (t == null) return false;
                        Zikloa z = t.getZikloa();
                        if (z == null) return false;
                        Familia f = z.getFamilia();
                        return f != null && familiaId.equals(f.getId());
                    })
                    .collect(Collectors.toList());
        }

        // 4) Filtratu taldeaz
        if (taldeaId != null) {
            koadernoak = koadernoak.stream()
                    .filter(k -> {
                        Moduloa m = k.getModuloa();
                        Taldea t = (m != null ? m.getTaldea() : null);
                        return t != null && taldeaId.equals(t.getId());
                    })
                    .collect(Collectors.toList());
        }

        // 5) Ordenatu: Familia → Taldea → Moduloa
        Comparator<Koadernoa> cmp = Comparator
                .comparing((Koadernoa k) -> {
                    Moduloa m = k.getModuloa();
                    Taldea t = m != null ? m.getTaldea() : null;
                    Zikloa z = t != null ? t.getZikloa() : null;
                    Familia f = z != null ? z.getFamilia() : null;
                    return f != null ? f.getIzena() : "";
                }, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(k -> {
                    Moduloa m = k.getModuloa();
                    Taldea t = m != null ? m.getTaldea() : null;
                    return t != null ? t.getIzena() : "";
                }, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(k -> k.getModuloa() != null ? k.getModuloa().getIzena() : "",
                               String.CASE_INSENSITIVE_ORDER);

        koadernoak = koadernoak.stream()
                .sorted(cmp)
                .collect(Collectors.toList());

        model.addAttribute("koadernoak", koadernoak);
        model.addAttribute("familiaId", familiaId);
        model.addAttribute("taldeaId", taldeaId);

        return "kudeatzaile/koadernoak/index";
    }

    /**
     * Jada zenuen metodoa: ikasturte berrirako koadernoak sortu
     */
    @PostMapping("/sortu")
    public String sortuKoadernoak(@RequestParam("ikasturteaId") Long ikasturteaId) {
        Ikasturtea ikasturtea = ikasturteaService.getById(ikasturteaId);
        koadernoaService.sortuKoadernoakIkasturteBerrirako(ikasturtea);
        return "redirect:/kudeatzaile/egutegia?ikasturteaId=" + ikasturteaId;
    }
}
