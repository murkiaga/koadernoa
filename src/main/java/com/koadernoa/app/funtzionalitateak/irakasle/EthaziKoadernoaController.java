package com.koadernoa.app.funtzionalitateak.irakasle;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.ethazi.entitateak.EthaziKoadernoa;
import com.koadernoa.app.ethazi.dto.KronogramaJardueraForm;
import com.koadernoa.app.ethazi.entitateak.kronograma.KronogramaJardueraMota;
import com.koadernoa.app.ethazi.service.EthaziKoadernoaService;
import com.koadernoa.app.ethazi.service.KronogramaService;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.repository.EgutegiaRepository;
import com.koadernoa.app.objektuak.egutegia.service.EgutegiaService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.repository.IkasleaRepository;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle/ethazi-koadernoa")
@RequiredArgsConstructor
public class EthaziKoadernoaController {
    public static final String KOADERNO_MOTA = "koadernoMotaAktiboa";
    public static final String ETHAZI_MOTA = "ETHAZI";
    public static final String ARRUNTA_MOTA = "ARRUNTA";

    private final EthaziKoadernoaService ethaziKoadernoaService;
    private final IrakasleaService irakasleaService;
    private final IkasleaRepository ikasleaRepository;
    private final ModuloaRepository moduloaRepository;
    private final EgutegiaRepository egutegiaRepository;
    private final EgutegiaService egutegiaService;
    private final KronogramaService kronogramaService;

    @GetMapping("/{id}/hautatu")
    public String hautatu(@PathVariable Long id, Authentication auth, HttpSession session,
            RedirectAttributes flash) {
        EthaziKoadernoa koadernoa = ethaziKoadernoaService.findById(id);
        if (koadernoa == null) {
            flash.addFlashAttribute("error", "Ethazi koadernoa ez da existitzen.");
            return "redirect:/irakasle";
        }
        Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        if (!adminEdoKudeatzaileaDa(auth)
                && !ethaziKoadernoaService.irakasleakBadaukaSarbidea(irakaslea, koadernoa)) {
            flash.addFlashAttribute("error", "Ez duzu Ethazi koaderno honetarako sarbiderik.");
            return "redirect:/irakasle";
        }
        session.setAttribute("ethaziKoadernoAktiboa", koadernoa);
        session.setAttribute(KOADERNO_MOTA, ETHAZI_MOTA);
        session.removeAttribute("koadernoAktiboa");
        return "redirect:/irakasle/ethazi-koadernoa";
    }

    @GetMapping({"", "/"})
    public String hasiera(Authentication auth,
            @SessionAttribute(name = "ethaziKoadernoAktiboa", required = false) EthaziKoadernoa sessionKoadernoa,
            HttpSession session, Model model, RedirectAttributes flash) {
        if (sessionKoadernoa == null || sessionKoadernoa.getId() == null) {
            return "redirect:/irakasle";
        }
        EthaziKoadernoa koadernoa = ethaziKoadernoaService.findById(sessionKoadernoa.getId());
        Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        if (koadernoa == null || (!adminEdoKudeatzaileaDa(auth)
                && !ethaziKoadernoaService.irakasleakBadaukaSarbidea(irakaslea, koadernoa))) {
            session.removeAttribute("ethaziKoadernoAktiboa");
            session.removeAttribute(KOADERNO_MOTA);
            flash.addFlashAttribute("error", "Ethazi koaderno aktiboa ez dago erabilgarri.");
            return "redirect:/irakasle";
        }
        session.setAttribute("ethaziKoadernoAktiboa", koadernoa);
        session.setAttribute(KOADERNO_MOTA, ETHAZI_MOTA);
        model.addAttribute("ethaziKoadernoAktiboa", koadernoa);
        model.addAttribute("ikuspegia", ethaziKoadernoaService.hasieraIkuspegia(koadernoa));
        return "irakasleak/ethazi/hasiera";
    }

