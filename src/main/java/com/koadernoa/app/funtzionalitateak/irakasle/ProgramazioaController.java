package com.koadernoa.app.funtzionalitateak.irakasle;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Ebaluaketa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Programazioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.UnitateDidaktikoa;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.ProgramazioaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.DenboralizazioGeneratorService;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoaService;
import com.koadernoa.app.objektuak.koadernoak.service.ProgramazioaService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static com.koadernoa.app.security.SecurityUtils.isKudeatzailea;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle/programazioa")
@SessionAttributes("koadernoAktiboa")
@RequiredArgsConstructor
public class ProgramazioaController {

    private final ProgramazioaService programazioaService;
    private final IrakasleaService irakasleaService;
    private final KoadernoaService koadernoaService;
    private final KoadernoaRepository koadernoaRepository;
    private final ProgramazioaRepository programazioaRepository;
    private final DenboralizazioGeneratorService denboralizazioGeneratorService;
    

    @GetMapping
    public String index(
      @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoAktiboa,
      Authentication auth, Model model, RedirectAttributes ra
    ) {
      if (koadernoAktiboa == null || koadernoAktiboa.getId() == null) {
        ra.addFlashAttribute("error", "Ez dago koaderno aktiborik aukeratuta.");
        return "redirect:/irakasle";
      }

      var k = koadernoaRepository.findByIdWithOrdutegiaAndEgutegia(koadernoAktiboa.getId()).orElseThrow();
      k.getEgutegia().getEgunBereziak().size(); // initialize

      Programazioa programazioa = programazioaService
            .loadWithEbaluaketakUdetajpByKoadernoId(k.getId())
            .orElseGet(() -> programazioaService.createWithDefaultEbaluaketak(koadernoAktiboa));

      // ✅ UD zerrenda “lautu” (EBAL ordena, eta barruan UD.posizioa)
      var unitateak = programazioa.getEbaluaketak() == null ? java.util.List.<UnitateDidaktikoa>of()
          : programazioa.getEbaluaketak().stream()
              .sorted(java.util.Comparator.comparing(e -> java.util.Optional.ofNullable(((Ebaluaketa)e).getOrdena()).orElse(0)))
              .flatMap(e -> e.getUnitateak() == null ? java.util.stream.Stream.<UnitateDidaktikoa>empty()
                                                     : e.getUnitateak().stream()
                                                         .sorted(java.util.Comparator
                                                             .comparingInt(UnitateDidaktikoa::getPosizioa)
                                                             .thenComparing(UnitateDidaktikoa::getId)))
              .collect(java.util.stream.Collectors.toList());

      //Ebaluazioen ordu erabilgarriak:
      Map<Long,Integer> ebalDispon =
    		    programazioaService.ebalOrduErabilgarriakBlokeekin(programazioa, k.getEgutegia(), k.getOrdutegiak());

      //(aukeran) UD orduen batura ebaluazioz
      Map<Long,Integer> ebalUdOrduak = programazioa.getEbaluaketak().stream()
        .collect(java.util.stream.Collectors.toMap(Ebaluaketa::getId,
            e -> e.getUnitateak().stream()
                  .mapToInt(u -> java.util.Optional.ofNullable(u.getOrduak()).orElse(0))
                  .sum()
        ));

      boolean canBulk =
          k.getEgutegia() != null &&
          k.getEgutegia().getHasieraData() != null &&
          k.getEgutegia().getBukaeraData() != null &&
          k.getOrdutegiak() != null && !k.getOrdutegiak().isEmpty() &&
          unitateak != null && !unitateak.isEmpty();

      //UD guztien orduen batura (UD.orduak; planifikatuak aparteko map-ean)
      int totalUdHours = unitateak == null ? 0
          : unitateak.stream().mapToInt(ud -> java.util.Optional.ofNullable(ud.getOrduak()).orElse(0)).sum();

      model.addAttribute("koadernoAktiboa", k);
      model.addAttribute("programazioa", programazioa);
      model.addAttribute("unitateak", unitateak);
      model.addAttribute("jpOrduakMap", programazioaService.planifikatutakoOrduakMap(unitateak));
      model.addAttribute("canBulk", canBulk);
      model.addAttribute("totalUdHours", totalUdHours);
      model.addAttribute("ebaluaketak", programazioa.getEbaluaketak());
      model.addAttribute("ebalDispon", ebalDispon);
      model.addAttribute("ebalUdOrduak", ebalUdOrduak);
      return "irakasleak/programazioa/index";
    }

 // ---------- UD CRUD ----------
    @PostMapping("/ud")
    public String sortuUd(
        @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoAktiboa,
        @RequestParam Long ebaluaketaId,                
        @RequestParam String kodea,
        @RequestParam String izenburua,
        @RequestParam(defaultValue = "0") int orduak,
        @RequestParam(defaultValue = "0") int posizioa,
        RedirectAttributes ra
    ) {
        if (koadernoAktiboa == null || koadernoAktiboa.getId() == null) {
            ra.addFlashAttribute("error", "Koaderno aktiboa falta da.");
            return "redirect:/irakasle";
        }
        // UD berria ebaluaketa horren barruan sortu
        programazioaService.addUdToEbaluaketa(ebaluaketaId, kodea, izenburua, orduak, posizioa);
        ra.addFlashAttribute("ok", "UD sortuta.");
        return "redirect:/irakasle/programazioa";
    }

