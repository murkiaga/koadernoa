package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.web.csrf.CsrfToken;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.egutegia.repository.MailaRepository;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.entitateak.ModuloaFormDto;
import com.koadernoa.app.objektuak.modulua.repository.IkasleaRepository;
import com.koadernoa.app.objektuak.modulua.service.IkasleArgazkiService;
import com.koadernoa.app.objektuak.modulua.service.ModuloaService;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.entitateak.ZikloMaila;
import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;
import com.koadernoa.app.objektuak.zikloak.repository.FamiliaRepository;
import com.koadernoa.app.objektuak.zikloak.service.TaldeaService;
import com.koadernoa.app.objektuak.zikloak.service.ZikloaService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@Controller
@RequestMapping("/kudeatzaile")
@RequiredArgsConstructor
public class KudeatzaileController {
	
	private final ZikloaService zikloaService;
	private final TaldeaService taldeaService;
    private final ModuloaService moduloaService;
    private final MailaRepository mailaRepository;
    private final IrakasleaRepository irakasleaRepository;
    private final FamiliaRepository familiaRepository;
    private final IkasleaRepository ikasleaRepository;
    private final IkasleArgazkiService ikasleArgazkiService;
    private final IkasturteaRepository ikasturteaRepository;
    
    /** Kudeatzaileko orri guztietan erabilgarri: ikasturte aktiboa */
    @ModelAttribute("ikasturteAktiboa")
    public com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea ikasturteAktiboa() {
        return ikasturteaRepository.findByAktiboaTrue().orElse(null);
    }

	@GetMapping({"","/"})
    public String kudeatzaileDashboard(Model model) {
        return "kudeatzaile/kudeatzaile_dashboard";
    }
//----ZIKLOAK
	@GetMapping("/zikloak")
    public String erakutsiZikloak(Model model) {
        List<Zikloa> zikloak = zikloaService.getAll();
        model.addAttribute("zikloak", zikloak);
        return "kudeatzaile/zikloak/index";
    }
	
	@GetMapping("/zikloak/{id}")
	public String zikloarenFitxa(@PathVariable("id") Long id, Model model) {
	    Zikloa zikloa = zikloaService.getById(id).orElseThrow();
	    model.addAttribute("zikloa", zikloa);
	    model.addAttribute("taldeak", zikloa.getTaldeak());
	    return "kudeatzaile/zikloak/ziklo-fitxa";
	}
	
	@GetMapping("/zikloak/sortu")
    public String sortuZikloaForm(Model model) {
        model.addAttribute("zikloa", new Zikloa());
        model.addAttribute("mailak", ZikloMaila.values());
        model.addAttribute("familiak", familiaRepository.findAll());
        return "kudeatzaile/zikloak/zikloa-form";
    }
	
	@GetMapping("/zikloak/editatu/{id}")
	public String editatuZikloa(@PathVariable("id") Long id, Model model) {
		Zikloa zikloa = zikloaService.getById(id)
			    .orElseThrow(() -> new IllegalArgumentException("Zikloa ez da aurkitu: " + id));
	    model.addAttribute("zikloa", zikloa);
	    model.addAttribute("mailak", ZikloMaila.values());
	    model.addAttribute("familiak", familiaRepository.findAll());
	    return "kudeatzaile/zikloak/zikloa-form";
	}
	
	@PostMapping("/zikloak/gorde")
	public String gordeZikloa(@ModelAttribute Zikloa zikloa) {
	    zikloaService.save(zikloa);
	    return "redirect:/kudeatzaile/zikloak";
	}
	
//----TALDEAK
	@GetMapping("/taldeak")
	public String taldeZerrenda(@RequestParam(name = "zikloaId", required = false) Long zikloaId,
	                            Model model,
	                            HttpServletRequest request) { // csrf-a baduzu model-era gehitzen jarrai dezakezu, aukeran
	    List<Taldea> taldeak = (zikloaId != null)
	            ? taldeaService.getByZikloaId(zikloaId)
	            : taldeaService.getAll();
	    
	    //Taldeetako ikasle kopurua jasotzeko
	    List<Long> ids = taldeak.stream().map(Taldea::getId).toList();
	    Map<Long, Long> ikasleKop = new HashMap<>();
	    ikasleaRepository.countByTaldeaIds(ids).forEach(row ->
	        ikasleKop.put(row.getTaldeaId(), row.getKop())
	    );
	    // faltan daudenak 0-ra
	    for (Long id : ids) ikasleKop.putIfAbsent(id, 0L);

	    model.addAttribute("taldeak", taldeak);
	    model.addAttribute("ikasleKop", ikasleKop);
	    model.addAttribute("irakasleak", irakasleaRepository.findAll());
	    model.addAttribute("zikloak", zikloaService.getAll()); // dropdown-erako
	    model.addAttribute("zikloaId", zikloaId);              // aukeratutako balioa

	    // (aukeran) CSRF model-era gehitzen jarrai dezakezu:
	    CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
	    model.addAttribute("csrf", csrf);

	    return "kudeatzaile/taldeak/index";
	}

    @GetMapping("/taldeak/sortu")
    public String taldeaSortuForm(Model model) {
        model.addAttribute("taldea", new Taldea());
        model.addAttribute("zikloak", zikloaService.getAll());
        return "kudeatzaile/taldeak/taldea-form";
    }

    @GetMapping("/taldeak/editatu/{id}")
    public String taldeaEditatuForm(@PathVariable("id") Long id, Model model) {
        Taldea taldea = taldeaService.getById(id).orElseThrow();
        model.addAttribute("taldea", taldea);
        model.addAttribute("zikloak", zikloaService.getAll());
        return "kudeatzaile/taldeak/taldea-form";
    }

