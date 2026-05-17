package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.service.IkasturteaService;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoaService;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoJabeEsleipenService;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoJabeInportazioEmaitza;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
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
    private final KoadernoJabeEsleipenService koadernoJabeEsleipenService;
    private final FamiliaRepository familiaRepository;
    private final TaldeaRepository taldeaRepository;
    private final IrakasleaRepository irakasleaRepository;

    /**
     * Kudeatzaileko koaderno zerrenda:
     * - filtratu familiaz
     * - filtratu taldeaz
     */
    @GetMapping({"", "/"})
    public String index(
            @RequestParam(required = false) Long familiaId,
            @RequestParam(required = false) Long taldeaId,
            @RequestParam(required = false) Long irakasleId,
            @RequestParam(required = false) Long ikasturteaId,
            @RequestParam(required = false) Boolean moodleEstekaDu,
            Model model) {

        // 1) Familia eta talde zerrendak (filtro dropdown-entzat)
        List<Familia> familiak = familiaRepository.findAll(); // nahi baduzu: findAllByAktiboTrueOrderByIzenaAsc()
        familiak = familiak.stream()
                .sorted((f1, f2) -> f1.getIzena().compareToIgnoreCase(f2.getIzena()))
                .collect(Collectors.toList());
        model.addAttribute("familiak", familiak);

        List<Taldea> taldeak;
        Long filtroTaldeaId = taldeaId;
        if (familiaId != null) {
            taldeak = taldeaRepository.findByZikloa_Familia_IdOrderByIzenaAsc(familiaId);
            final Long requestedTaldeaId = filtroTaldeaId;
            if (requestedTaldeaId != null && taldeak.stream().noneMatch(t -> requestedTaldeaId.equals(t.getId()))) {
                filtroTaldeaId = null;
            }
        } else {
            taldeak = taldeaRepository.findAllByOrderByIzenaAsc();
        }
        final Long selectedTaldeaId = filtroTaldeaId;
        model.addAttribute("taldeak", taldeak);

        // 2) Ikasturte filtroa: defektuz ikasturte aktiboa
        List<Ikasturtea> ikasturteak = ikasturteaService.getAllOrderedDesc();
        Long selectedIkasturteaId = ikasturteaId != null
                ? ikasturteaId
                : ikasturteaService.getAktiboa().map(Ikasturtea::getId).orElse(null);

        // 3) Koaderno guztiak kargatu (service-ak inplementatu dezake simpleki repo.findAll() deituz)
        List<Koadernoa> koadernoak = koadernoaService.findAll();

        if (selectedIkasturteaId != null) {
            koadernoak = koadernoak.stream()
                    .filter(k -> k.getEgutegia() != null
                            && k.getEgutegia().getIkasturtea() != null
                            && selectedIkasturteaId.equals(k.getEgutegia().getIkasturtea().getId()))
                    .collect(Collectors.toList());
        }

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
        if (selectedTaldeaId != null) {
            koadernoak = koadernoak.stream()
                    .filter(k -> {
                        Moduloa m = k.getModuloa();
                        Taldea t = (m != null ? m.getTaldea() : null);
                        return t != null && selectedTaldeaId.equals(t.getId());
                    })
                    .collect(Collectors.toList());
        }


        if (irakasleId != null) {
            koadernoak = koadernoak.stream()
                    .filter(k -> k.getIrakasleak() != null && k.getIrakasleak().stream().anyMatch(ir -> irakasleId.equals(ir.getId())))
                    .collect(Collectors.toList());
        }

        if (moodleEstekaDu != null) {
            koadernoak = koadernoak.stream()
                    .filter(k -> k.hasMoodleEsteka() == moodleEstekaDu)
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
        model.addAttribute("taldeaId", selectedTaldeaId);
        model.addAttribute("irakasleId", irakasleId);
        model.addAttribute("ikasturteaId", selectedIkasturteaId);
        model.addAttribute("moodleEstekaDu", moodleEstekaDu);
        model.addAttribute("ikasturteak", ikasturteak);
        model.addAttribute("irakasleak", irakasleaRepository.findAll());
        model.addAttribute("badagoJabeGabekoKoadernorik", koadernoJabeEsleipenService.badagoJabeGabekoKoadernorik());

        return "kudeatzaile/koadernoak/index";
    }


    @GetMapping("/jabe-gabeak/exportatu")
    public ResponseEntity<byte[]> exportatuJabeGabekoKoadernoak() {
        try {
            byte[] edukia = koadernoJabeEsleipenService.exportatuJabeGabekoKoadernoak();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"jabe-gabeko-koadernoak.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(edukia);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Ezin izan da Excel fitxategia sortu: " + ex.getMessage()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    @PostMapping("/jabe-gabeak/inportatu")
    public String inportatuJabeGabekoKoadernoak(@RequestParam("fitxategia") MultipartFile fitxategia,
                                                 RedirectAttributes ra) {
        try {
            KoadernoJabeInportazioEmaitza emaitza = koadernoJabeEsleipenService.inportatuJabeEsleipenak(fitxategia);
            ra.addFlashAttribute("success", emaitza.laburpena());
            if (emaitza.hasErroreak()) {
                ra.addFlashAttribute("error", emaitza.erroreLaburpena());
            }
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/kudeatzaile/koadernoak";
    }


    @PostMapping("/{id}/jabea")
    public String aldatuJabea(@PathVariable Long id,
                              @RequestParam(name = "jabeaId", required = false) Long jabeaId,
                              @RequestParam(required = false) Long familiaId,
                              @RequestParam(required = false) Long taldeaId,
                              @RequestParam(required = false) Long irakasleId,
                              @RequestParam(required = false) Long ikasturteaId,
                              @RequestParam(required = false) Boolean moodleEstekaDu,
                              RedirectAttributes ra) {
        try {
            koadernoaService.aldatuJabea(id, jabeaId);
            ra.addFlashAttribute("success", "Koadernoaren jabea eguneratu da.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }

        ra.addAttribute("familiaId", familiaId);
        ra.addAttribute("taldeaId", taldeaId);
        ra.addAttribute("irakasleId", irakasleId);
        ra.addAttribute("ikasturteaId", ikasturteaId);
        ra.addAttribute("moodleEstekaDu", moodleEstekaDu);
        return "redirect:/kudeatzaile/koadernoak";
    }

    @PostMapping("/{id}/moodle-esteka")
    public String aldatuMoodleEsteka(@PathVariable Long id,
                                     @RequestParam("moodleEsteka") String moodleEsteka,
                                     @RequestParam(required = false) Long familiaId,
                                     @RequestParam(required = false) Long taldeaId,
                                     @RequestParam(required = false) Long irakasleId,
                                     @RequestParam(required = false) Long ikasturteaId,
                                     @RequestParam(required = false) Boolean moodleEstekaDu,
                                     RedirectAttributes ra) {
        try {
            koadernoaService.gordeMoodleEsteka(id, moodleEsteka);
            ra.addFlashAttribute("success", "Moodle esteka eguneratu da.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }

        ra.addAttribute("familiaId", familiaId);
        ra.addAttribute("taldeaId", taldeaId);
        ra.addAttribute("irakasleId", irakasleId);
        ra.addAttribute("ikasturteaId", ikasturteaId);
        ra.addAttribute("moodleEstekaDu", moodleEstekaDu);
        return "redirect:/kudeatzaile/koadernoak";
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
