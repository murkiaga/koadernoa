package com.koadernoa.app.funtzionalitateak.irakasle;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.ethazi.entitateak.EthaziKoadernoa;
import com.koadernoa.app.ethazi.dto.KronogramaJardueraForm;
import com.koadernoa.app.ethazi.dto.JardueraMultzoForm;
import com.koadernoa.app.ethazi.entitateak.kronograma.KronogramaJardueraMota;
import com.koadernoa.app.ethazi.service.EthaziKoadernoaService;
import com.koadernoa.app.ethazi.service.EthaziDenboralizazioaService;
import com.koadernoa.app.ethazi.service.EthaziJarraipenaService;
import com.koadernoa.app.ethazi.service.KronogramaService;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.repository.EgutegiaRepository;
import com.koadernoa.app.objektuak.egutegia.service.EgutegiaService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
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
    private final EthaziJarraipenaService ethaziJarraipenaService;
    private final EthaziDenboralizazioaService ethaziDenboralizazioaService;

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

    @GetMapping("/denboralizazioa")
    public String denboralizazioa(Authentication auth,
            @SessionAttribute(name = "ethaziKoadernoAktiboa", required = false) EthaziKoadernoa sessionKoadernoa,
            @RequestParam(name = "moduloaId", required = false) Long moduloaId,
            @RequestParam(name = "urtea", required = false) Integer urtea,
            @RequestParam(name = "hilabetea", required = false) Integer hilabetea,
            @RequestParam(name = "bista", required = false, defaultValue = "egutegia") String bista,
            @RequestParam(name = "egutegiMota", required = false) String egutegiMota,
            @RequestParam(name = "asteHasiera", required = false) LocalDate asteHasiera,
            HttpSession session, Model model, RedirectAttributes flash) {
        EthaziKoadernoa koadernoa = lortuBaimendutakoKoadernoa(sessionKoadernoa, auth, session, flash);
        if (koadernoa == null) return "redirect:/irakasle";
        Egutegia egutegia = lortuTaldearenEgutegia(koadernoa);
        if (egutegia == null) {
            model.addAttribute("egutegiErrorea", "Taldearen mailak ez du ikasturte aktiborako egutegirik.");
            model.addAttribute("ethaziKoadernoAktiboa", koadernoa);
            return "irakasleak/ethazi/denboralizazioa";
        }
        LocalDate gaur = LocalDate.now();
        int ikuspegiUrtea = urtea == null ? gaur.getYear() : urtea;
        int ikuspegiHilabetea = hilabetea == null ? gaur.getMonthValue() : hilabetea;
        if (ikuspegiHilabetea < 1 || ikuspegiHilabetea > 12) {
            ikuspegiUrtea += ikuspegiHilabetea < 1 ? -1 : 1;
            ikuspegiHilabetea = ikuspegiHilabetea < 1 ? 12 : 1;
        }
        Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        if (egutegiMota == null || egutegiMota.isBlank()) {
            Object gordetakoMota = session.getAttribute("ethaziDenboralizazioaEgutegiMota");
            egutegiMota = gordetakoMota instanceof String mota && !mota.isBlank()
                    ? mota : "hilabetea";
        } else {
            egutegiMota = "astea".equalsIgnoreCase(egutegiMota) ? "astea" : "hilabetea";
            session.setAttribute("ethaziDenboralizazioaEgutegiMota", egutegiMota);
        }
        model.addAttribute("ethaziKoadernoAktiboa", koadernoa);
        boolean faltakBista = "faltak".equalsIgnoreCase(bista);
        model.addAttribute("bista", faltakBista ? "faltak" : "egutegia");
        var denboralizazioa = ethaziDenboralizazioaService.prestatu(
                koadernoa, egutegia, irakaslea, moduloaId, ikuspegiUrtea, ikuspegiHilabetea);
        LocalDate asteHasieraEraginkorra = asteHasiera == null ? null
                : asteHasiera.with(java.time.DayOfWeek.MONDAY);
        if ("astea".equals(egutegiMota) && asteHasieraEraginkorra == null) {
            asteHasieraEraginkorra = denboralizazioa.asteak().stream()
                    .filter(astea -> astea.stream().anyMatch(eguna -> eguna.data().equals(gaur)))
                    .findFirst().or(() -> denboralizazioa.asteak().stream()
                            .filter(astea -> astea.stream().anyMatch(
                                    EthaziDenboralizazioaService.Eguna::hilabeteAktibokoa)).findFirst())
                    .flatMap(astea -> astea.stream().findFirst()).map(
                            EthaziDenboralizazioaService.Eguna::data).orElse(null);
        }
        LocalDate aukeratutakoAsteHasiera = asteHasieraEraginkorra;
        var astekoEgunak = "astea".equals(egutegiMota) && aukeratutakoAsteHasiera != null
                ? denboralizazioa.asteak().stream()
                        .filter(astea -> !astea.isEmpty()
                                && astea.get(0).data().equals(aukeratutakoAsteHasiera))
                        .findFirst().orElse(java.util.List.of())
                : java.util.List.<EthaziDenboralizazioaService.Eguna>of();
        model.addAttribute("denboralizazioa", denboralizazioa);
        model.addAttribute("egutegiMota", egutegiMota);
        model.addAttribute("asteHasiera", asteHasieraEraginkorra);
        model.addAttribute("astekoEgunak", astekoEgunak);
        model.addAttribute("ikusgaiAsteak", "astea".equals(egutegiMota)
                ? java.util.List.of(astekoEgunak) : denboralizazioa.asteak());
        model.addAttribute("egutegiOharrak", oharMap(egutegia));
        if (faltakBista) {
            model.addAttribute("jarraipena", ethaziJarraipenaService.prestatu(
                    koadernoa, egutegia, irakaslea, adminEdoKudeatzaileaDa(auth), moduloaId));
        }
        return "irakasleak/ethazi/denboralizazioa";
    }

    @PostMapping("/programazioa/erronkak/{erronkaId}/denboralizaziora-bolkatu")
    public String denboralizazioraBolkatu(@PathVariable Long erronkaId, Authentication auth,
            @SessionAttribute(name = "ethaziKoadernoAktiboa", required = false) EthaziKoadernoa sessionKoadernoa,
            HttpSession session, RedirectAttributes flash) {
        EthaziKoadernoa koadernoa = lortuBaimendutakoKoadernoa(sessionKoadernoa, auth, session, flash);
        if (koadernoa == null) return "redirect:/irakasle";
        try {
            Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
            int egunekoJardueraKopurua = ethaziDenboralizazioaService.bolkatu(
                    koadernoa, lortuTaldearenEgutegia(koadernoa), irakaslea, erronkaId, auth);
            flash.addFlashAttribute("success", "Erronka denboralizaziora bolkatu da ("
                    + egunekoJardueraKopurua + " eguneko jarduera). Aurreko bolkadura ordezkatu da.");
        } catch (IllegalArgumentException | AccessDeniedException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/irakasle/ethazi-koadernoa/programazioa";
    }

    @GetMapping("/denboralizazioa/asistentzia")
    public String denboralizazioAsistentzia(Authentication auth,
            @SessionAttribute(name = "ethaziKoadernoAktiboa", required = false) EthaziKoadernoa sessionKoadernoa,
            @RequestParam Long moduloaId, @RequestParam LocalDate data,
            @RequestParam(required = false) Integer urtea,
            @RequestParam(required = false) Integer hilabetea,
            @RequestParam(required = false) Long filtroModuloaId,
            @RequestParam(required = false) String egutegiMota,
            @RequestParam(required = false) LocalDate asteHasiera,
            HttpSession session, RedirectAttributes flash) {
        EthaziKoadernoa koadernoa = lortuBaimendutakoKoadernoa(sessionKoadernoa, auth, session, flash);
        if (koadernoa == null) return "redirect:/irakasle";
        Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        Koadernoa asistentziaKoadernoa = ethaziDenboralizazioaService
                .lortuAsistentziaKoadernoa(koadernoa, irakaslea, moduloaId);
        if (asistentziaKoadernoa == null) {
            flash.addFlashAttribute("error", "Aukeratutako moduluak ez du asistentzia-koaderno erabilgarririk.");
            return "redirect:/irakasle/ethazi-koadernoa/denboralizazioa?urtea="
                    + (urtea == null ? data.getYear() : urtea) + "&hilabetea="
                    + (hilabetea == null ? data.getMonthValue() : hilabetea)
                    + (filtroModuloaId == null ? "" : "&moduloaId=" + filtroModuloaId);
        }
        try {
            ethaziDenboralizazioaService.sortuEthaziAsistentziaSaioak(
                    koadernoa, lortuTaldearenEgutegia(koadernoa),
                    asistentziaKoadernoa, moduloaId, data);
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/irakasle/ethazi-koadernoa/denboralizazioa?urtea="
                    + (urtea == null ? data.getYear() : urtea) + "&hilabetea="
                    + (hilabetea == null ? data.getMonthValue() : hilabetea)
                    + (filtroModuloaId == null ? "" : "&moduloaId=" + filtroModuloaId);
        }
        session.setAttribute("koadernoAktiboa", asistentziaKoadernoa);
        String itzulera = "&itzuliEthazi=true"
                + (urtea == null ? "" : "&itzuliEthaziUrtea=" + urtea)
                + (hilabetea == null ? "" : "&itzuliEthaziHilabetea=" + hilabetea)
                + (filtroModuloaId == null ? "" : "&itzuliEthaziModuloaId=" + filtroModuloaId)
                + (egutegiMota == null ? "" : "&itzuliEthaziEgutegiMota=" + egutegiMota)
                + (asteHasiera == null ? "" : "&itzuliEthaziAsteHasiera=" + asteHasiera);
        return "redirect:/irakasle/asistentzia?data=" + data + itzulera;
    }

    @GetMapping("/denboralizazioa/jarduerak/{jardueraId}")
    public ResponseEntity<?> denboralizazioJarduera(Authentication auth,
            @SessionAttribute(name = "ethaziKoadernoAktiboa", required = false) EthaziKoadernoa sessionKoadernoa,
            @PathVariable Long jardueraId) {
        EthaziKoadernoa koadernoa = lortuBaimendutakoKoadernoaApi(sessionKoadernoa, auth);
        if (koadernoa == null) return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Ez duzu Ethazi koaderno honetarako sarbiderik."));
        try {
            Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
            return ResponseEntity.ok(ethaziDenboralizazioaService
                    .lortuJarduera(koadernoa, irakaslea, jardueraId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/denboralizazioa/jarduerak/{jardueraId}")
    public ResponseEntity<?> denboralizazioJardueraGorde(Authentication auth,
            @SessionAttribute(name = "ethaziKoadernoAktiboa", required = false) EthaziKoadernoa sessionKoadernoa,
            @PathVariable Long jardueraId, @RequestParam String izenburua,
            @RequestParam(required = false) String deskribapena,
            @RequestParam(required = false) BigDecimal orduak,
            @RequestParam(required = false) String oharrak) {
        EthaziKoadernoa koadernoa = lortuBaimendutakoKoadernoaApi(sessionKoadernoa, auth);
        if (koadernoa == null) return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Ez duzu Ethazi koaderno honetarako sarbiderik."));
        try {
            Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
            ethaziDenboralizazioaService.eguneratuJarduera(
                    koadernoa, irakaslea, jardueraId, izenburua, deskribapena, orduak, oharrak);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/denboralizazioa/jarduerak/{jardueraId}/mugitu")
    public ResponseEntity<?> denboralizazioJardueraMugitu(Authentication auth,
            @SessionAttribute(name = "ethaziKoadernoAktiboa", required = false) EthaziKoadernoa sessionKoadernoa,
            @PathVariable Long jardueraId, @RequestParam LocalDate data) {
        EthaziKoadernoa koadernoa = lortuBaimendutakoKoadernoaApi(sessionKoadernoa, auth);
        if (koadernoa == null) return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Ez duzu Ethazi koaderno honetarako sarbiderik."));
        try {
            Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
            ethaziDenboralizazioaService.mugituJarduera(koadernoa, irakaslea, jardueraId, data);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/jarraipena")
    public String jarraipena(Authentication auth,
            @SessionAttribute(name = "ethaziKoadernoAktiboa", required = false) EthaziKoadernoa sessionKoadernoa,
            @RequestParam(name = "moduloaId", required = false) Long moduloaId,
            HttpSession session, Model model, RedirectAttributes flash) {
        EthaziKoadernoa koadernoa = lortuBaimendutakoKoadernoa(sessionKoadernoa, auth, session, flash);
        if (koadernoa == null) return "redirect:/irakasle";
        return "redirect:/irakasle/ethazi-koadernoa/denboralizazioa?bista=faltak"
                + (moduloaId == null ? "" : "&moduloaId=" + moduloaId);
    }

    @GetMapping("/jarraipena/gelaxka")
    public ResponseEntity<?> jarraipenGelaxka(Authentication auth,
            @SessionAttribute(name = "ethaziKoadernoAktiboa", required = false) EthaziKoadernoa sessionKoadernoa,
            @RequestParam Long ikasleaId,
            @RequestParam Long erronkaId,
            @RequestParam LocalDate data,
            @RequestParam(name = "moduloaId", required = false) Long moduloaId) {
        EthaziKoadernoa koadernoa = lortuBaimendutakoKoadernoaApi(sessionKoadernoa, auth);
        if (koadernoa == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Ez duzu Ethazi koaderno honetarako sarbiderik."));
        }
        try {
            Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
            return ResponseEntity.ok(ethaziJarraipenaService.gelaxkaXehetasuna(
                    koadernoa, irakaslea, adminEdoKudeatzaileaDa(auth),
                    ikasleaId, erronkaId, data, moduloaId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/jarraipena/gelaxka")
    public ResponseEntity<Map<String, String>> jarraipenGelaxkaGorde(Authentication auth,
            @SessionAttribute(name = "ethaziKoadernoAktiboa", required = false) EthaziKoadernoa sessionKoadernoa,
            @RequestBody EthaziJarraipenaService.GelaxkaGordeEskaera eskaera) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "message", "Falten bista kontsultatzeko bakarrik da; asistentzia denboralizaziotik pasatu behar da."));
    }

    @PostMapping("/programazioa/erronkak/{erronkaId}/jarduerak/gorde")
    public String programazioJardueraGorde(@PathVariable Long erronkaId,
            @ModelAttribute KronogramaJardueraForm form, Authentication auth,
            RedirectAttributes flash) {
        form.setMota(KronogramaJardueraMota.MODULUA);
        form.setAurreikusitakoModuloaId(null);
        try {
            kronogramaService.gordeProgramaziotik(erronkaId, form, auth);
            flash.addFlashAttribute("success", form.getId() == null
                    ? "Jarduera sortu da." : "Jarduera eguneratu da.");
        } catch (IllegalArgumentException | AccessDeniedException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/irakasle/ethazi-koadernoa/programazioa";
    }

    @PostMapping("/programazioa/erronkak/{erronkaId}/multzoak/gorde")
    public String programazioMultzoaGorde(@PathVariable Long erronkaId,
            @ModelAttribute JardueraMultzoForm form, Authentication auth,
            RedirectAttributes flash) {
        try {
            kronogramaService.multzoaGorde(erronkaId, form, auth);
            flash.addFlashAttribute("success", form.getId() == null
                    ? "Jarduera-multzoa sortu da." : "Jarduera-multzoa eguneratu da.");
        } catch (IllegalArgumentException | AccessDeniedException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/irakasle/ethazi-koadernoa/programazioa";
    }

    @PostMapping("/programazioa/erronkak/{erronkaId}/multzoak/{multzoaId}/ezabatu")
    public String programazioMultzoaEzabatu(@PathVariable Long erronkaId,
            @PathVariable Long multzoaId, Authentication auth, RedirectAttributes flash) {
        try {
            kronogramaService.multzoaEzabatu(erronkaId, multzoaId, auth);
            flash.addFlashAttribute("success", "Jarduera-multzoa ezabatu da; bertako jarduerak multzokatu gabe geratu dira.");
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
            kronogramaService.jardueraKokatu(erronkaId, eskaera.moduloaId(), eskaera.jardueraId(),
                    eskaera.multzoaId(), eskaera.jardueraIds(), eskaera.programazioJardueraIds(),
                    eskaera.blokeak(), auth);
            return ResponseEntity.ok(Map.of("message", "Ordena eta datak eguneratu dira."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(403).body(Map.of("message", ex.getMessage()));
        }
    }

    public record BerrordenatuEskaera(Long moduloaId, Long jardueraId, Long multzoaId,
            java.util.List<Long> jardueraIds, java.util.List<Long> programazioJardueraIds,
            java.util.List<String> blokeak) {}

    @PostMapping("/programazioa/erronkak/{erronkaId}/multzoak/berrordenatu")
    public ResponseEntity<Map<String, String>> programazioMultzoakBerrordenatu(
            @PathVariable Long erronkaId, @RequestBody ProgramazioBlokeakBerrordenatuEskaera eskaera,
            Authentication auth) {
        try {
            kronogramaService.programazioBlokeakBerrordenatu(
                    erronkaId, eskaera.moduloaId(), eskaera.blokeak(), auth);
            return ResponseEntity.ok(Map.of("message", "Multzoen ordena eta jardueren datak eguneratu dira."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(403).body(Map.of("message", ex.getMessage()));
        }
    }

    public record ProgramazioBlokeakBerrordenatuEskaera(
            Long moduloaId, java.util.List<String> blokeak) {}

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

    private EthaziKoadernoa lortuBaimendutakoKoadernoaApi(EthaziKoadernoa sessionKoadernoa,
            Authentication auth) {
        if (sessionKoadernoa == null || sessionKoadernoa.getId() == null) return null;
        EthaziKoadernoa koadernoa = ethaziKoadernoaService.findById(sessionKoadernoa.getId());
        if (koadernoa == null) return null;
        Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        return adminEdoKudeatzaileaDa(auth)
                || ethaziKoadernoaService.irakasleakBadaukaSarbidea(irakaslea, koadernoa)
                        ? koadernoa : null;
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
