package com.koadernoa.app.funtzionalitateak.irakasle;

import java.security.Principal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.service.EgutegiaService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoOrdutegiBlokea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoaService;
import com.koadernoa.app.objektuak.modulua.service.ModuloaService;
import com.koadernoa.app.objektuak.zikloak.service.TaldeaService;
import com.koadernoa.app.objektuak.zikloak.service.ZikloaService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle")
@RequiredArgsConstructor
@SessionAttributes("koadernoAktiboa") //<- KOADERNOA SESIOAN GORDE
public class IrakasleController {

	private final IrakasleaService irakasleaService;
	private final KoadernoaService koadernoaService;
	private final KoadernoaRepository koadernoaRepository;
	private final EgutegiaService egutegiaService;
	
	private static final List<Astegunak> ASTE_ORDENA = List.of(
            Astegunak.ASTELEHENA, Astegunak.ASTEARTEA, Astegunak.ASTEAZKENA,
            Astegunak.OSTEGUNA, Astegunak.OSTIRALA
    );
	
	
	@GetMapping("/koaderno/{id}")
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


    @GetMapping({"/", ""})
    public String index(Model model, Authentication auth, @ModelAttribute("koadernoAktiboa") Koadernoa koadernoAktiboa) {
    	String emaila = null;

        if (auth.getPrincipal() instanceof OAuth2User oAuth2User) {
            emaila = oAuth2User.getAttribute("email");
        } else {
            emaila = auth.getName(); // fallback, adib. test lokaletan
        }

        Koadernoa koadernoa = koadernoaRepository.findWithOrdutegiaById(koadernoAktiboa.getId()).orElseThrow();
        
        model.addAttribute("ikasturteaIzena",
        		koadernoa.getEgutegia()!=null && koadernoa.getEgutegia().getIkasturtea()!=null
                        ? koadernoa.getEgutegia().getIkasturtea().getIzena() : "(ikasturterik ez)");
        
        model.addAttribute("rows", IntStream.rangeClosed(1, 12).boxed().toList());
        model.addAttribute("cols", ASTE_ORDENA);
        // hautatutako slot-ak set batean
        Set<String> selected = new HashSet<>();
        if (koadernoa.getOrdutegiak() != null) {
            for (KoadernoOrdutegiBlokea b : koadernoa.getOrdutegiak()) {
                int col = ASTE_ORDENA.indexOf(b.getAsteguna()) + 1;
                for (int s = b.getHasieraSlot(); s <= b.bukaeraSlot(); s++) {
                    selected.add(col + "-" + s);
                }
            }
        }
        model.addAttribute("selected", selected);
        model.addAttribute("editable", false); // defektuz blokeatuta

        
        Irakaslea irakaslea = irakasleaService.findByEmaila(emaila);
        model.addAttribute("irakaslea", irakaslea);
        model.addAttribute("koadernoAktiboDago", koadernoAktiboa != null);

        return "irakasleak/index";
    }
    
    @GetMapping("/egutegia")
    public String koadernoarenEgutegia(@ModelAttribute("koadernoAktiboa") Koadernoa koadernoa, Model model) {
    	
    	if (koadernoa == null) {
            model.addAttribute("errorea", "Ez dago koaderno aktiborik aukeratuta.");
            return "error/404";
        }

        Egutegia egutegia = koadernoa.getEgutegia();

        Map<String, List<List<LocalDate>>> hilabeteka = egutegiaService.prestatuHilabetekoEgutegiak(egutegia);
        Map<String, String> klaseak = egutegiaService.kalkulatuKlaseak(egutegia);
        Map<String, String> deskribapenaMap = egutegia.getEgunBereziak().stream()
            .collect(Collectors.toMap(
                eb -> eb.getData().toString(),
                EgunBerezi::getDeskribapena,
                (a, b) -> a
            ));

        model.addAttribute("egutegia", egutegia);
        model.addAttribute("ikasturtea", egutegia.getIkasturtea());
        model.addAttribute("hilabeteka", hilabeteka);
        model.addAttribute("klaseMap", klaseak);
        model.addAttribute("deskribapenaMap", deskribapenaMap);

        return "irakasleak/egutegia/egutegi-fitxa";
    }
}