    @PostMapping("/taldeak/gorde")
    public String gorde(@ModelAttribute Taldea taldea) {
        taldeaService.save(taldea);
        return "redirect:/kudeatzaile/taldeak";
    }

    @GetMapping("/taldeak/ezabatu/{id}")
    public String ezabatuTaldea(@PathVariable("id") Long id) {
        taldeaService.deleteById(id);
        return "redirect:/kudeatzaile/taldeak";
    }
    
    @PostMapping("/taldeak/{id}/tutorea")
    public String eguneratuTaldekoTutorea(@PathVariable("id") Long taldeaId,
                                          @RequestParam(name = "irakasleId", required = false) Long irakasleId,
                                          RedirectAttributes ra) {
        try {
            taldeaService.eguneratuTutorea(taldeaId, irakasleId);
            ra.addFlashAttribute("msg", "Tutorea eguneratuta.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorea", ex.getMessage());
        }
        return "redirect:/kudeatzaile/taldeak";
    }
    
    @GetMapping("/taldeak/{id}/argazkiak")
    public String taldeArgazkiak(@PathVariable("id") Long taldeId,
                                 Model model,
                                 HttpServletRequest request) {
        var taldea = taldeaService.getById(taldeId)
                .orElseThrow(() -> new IllegalArgumentException("Taldea ez da aurkitu: " + taldeId));

        model.addAttribute("taldea", taldea);
        model.addAttribute("ikasleak",
            ikasleaRepository.findByTaldea_IdOrderByAbizena1AscAbizena2AscIzenaAsc(taldeId));

        // 🔹 CSRF model-era modu sinplean
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        model.addAttribute("csrf", csrf);

        model.addAttribute("taldeak", taldeaService.getAllWithStudents());

        return "kudeatzaile/taldeak/argazkiak";
    }
    
    @PostMapping("/taldeak/{taldeId}/argazkiak/{ikasleId}")
    @ResponseBody
    public Map<String,Object> igoArgazkia(@PathVariable Long taldeId,
                                          @PathVariable Long ikasleId,
                                          @RequestParam("foto") MultipartFile file,
                                          HttpServletResponse resp) {
        Map<String,Object> out = new HashMap<>();
        try {
            // (Aukeran) egiaztatu ikaslea talde horretakoa dela
            var ikasleaOpt = ikasleaRepository.findById(ikasleId);
            if (ikasleaOpt.isEmpty() || ikasleaOpt.get().getTaldea()==null
                    || !ikasleaOpt.get().getTaldea().getId().equals(taldeId)) {
                throw new IllegalArgumentException("Ikaslea ez dator talde honekin bat.");
            }

            String url = ikasleArgazkiService.gordeArgazkia(ikasleId, file);
            out.put("ok", true);
            out.put("url", url);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.put("ok", false);
            out.put("errorea", e.getMessage());
        }
        return out;
    }

	
//----MODULOAK
    @GetMapping("/moduloak")
    public String moduloZerrenda(@RequestParam(name = "taldeaId", required = false) Long taldeaId, 
    							Model model) {
    	List<Moduloa> moduluak;
        if (taldeaId != null) {
            moduluak = moduloaService.getByTaldeaId(taldeaId);
            // Ikasle zerrenda gehitu modelera
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
    
    @PostMapping("/moduloak/gorde")
    public String gordeModuloa(@Valid @ModelAttribute("moduloaForm") ModuloaFormDto form,
                               BindingResult br,
                               Model model) {
        if (br.hasErrors()) {
            // berriz kargatu aukerak
            model.addAttribute("taldeak", taldeaService.getAll());
            model.addAttribute("mailak", mailaRepository.findAllByAktiboTrueOrderByOrdenaAscIzenaAsc());
            return "kudeatzaile/moduloak/moduloa-form";
        }
        moduloaService.saveFromDto(form);
        return "redirect:/kudeatzaile/moduloak";
    }

    @GetMapping("/moduloak/sortu")
    public String sortuModuloaForm(@RequestParam(name = "taldeaId", required = false) Long taldeaId,
                                   Model model) {
        ModuloaFormDto form = new ModuloaFormDto();
        if (taldeaId != null) form.setTaldeaId(taldeaId); // aurrez hautatu
        model.addAttribute("moduloaForm", form);
        model.addAttribute("taldeak", taldeaService.getAll());
        model.addAttribute("mailak", mailaRepository.findAllByAktiboTrueOrderByOrdenaAscIzenaAsc());
        return "kudeatzaile/moduloak/moduloa-form";
    }

    @GetMapping("/moduloak/editatu/{id}")
    public String editatuModuloa(@PathVariable Long id, Model model) {
        Moduloa m = moduloaService.getById(id).orElseThrow();
        ModuloaFormDto form = new ModuloaFormDto();
        form.setId(m.getId());
        form.setIzena(m.getIzena());
        form.setKodea(m.getKodea());
        form.setEeiKodea(m.getEeiKodea());
        form.setMailaId(m.getMaila() != null ? m.getMaila().getId() : null);
        form.setTaldeaId(m.getTaldea() != null ? m.getTaldea().getId() : null);

        model.addAttribute("moduloaForm", form);
        model.addAttribute("taldeak", taldeaService.getAll());
        model.addAttribute("mailak", mailaRepository.findAllByAktiboTrueOrderByOrdenaAscIzenaAsc());
        return "kudeatzaile/moduloak/moduloa-form";
    }

    @GetMapping("/moduloak/ezabatu/{id}")
    public String moduloaEzabatu(@PathVariable("id") Long id) {
        moduloaService.delete(id);
        return "redirect:/kudeatzaile/moduloak";
    }
}
