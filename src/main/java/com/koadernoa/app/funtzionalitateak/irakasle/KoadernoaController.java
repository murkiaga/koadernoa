package com.koadernoa.app.funtzionalitateak.irakasle;

import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoaSortuDto;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoaService;
import com.koadernoa.app.objektuak.modulua.service.IkasleaService;

import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle/koadernoa")
@RequiredArgsConstructor
@SessionAttributes("koadernoAktiboa") //<-KOADERNOA SESIOAN GORDE
public class KoadernoaController {
	
	private final IrakasleaService irakasleaService;
	private final KoadernoaService koadernoaService;
	private final IkasleaService ikasleaService;
	private final KoadernoaRepository koadernoaRepository;
	
	private static final List<Astegunak> ASTE_ORDENA = List.of(
	        Astegunak.ASTELEHENA,
	        Astegunak.ASTEARTEA,
	        Astegunak.ASTEAZKENA,
	        Astegunak.OSTEGUNA,
	        Astegunak.OSTIRALA
	    );

	
	@GetMapping("/{id}")
	public String hautatuKoadernoa(
	        @PathVariable Long id,
	        @RequestParam(value = "next", required = false) String nextEncoded,
	        Authentication auth,
	        Model model,
	        RedirectAttributes ra) {

	    Koadernoa koadernoa = koadernoaService.findById(id);
	    if (koadernoa == null) {
	        ra.addFlashAttribute("error", "Koadernoa ez da existitzen.");
	        return "redirect:/irakasle";
	    }

	    // (Aukerakoa) Egiaztatu irakasleak koaderno honetarako sarbidea duela
	    Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
	    if (!koadernoaService.irakasleakBadaukaSarbidea(irakaslea, koadernoa)) {
	        ra.addFlashAttribute("error", "Ez duzu koaderno honetarako sarbiderik.");
	        return "redirect:/irakasle";
	    }

	    // Sesioan gorde (@SessionAttributes("koadernoAktiboa") dela eta)
	    model.addAttribute("koadernoAktiboa", koadernoa);

	    // 'next' DEKODETU eta modu seguruan balidatu
	    String next = null;
	    if (nextEncoded != null && !nextEncoded.isBlank()) {
	        next = java.net.URLDecoder.decode(nextEncoded, java.nio.charset.StandardCharsets.UTF_8);
	    }

	    if (isSafeInternal(next)) {
	        return "redirect:" + next;
	    }
	    return "redirect:/irakasle";
	}
	
	private boolean isSafeInternal(String next) {
	    if (next == null) return false;
	    // injekzio saiakerak/kanpoko URL-ak baztertu
	    if (next.contains("\r") || next.contains("\n")) return false;
	    if (next.startsWith("http://") || next.startsWith("https://")) return false;
	    if (!next.startsWith("/")) return false;

	    // loopak saihestu eta bide zentzudunak onartu
	    if (next.startsWith("/irakasle/koaderno/")) return false;
	    // Nahi baduzu, hemen murriztu: /irakasle... soilik
	    if (!next.startsWith("/irakasle")) return false;

	    return true;
	}
	
	@GetMapping("/berria")
	public String erakutsiFormularioa(Authentication auth, Model model) {
		Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        model.addAttribute("moduluak", koadernoaService.lortuErabilgarriDaudenModuluak(irakaslea));
        model.addAttribute("irakasleAukeragarriak", koadernoaService.lortuFamiliaBerekoIrakasleak(irakaslea));
        model.addAttribute("irakasleLogeatua", irakaslea);
        model.addAttribute("koadernoaDto", new KoadernoaSortuDto());
        //Ordutegia zehazteko:
        model.addAttribute("rows", IntStream.rangeClosed(1, 12).boxed().toList());
        model.addAttribute("cols", ASTE_ORDENA);
        model.addAttribute("selected", Set.of()); //hasiera hutsa
        return "irakasleak/koadernoa-sortu";
    }

	@PostMapping("/berria")
	public String submit(@ModelAttribute("koadernoaDto") KoadernoaSortuDto dto,
	                     Authentication auth,
	                     @RequestParam(name = "cells", required = false) List<String> cells) {

	    Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);

	    // 1) Koadernoa sortu
	    Koadernoa berria = koadernoaService.sortuKoadernoa(
	            dto,
	            irakaslea,
	            cells == null ? List.of() : cells
	    );