    @PostMapping("/ud/{udId}/eguneratu")
    public String eguneratuUd(@PathVariable Long udId,
            @RequestParam String kodea,
            @RequestParam String izenburua,
            @RequestParam int orduak,
            @RequestParam(required = false) Integer posizioa, // posizioa mantendu nahi baduzu, pasa; bestela null
            @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa koadernoa,
            Authentication auth,
            RedirectAttributes ra) {

        if (koadernoa == null || koadernoa.getId() == null) {
            ra.addFlashAttribute("error", "Koaderno aktiboa falta da.");
            return "redirect:/irakasle";
        }
        Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        if (!isKudeatzailea(auth) && !koadernoaService.irakasleakBadaukaSarbidea(irakaslea, koadernoa)) {
            ra.addFlashAttribute("error", "Ez duzu baimenik ekintza honetarako.");
            return "redirect:/irakasle";
        }
        // 🛡️ UD hori koaderno honetakoa dela egiaztatu
        if (!programazioaService.udDagokioKoadernoari(udId, koadernoa.getId())) {
            ra.addFlashAttribute("error", "UD ez dator bat koaderno aktiboarekin.");
            return "redirect:/irakasle/programazioa";
        }

        programazioaService.updateUd(udId, kodea, izenburua, orduak, posizioa);
        ra.addFlashAttribute("ok", "UD eguneratua.");
        return "redirect:/irakasle/programazioa";
    }

    @PostMapping("/ud/{udId}/ezabatu")
    public String ezabatuUd(@PathVariable Long udId, RedirectAttributes ra) {
        programazioaService.deleteUd(udId);
        ra.addFlashAttribute("ok", "UD ezabatuta.");
        return "redirect:/irakasle/programazioa";
    }

// ---------- JardueraPlanifikatua CRUD ----------
    @PostMapping("/ud/{udId}/planifikatua")
    public String sortuPlanifikatua(
        @PathVariable Long udId,
        @RequestParam String izenburua,
        @RequestParam(defaultValue = "0") int orduak,
        RedirectAttributes ra
    ) {
        programazioaService.addJardueraPlanifikatua(udId, izenburua, orduak);
        ra.addFlashAttribute("ok", "Jarduera planifikatua sortuta.");
        return "redirect:/irakasle/programazioa";
    }

    @PostMapping("/planifikatuak/{jpId}/eguneratu")
    public String eguneratuPlanifikatua(
        @PathVariable Long jpId,
        @RequestParam String izenburua,
        @RequestParam int orduak,
        RedirectAttributes ra
    ) {
        programazioaService.updateJardueraPlanifikatua(jpId, izenburua, orduak);
        ra.addFlashAttribute("ok", "Jarduera planifikatua eguneratua.");
        return "redirect:/irakasle/programazioa";
    }

    @PostMapping("/planifikatuak/{jpId}/ezabatu")
    public String ezabatuPlanifikatua(@PathVariable Long jpId, RedirectAttributes ra) {
        programazioaService.deleteJardueraPlanifikatua(jpId);
        ra.addFlashAttribute("ok", "Jarduera planifikatua ezabatuta.");
        return "redirect:/irakasle/programazioa";
    }
    
 // ---------- Programazioa -> Denboralizazioa ----------
    @GetMapping("/preview")
    @ResponseBody
    public ResponseEntity<?> preview(@RequestParam Long koadernoId) {
        Koadernoa k = koadernoaRepository.findById(koadernoId).orElse(null);
        Programazioa p = programazioaRepository.findByKoadernoaId(koadernoId).orElse(null);
        if (!canBulk(k, p)) {
            return ResponseEntity.badRequest().body("Ezin da aurreikusi: egutegia/ordutegia/programazioa falta da.");
        }
        List<DenboralizazioGeneratorService.PreviewItem> items =
        		denboralizazioGeneratorService.generateFromProgramazioa(k, p, /*preview*/ true, /*replaceExisting*/ false);
        return ResponseEntity.ok(items);
    }
	
	@PostMapping("/bolkatu")
	public String bulkatu(@RequestParam Long koadernoId,
						            @RequestParam(defaultValue = "true") boolean replaceExisting,
						            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
		Koadernoa k = koadernoaRepository.findById(koadernoId).orElse(null);
        Programazioa p = programazioaRepository.findByKoadernoaId(koadernoId).orElse(null);
        if (!canBulk(k, p)) {
            ra.addFlashAttribute("error", "Ezin da bolkatu: egutegi/ordutegia/programazioa falta da.");
            return "redirect:/irakasle/programazioa?koadernoId=" + koadernoId;
        }
        var items = denboralizazioGeneratorService.generateFromProgramazioa(k, p, /*preview*/ false, replaceExisting);
        ra.addFlashAttribute("success", "Bolketa eginda: " + items.size() + " jarduera sortu dira.");
        return "redirect:/irakasle/denboralizazioa"; // edo programaziora bueltatu nahi baduzu, alda ezazu
	}
	
	/** Programazioa -> Denboralizazio baldintza azkarrak: egutegia + ordutegia + gutxienez UD bat */
	private boolean canBulk(Koadernoa k, Programazioa p) {
	    if (k == null || p == null) return false;

	    if (k.getEgutegia() == null ||
	        k.getEgutegia().getHasieraData() == null ||
	        k.getEgutegia().getBukaeraData() == null) {
	        return false;
	    }
	    if (k.getOrdutegiak() == null || k.getOrdutegiak().isEmpty()) return false;
	    
	    boolean badagoUdBat = p.getEbaluaketak() != null &&
	        p.getEbaluaketak().stream()
	            .anyMatch(e -> e.getUnitateak() != null && !e.getUnitateak().isEmpty());

	    return badagoUdBat;
	}
}
