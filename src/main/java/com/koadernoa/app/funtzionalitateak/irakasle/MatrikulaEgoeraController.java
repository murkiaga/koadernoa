package com.koadernoa.app.funtzionalitateak.irakasle;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.entitateak.MatrikulaEgoera;
import com.koadernoa.app.objektuak.modulua.repository.IkasleaRepository;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle/ikasleak")
@RequiredArgsConstructor
@SessionAttributes("koadernoAktiboa") //<-KOADERNOA SESIOAN GORDE
public class MatrikulaEgoeraController {
	
	private final MatrikulaRepository matrikulaRepository;
	private final IkasleaRepository ikasleaRepository;
	private final IrakasleaService irakasleaService;
	private final KoadernoaService koadernoaService;

	@GetMapping({"/",""})
	public String ikasleZerrenda(
	    @ModelAttribute("koadernoAktiboa") Koadernoa koadernoAktiboa,
	    Model model
	) {
	    // Koaderno aktiborik ez badago → orri sinple bat erakutsi
	    if (koadernoAktiboa == null || koadernoAktiboa.getId() == null) {
	        return "irakasleak/errorea_koadernoa";
	    }
	    
	    //Koadernoaren taldeko ikasle kopurua
	    Long taldeId = (koadernoAktiboa != null && koadernoAktiboa.getModuloa() != null && koadernoAktiboa.getModuloa().getTaldea() != null)
	        ? koadernoAktiboa.getModuloa().getTaldea().getId()
	        : null;

	    long taldeIkasleKop = 0L;
	    if (taldeId != null) {
	      taldeIkasleKop = ikasleaRepository.countByTaldea_Id(taldeId);
	    }
	    model.addAttribute("taldeIkasleKop", taldeIkasleKop);
	    

	    // Egoera GUZTIAK ekarri (repoan definituta dagoen metodoa)
	    List<Matrikula> matrikulak =
	        matrikulaRepository.findAllByKoadernoaFetchIkasleaOrderByIzena(koadernoAktiboa.getId());

	    model.addAttribute("matrikulak", matrikulak);
	    model.addAttribute("kop", matrikulak.size());

	    // 🔹 Alias txiki bat txantiloirako: ${koadernoa.*} erabiltzen baduzu titulua/izenak...
	    model.addAttribute("koadernoa", koadernoAktiboa);

	    return "irakasleak/ikasleak/index";
	}
	
    @PostMapping("/matrikula/{id}/egoera")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> aldatuEgoera(
            @PathVariable Long id,
            @RequestParam("egoera") MatrikulaEgoera egoera,
            @RequestParam(value = "oharra", required = false) String oharra,
            Authentication auth // <-- erabiltzaile autentifikatua jasotzeko
    ) {
        
    	Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);

        if (irakaslea == null) {
            return ResponseEntity.status(403).body(Map.of("ok", false, "msg", "Ezin da irakaslea identifikatu"));
        }

        // 2. Matrikula existitzen dela egiaztatu
        Optional<Matrikula> opt = matrikulaRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "msg", "Matrikula ez da aurkitu"));
        }

        Matrikula m = opt.get();
        Long koadernoId = m.getKoadernoa().getId();

        // 3. Irakasleak koaderno horretan sarbidea duen egiaztatu
        if (!koadernoaService.irakasleakBadaukaSarbidea(irakaslea, koadernoId)) {
            return ResponseEntity.status(403).body(Map.of("ok", false, "msg", "Ez daukazu baimenik matrikula hau aldatzeko"));
        }

        // 4. Egoera aldatu
        m.setEgoera(egoera);
        m.setOharra(oharra != null ? oharra.trim() : null);

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "egoera", m.getEgoera().name(),
                "oharra", m.getOharra()
        ));
    }
}