    @GetMapping("/ikasleak")
    public String ikasleak(Authentication auth,
            @SessionAttribute(name = "ethaziKoadernoAktiboa", required = false) EthaziKoadernoa sessionKoadernoa,
            HttpSession session, Model model, RedirectAttributes flash) {
        EthaziKoadernoa koadernoa = lortuBaimendutakoKoadernoa(sessionKoadernoa, auth, session, flash);
        if (koadernoa == null) return "redirect:/irakasle";

        var ikasleak = ikasleaRepository
                .findByTaldea_IdOrderByAbizena1AscAbizena2AscIzenaAsc(koadernoa.getTaldea().getId());
        model.addAttribute("ethaziKoadernoAktiboa", koadernoa);
        model.addAttribute("ikasleak", ikasleak);
        model.addAttribute("kop", ikasleak.size());
        return "irakasleak/ethazi/ikasleak";
    }

    @GetMapping("/egutegia")
    public String egutegia(Authentication auth,
            @SessionAttribute(name = "ethaziKoadernoAktiboa", required = false) EthaziKoadernoa sessionKoadernoa,
            HttpSession session, Model model, RedirectAttributes flash) {
        EthaziKoadernoa koadernoa = lortuBaimendutakoKoadernoa(sessionKoadernoa, auth, session, flash);
        if (koadernoa == null) return "redirect:/irakasle";
        model.addAttribute("ethaziKoadernoAktiboa", koadernoa);

        Egutegia egutegia = lortuTaldearenEgutegia(koadernoa);
        if (egutegia == null) {
            model.addAttribute("egutegiErrorea", "Taldearen mailak ez du ikasturte aktiborako egutegirik.");
            return "irakasleak/ethazi/egutegia";
        }

        model.addAttribute("egutegia", egutegia);
        model.addAttribute("ikasturtea", egutegia.getIkasturtea());
        model.addAttribute("hilabeteka", egutegiaService.prestatuHilabetekoEgutegiak(egutegia));
        model.addAttribute("klaseMap", egutegiaService.kalkulatuKlaseak(egutegia));
        model.addAttribute("oharraMap", oharMap(egutegia));
        model.addAttribute("deskribapenaMap", deskribapenMap(egutegia));
        return "irakasleak/ethazi/egutegia";
    }

    @GetMapping("/programazioa")
    public String programazioa(Authentication auth,
            @SessionAttribute(name = "ethaziKoadernoAktiboa", required = false) EthaziKoadernoa sessionKoadernoa,
            HttpSession session, Model model, RedirectAttributes flash) {
        EthaziKoadernoa koadernoa = lortuBaimendutakoKoadernoa(sessionKoadernoa, auth, session, flash);
        if (koadernoa == null) return "redirect:/irakasle";
        model.addAttribute("ethaziKoadernoAktiboa", koadernoa);
        Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        model.addAttribute("programazioaIkuspegia",
                ethaziKoadernoaService.programazioaIkuspegia(
                        koadernoa, lortuTaldearenEgutegia(koadernoa), irakaslea));
        return "irakasleak/ethazi/programazioa";
    }