	    // 2) Taldeko ikasleak automatikoki sinkronizatu (badagoenik balego)
	    try {
	        ikasleaService.syncKoadernoBakarra(berria.getId());
	    } catch (Exception e) {
	        // nahi baduzu, log-ean utzi; baina ez bota koadernoaren sorrera atzera
	        // log.error("Ezin izan dira taldeko ikasleak inportatu automatikoki", e);
	    }

	    return "redirect:/irakasle";
	}
	
	@PostMapping("/{id}/ordutegia/cell")
    @ResponseBody
    public ResponseEntity<?> toggleCell(@PathVariable Long id,
                                        @RequestParam int col,
                                        @RequestParam int row,
                                        @RequestParam boolean selected) {
        koadernoaService.setSlotSelected(id, col, row, selected);
        return ResponseEntity.ok(Map.of("ok", true));
    }


	
	@PostMapping("/{id}/inportatu-taldetik")
	public String inportatuTaldekoIkasleakKoadernoan(@PathVariable("id") Long koadernoaId,
	                                                 RedirectAttributes ra) {
	    var res = ikasleaService.syncKoadernoBakarra(koadernoaId); 

	    if (res.ohartarazpena() != null) {
	        ra.addFlashAttribute("errorea", res.ohartarazpena());
	    } else if (res.sortuak() > 0) {
	        ra.addFlashAttribute("msg", res.sortuak() + " ikasle matrikulatu dira koaderno honetan (sinkronizatuta).");
	    } else {
	        ra.addFlashAttribute("msg", "Koadernoa sinkronizatuta: ez zegoen aldaketarik.");
	    }
	    return "redirect:/irakasle/ikasleak";
	}
	
	@PostMapping("/{id}/partekatu")
    public String partekatuKoadernoa(
            @PathVariable Long id,
            @RequestParam("ident") String ident,
            Authentication auth,
            RedirectAttributes ra) {

        // 1) Uneko irakaslea (zure service-ak dagoeneko OAuth2 / username bideratzen du)
        Irakaslea uneKoIrakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);

        // 2) Koadernoa ekarri
        Koadernoa koadernoa = koadernoaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Koadernoa ez da aurkitu"));

        if (koadernoa.getIrakasleak() == null) {
            koadernoa.setIrakasleak(new ArrayList<>());
        }

        // 3) Egiaztatu une-ko irakaslea koadernoaren irakasle-zerrendan dagoela
        boolean uneanBadago = koadernoa.getIrakasleak().stream()
                .anyMatch(i -> i.getId().equals(uneKoIrakaslea.getId()));

        if (!uneanBadago) {
            ra.addFlashAttribute("error", "Ez duzu koaderno hau partekatzeko baimenik.");
            return "redirect:/irakasle";
        }

        // 4) Bilatu gehitu nahi duzun irakaslea (email edo izenaren arabera)
        var targetOpt = irakasleaService.bilatuIdent(ident);
        if (targetOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Ez da irakaslerik aurkitu: \"" + ident + "\".");
            return "redirect:/irakasle";
        }
        Irakaslea target = targetOpt.get();

        // 5) Dagoeneko badago?
        boolean already = koadernoa.getIrakasleak().stream()
                .anyMatch(i -> i.getId().equals(target.getId()));

        if (already) {
            ra.addFlashAttribute("success", "Irakaslea jada badago koaderno honetan.");
            return "redirect:/irakasle";
        }

        // 6) Gehitu eta gorde
        koadernoa.getIrakasleak().add(target);
        koadernoaRepository.save(koadernoa);

        String izenOsoa = target.getIzena(); // edo target.getIzenaOsoa(), zuk daukazunaren arabera
        ra.addFlashAttribute("success",
                "Koadernoa partekatu da irakasle honekin: " + izenOsoa + ".");

        return "redirect:/irakasle";
    }
	
	@PostMapping("/{id}/irten")
	public String utziKoadernoa(@PathVariable Long id,
	                            Authentication auth,
	                            Model model,
	                            RedirectAttributes ra) {

	    // 1) Nor dago logeatuta?
	    Irakaslea ni = irakasleaService.getLogeatutaDagoenIrakaslea(auth);

	    // 2) Koadernoa hartu
	    Koadernoa k = koadernoaService.findById(id);
	    if (k == null) {
	        ra.addFlashAttribute("error", "Koadernoa ez da existitzen.");
	        return "redirect:/irakasle";
	    }

	    // 3) Egiaztatu ni koaderno honetako irakaslea naizela
	    boolean nireKoadernoaDa = k.getIrakasleak().stream()
	            .anyMatch(ir -> ir.getId().equals(ni.getId()));

	    if (!nireKoadernoaDa) {
	        ra.addFlashAttribute("error", "Koaderno honetako irakasle ez zara.");
	        return "redirect:/irakasle";
	    }

	    // 4) Ez utzi azken irakaslea izaten
	    if (k.getIrakasleak().size() <= 1) {
	        ra.addFlashAttribute("error",
	                "Ezin duzu koaderno hau utzi; bestela irakaslerik gabe geratuko litzateke.");
	        return "redirect:/irakasle";
	    }

	    // 5) Nire burua zerrendatik kendu eta gorde
	    k.getIrakasleak().removeIf(ir -> ir.getId().equals(ni.getId()));
	    koadernoaRepository.save(k); 

	    // 6) Nire beste koadernoak bilatu
	    List<Koadernoa> nireKoadernoak = koadernoaRepository.findAllByIrakasleak_Id(ni.getId());

	    if (nireKoadernoak.isEmpty()) {
	        // koaderno aktiborik EZ → sesioan null jartzen dugu
	        model.addAttribute("koadernoAktiboa", null);
	        return "redirect:/login?logout";
	    } else {
	        // beste bat badago → lehenengoa aktibo jarri (edo zuk nahi duzuna)
	        model.addAttribute("koadernoAktiboa", nireKoadernoak.get(0));
	    }

	    ra.addFlashAttribute("success", "Koaderno hau utzi duzu.");
	    return "redirect:/irakasle";
	}


	
	@PostMapping("/{id}/ezabatu")
	public String ezabatuKoadernoa(@PathVariable Long id,
	                               @RequestParam("confirmIzena") String confirmIzena,
	                               @RequestParam("confirmIkasturtea") String confirmIkasturtea,
	                               Authentication auth,
	                               RedirectAttributes ra) {

	    // 1) Koadernoa existitzen den egiaztatu
	    Koadernoa k = koadernoaService.findById(id);
	    if (k == null) {
	        ra.addFlashAttribute("error", "Koadernoa ez da existitzen.");
	        return "redirect:/irakasle";
	    }

	    // 2) Irakasleak baimena duen egiaztatu
	    Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
	    if (!koadernoaService.irakasleakBadaukaSarbidea(irakaslea, k)) {
	        ra.addFlashAttribute("error", "Ez duzu koaderno hau ezabatzeko baimenik.");
	        return "redirect:/irakasle";
	    }

	    // 3) Confirm testuak konparatu (trim + case-sensitive)
	    String izenaEsperotakoa = k.getIzena() != null ? k.getIzena().trim() : "";
	    String ikasEsperotakoa =
	            (k.getEgutegia() != null &&
	             k.getEgutegia().getIkasturtea() != null &&
	             k.getEgutegia().getIkasturtea().getIzena() != null)
	                    ? k.getEgutegia().getIkasturtea().getIzena().trim()
	                    : "";

	    String izenaUser = confirmIzena != null ? confirmIzena.trim() : "";
	    String ikasUser  = confirmIkasturtea != null ? confirmIkasturtea.trim() : "";

	    if (!izenaEsperotakoa.equals(izenaUser) || !ikasEsperotakoa.equals(ikasUser)) {
	        ra.addFlashAttribute("error",
	            "Baieztapen testuak ez datoz bat. Idatzi zehazki:\n" +
	            "Koadernoaren izena: \"" + izenaEsperotakoa + "\"\n" +
	            "Ikasturtea: \"" + ikasEsperotakoa + "\"");
	        return "redirect:/irakasle";
	    }

	    // 4) Benetako ezabaketa + debug
	    try {
	        System.out.println(">>> KOADERNOA EZABATZEN: id=" + k.getId()
	                + ", izena=" + izenaEsperotakoa);

	        koadernoaService.ezabatuKoadernoa(k);

	        ra.addFlashAttribute("success", "Koadernoa ondo ezabatu da.");
	    } catch (org.springframework.dao.DataIntegrityViolationException ex) {
	        ex.printStackTrace(); // log-ean ikusteko
	        ra.addFlashAttribute("error",
	            "Ezin izan da koadernoa ezabatu: badirudi badituela erlazionatutako datuak " +
	            "(jarduerak, asistentziak, notak...).");
	    } catch (Exception ex) {
	        ex.printStackTrace();
	        ra.addFlashAttribute("error", "Ezin izan da koadernoa ezabatu: " + ex.getMessage());
	    }

	    return "redirect:/irakasle";
	}

}
	
