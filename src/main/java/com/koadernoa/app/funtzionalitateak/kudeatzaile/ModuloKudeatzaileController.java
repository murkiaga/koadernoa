package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.egutegia.repository.MailaRepository;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.entitateak.MatrikulaEgoera;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.entitateak.ModuloaFormDto;
import com.koadernoa.app.objektuak.modulua.repository.IkasleaRepository;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;
import com.koadernoa.app.objektuak.modulua.service.ModuloaService;
import com.koadernoa.app.objektuak.zikloak.service.TaldeaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/kudeatzaile/moduloa")
@RequiredArgsConstructor
public class ModuloKudeatzaileController {

    private final ModuloaService moduloaService;
    private final TaldeaService taldeaService;
    private final MailaRepository mailaRepository;
    private final IkasturteaRepository ikasturteaRepository;
    private final IkasleaRepository ikasleaRepository;
    private final KoadernoaRepository koadernoaRepository;
    private final MatrikulaRepository matrikulaRepository;

    @ModelAttribute("ikasturteAktiboa")
    public com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea ikasturteAktiboa() {
        return ikasturteaRepository.findByAktiboaTrue().orElse(null);
    }

    @GetMapping({"", "/"})
    public String moduloZerrenda(@RequestParam(name = "taldeaId", required = false) Long taldeaId,
                                 Model model) {
        List<Moduloa> moduluak;
        if (taldeaId != null) {
            moduluak = moduloaService.getByTaldeaId(taldeaId);
            model.addAttribute("ikasleak",
                ikasleaRepository.findByTaldea_IdOrderByAbizena1AscAbizena2AscIzenaAsc(taldeaId));
        } else {
            moduluak = moduloaService.getAll();
        }
        model.addAttribute("taldeaId", taldeaId);
        model.addAttribute("moduluak", moduluak);
        model.addAttribute("taldeak", taldeaService.getAll());

        return "kudeatzaile/moduloak/index";
    }

    @PostMapping("/gorde")
    public String gordeModuloa(@Valid @ModelAttribute("moduloaForm") ModuloaFormDto form,
                               BindingResult br,
                               Model model) {
        if (br.hasErrors()) {
            model.addAttribute("taldeak", taldeaService.getAll());
            model.addAttribute("mailak", mailaRepository.findAllByAktiboTrueOrderByOrdenaAscIzenaAsc());
            return "kudeatzaile/moduloak/moduloa-form";
        }
        moduloaService.saveFromDto(form);
        return "redirect:/kudeatzaile/moduloa";
    }

    @GetMapping("/sortu")
    public String sortuModuloaForm(@RequestParam(name = "taldeaId", required = false) Long taldeaId,
                                   Model model) {
        ModuloaFormDto form = new ModuloaFormDto();
        if (taldeaId != null) form.setTaldeaId(taldeaId);
        model.addAttribute("moduloaForm", form);
        model.addAttribute("taldeak", taldeaService.getAll());
        model.addAttribute("mailak", mailaRepository.findAllByAktiboTrueOrderByOrdenaAscIzenaAsc());
        return "kudeatzaile/moduloak/moduloa-form";
    }

    @GetMapping("/editatu/{id}")
    public String editatuModuloa(@PathVariable Long id, Model model) {
        Moduloa m = moduloaService.getById(id).orElseThrow();
        ModuloaFormDto form = new ModuloaFormDto();
        form.setId(m.getId());
        form.setIzena(m.getIzena());
        form.setKodea(m.getKodea());
        form.setEeiKodea(m.getEeiKodea());
        form.setOrduak(m.getOrduak());
        form.setMailaId(m.getMaila() != null ? m.getMaila().getId() : null);
        form.setTaldeaId(m.getTaldea() != null ? m.getTaldea().getId() : null);

        model.addAttribute("moduloaForm", form);
        model.addAttribute("taldeak", taldeaService.getAll());
        model.addAttribute("mailak", mailaRepository.findAllByAktiboTrueOrderByOrdenaAscIzenaAsc());
        return "kudeatzaile/moduloak/moduloa-form";
    }

    @GetMapping("/{id}/matrikulak")
    public String moduloMatrikulak(@PathVariable("id") Long moduloId, Model model) {
        List<Koadernoa> koadernoak = koadernoaRepository.findByModuloaIdInAktiboIkasturtea(moduloId);
        if (koadernoak.isEmpty()) {
            throw new IllegalArgumentException("Ez da aurkitu ikasturte aktiboko koadernoarik modulu honentzat: " + moduloId);
        }

        Koadernoa koadernoa = koadernoak.get(0);
        List<Matrikula> matrikulak = matrikulaRepository.findAllByKoadernoaFetchIkasleaOrderByIzena(koadernoa.getId());

        model.addAttribute("koadernoa", koadernoa);
        model.addAttribute("moduloa", koadernoa.getModuloa());
        model.addAttribute("matrikulak", matrikulak);
        model.addAttribute("egoerak", MatrikulaEgoera.values());

        return "kudeatzaile/moduloak/matrikulak";
    }