    @PostMapping("/programazioa/erronkak/{erronkaId}/jarduerak/gorde")
    public String programazioJardueraGorde(@PathVariable Long erronkaId,
            @ModelAttribute KronogramaJardueraForm form, Authentication auth,
            RedirectAttributes flash) {
        form.setMota(KronogramaJardueraMota.MODULUA);
        form.setAurreikusitakoModuloaId(null);
        try {
            kronogramaService.gorde(erronkaId, form, auth);
            flash.addFlashAttribute("success", form.getId() == null
                    ? "Jarduera sortu da." : "Jarduera eguneratu da.");
        } catch (IllegalArgumentException | AccessDeniedException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/irakasle/ethazi-koadernoa/programazioa";
    }

    @PostMapping("/programazioa/erronkak/{erronkaId}/jarduerak/berrordenatu")
    public ResponseEntity<Map<String, String>> programazioJarduerakBerrordenatu(
            @PathVariable Long erronkaId, @RequestBody BerrordenatuEskaera eskaera,
            Authentication auth) {
        try {
            kronogramaService.berrordenatu(erronkaId, eskaera.moduloaId(), eskaera.jardueraIds(), auth);
            return ResponseEntity.ok(Map.of("message", "Ordena eta datak eguneratu dira."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(403).body(Map.of("message", ex.getMessage()));
        }
    }

    public record BerrordenatuEskaera(Long moduloaId, java.util.List<Long> jardueraIds) {}

    private Egutegia lortuTaldearenEgutegia(EthaziKoadernoa koadernoa) {
        Long mailaId = moduloaRepository.findByTaldeaId(koadernoa.getTaldea().getId()).stream()
                .filter(Moduloa::isAktibo)
                .map(Moduloa::getMaila)
                .filter(java.util.Objects::nonNull)
                .map(m -> m.getId())
                .findFirst()
                .orElse(null);
        return mailaId == null ? null
                : egutegiaRepository.findByIkasturtea_AktiboaTrueAndMaila_Id(mailaId).orElse(null);
    }

    private EthaziKoadernoa lortuBaimendutakoKoadernoa(EthaziKoadernoa sessionKoadernoa,
            Authentication auth, HttpSession session, RedirectAttributes flash) {
        if (sessionKoadernoa == null || sessionKoadernoa.getId() == null) return null;
        EthaziKoadernoa koadernoa = ethaziKoadernoaService.findById(sessionKoadernoa.getId());
        Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        if (koadernoa == null || (!adminEdoKudeatzaileaDa(auth)
                && !ethaziKoadernoaService.irakasleakBadaukaSarbidea(irakaslea, koadernoa))) {
            session.removeAttribute("ethaziKoadernoAktiboa");
            session.removeAttribute(KOADERNO_MOTA);
            flash.addFlashAttribute("error", "Ethazi koaderno aktiboa ez dago erabilgarri.");
            return null;
        }
        session.setAttribute("ethaziKoadernoAktiboa", koadernoa);
        session.setAttribute(KOADERNO_MOTA, ETHAZI_MOTA);
        return koadernoa;
    }

    private Map<String, String> oharMap(Egutegia egutegia) {
        Map<String, String> emaitza = new HashMap<>();
        if (egutegia.getEgunBereziak() == null) return emaitza;
        for (EgunBerezi eguna : egutegia.getEgunBereziak()) {
            if (eguna.getData() != null && eguna.getDeskribapena() != null
                    && !eguna.getDeskribapena().isBlank()) {
                emaitza.put(eguna.getData().toString(), eguna.getDeskribapena().trim());
            }
        }
        return emaitza;
    }

    private Map<String, String> deskribapenMap(Egutegia egutegia) {
        Map<String, String> emaitza = new HashMap<>();
        if (egutegia.getEgunBereziak() == null) return emaitza;
        Map<String, String> oharrak = oharMap(egutegia);
        for (EgunBerezi eguna : egutegia.getEgunBereziak()) {
            if (eguna.getData() == null || eguna.getMota() == null) continue;
            String testua = switch (eguna.getMota()) {
                case JAIEGUNA -> "Jaieguna";
                case EZ_LEKTIBOA -> "Ez-lektiboa";
                case LEKTIBOA -> "Lektiboa";
                case ORDEZKATUA -> "Ordezkatua" + (eguna.getOrdezkatua() == null
                        ? "" : " (" + eguna.getOrdezkatua().name() + ")");
            };
            String oharra = oharrak.get(eguna.getData().toString());
            emaitza.put(eguna.getData().toString(), oharra == null ? testua : testua + " — " + oharra);
        }
        return emaitza;
    }

    private boolean adminEdoKudeatzaileaDa(Authentication auth) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_KUDEATZAILEA".equals(a.getAuthority()));
    }
}