    @PostMapping("/{id}/matrikulak/{matrikulaId}/egoera")
    @ResponseBody
    public ResponseEntity<?> eguneratuMatrikulaEgoera(@PathVariable("id") Long moduloId,
                                                       @PathVariable Long matrikulaId,
                                                       @RequestParam("egoera") MatrikulaEgoera egoera) {
        Optional<Matrikula> opt = matrikulaRepository.findById(matrikulaId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Matrikula matrikula = opt.get();
        if (!matrikula.getKoadernoa().getModuloa().getId().equals(moduloId)) {
            return ResponseEntity.badRequest().body(Map.of("ok", false));
        }
        matrikula.setEgoera(egoera);
        matrikulaRepository.save(matrikula);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/{id}/ikasleak-bilatu")
    @ResponseBody
    public List<Map<String, Object>> bilatuIkasleakMatrikulaBerrirako(@PathVariable("id") Long moduloId,
                                                                      @RequestParam("q") String q) {
        if (q == null || q.trim().length() < 3) return List.of();
        List<Koadernoa> koadernoak = koadernoaRepository.findByModuloaIdInAktiboIkasturtea(moduloId);
        if (koadernoak.isEmpty()) return List.of();

        Koadernoa koadernoa = koadernoak.get(0);
        return matrikulaRepository.bilatuIkasleMatrikulatuGabeakKoadernoan(koadernoa.getId(), q.trim()).stream()
                .limit(10)
                .map(i -> Map.<String, Object>of(
                        "id", i.getId(),
                        "izena", i.getIzenOsoa(),
                        "hna", i.getHna() != null ? i.getHna() : ""
                ))
                .toList();
    }


    @GetMapping("/{id}/matrikula-aurreikuspena")
    @ResponseBody
    public ResponseEntity<?> aurreikusiMatrikula(@PathVariable("id") Long moduloId,
                                                 @RequestParam("ikasleaId") Long ikasleaId) {
        List<Koadernoa> koadernoak = koadernoaRepository.findByModuloaIdInAktiboIkasturtea(moduloId);
        if (koadernoak.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "msg", "Ez dago koaderno aktiborik"));
        }
        if (ikasleaRepository.findById(ikasleaId).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "msg", "Ikaslea ez da aurkitu"));
        }

        Koadernoa koadernoa = koadernoak.get(0);
        String eeiKodea = koadernoa.getModuloa().getEeiKodea();
        if (eeiKodea == null || eeiKodea.isBlank()) {
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "badago", false,
                    "mezua", "Ikasle hau matrikulatu nahi duzu?"
            ));
        }

        Long ikasturteaId = koadernoa.getEgutegia().getIkasturtea().getId();
        List<String> moduloIzenak = matrikulaRepository
                .findConflictModuloIzenakByIkasleaAndIkasturteaAndEeiKodeDifferentKoaderno(ikasleaId, ikasturteaId, eeiKodea, koadernoa.getId());

        if (moduloIzenak.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "badago", false,
                    "mezua", "Ikasle hau matrikulatu nahi duzu?"
            ));
        }

        String izenak = String.join(", ", moduloIzenak);
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "badago", true,
                "moduloak", moduloIzenak,
                "mezua", izenak + "(e)an matrikulatuta dago eta kendu egingo zaio. Jarraitu nahi duzu?"
        ));
    }

    @PostMapping("/{id}/matrikulak")
    public String gehituMatrikula(@PathVariable("id") Long moduloId,
                                  @RequestParam("ikasleaId") Long ikasleaId) {
        List<Koadernoa> koadernoak = koadernoaRepository.findByModuloaIdInAktiboIkasturtea(moduloId);
        if (koadernoak.isEmpty()) {
            return "redirect:/kudeatzaile/moduloa";
        }

        Optional<Ikaslea> ikasleaOpt = ikasleaRepository.findById(ikasleaId);
        if (ikasleaOpt.isEmpty()) {
            return "redirect:/kudeatzaile/moduloa/" + moduloId + "/matrikulak";
        }

        Koadernoa koadernoa = koadernoak.get(0);
        Ikaslea ikaslea = ikasleaOpt.get();
        Long ikasturteaId = koadernoa.getEgutegia().getIkasturtea().getId();
        String eeiKodea = koadernoa.getModuloa().getEeiKodea();

        if (eeiKodea != null && !eeiKodea.isBlank()) {
            List<Matrikula> desmatrikulatzekoak = matrikulaRepository
                    .findByIkasleaAndIkasturteaAndEeiKodeDifferentKoaderno(ikasleaId, ikasturteaId, eeiKodea, koadernoa.getId());
            matrikulaRepository.deleteAll(desmatrikulatzekoak);
        }

        if (!matrikulaRepository.existsByIkasleaIdAndKoadernoaId(ikasleaId, koadernoa.getId())) {
            Matrikula m = new Matrikula();
            m.setKoadernoa(koadernoa);
            m.setIkaslea(ikaslea);
            m.setEgoera(MatrikulaEgoera.MATRIKULATUA);
            matrikulaRepository.save(m);
        }

        return "redirect:/kudeatzaile/moduloa/" + moduloId + "/matrikulak";
    }

    @GetMapping("/ezabatu/{id}")
    public String moduloaEzabatu(@PathVariable("id") Long id) {
        moduloaService.delete(id);
        return "redirect:/kudeatzaile/moduloa";
    }
}
