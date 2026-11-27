package com.koadernoa.app.funtzionalitateak.irakasle;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.koadernoa.app.ethazi.entitateak.EthaziKoadernoa;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.GaitasunMota;
import com.koadernoa.app.ethazi.service.EthaziKoadernoaService;
import com.koadernoa.app.ethazi.service.KinielaService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle/ethazi-koadernoa/ebaluazioa")
@RequiredArgsConstructor
public class EthaziEbaluazioaController {

    private final KinielaService kinielaService;
    private final EthaziKoadernoaService ethaziKoadernoaService;
    private final IrakasleaService irakasleaService;

    @GetMapping("/{erronkaId}/errubrikak")
    public String errubrikak(@PathVariable Long erronkaId,
            @RequestParam(required = false) Long moduloaId,
            @RequestParam(defaultValue = "TEKNIKOA") GaitasunMota gaitasunMota,
            @RequestParam(required = false) Long adierazleaId,
            @RequestParam(required = false) Long ebidentziaId,
            @RequestParam(required = false) Long bestelakoProbaId,
            @SessionAttribute(name = "ethaziKoadernoAktiboa", required = false)
                    EthaziKoadernoa sessionKoadernoa,
            Authentication auth, HttpSession session, Model model) {
        if (sessionKoadernoa == null || sessionKoadernoa.getId() == null) {
            return "redirect:/irakasle";
        }
        EthaziKoadernoa koadernoa = ethaziKoadernoaService.findById(sessionKoadernoa.getId());
        Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        if (koadernoa == null || !ethaziKoadernoaService.irakasleakBadaukaSarbidea(irakaslea, koadernoa)) {
            session.removeAttribute("ethaziKoadernoAktiboa");
            session.removeAttribute(EthaziKoadernoaController.KOADERNO_MOTA);
            return "redirect:/irakasle";
        }

        var erronkaAukera = ethaziKoadernoaService.programazioaIkuspegia(koadernoa, null, irakaslea)
                .erronkak().stream()
                .filter(erronka -> erronka.id().equals(erronkaId) && !erronka.moduluAukerak().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Ez duzu erronka hau ebaluatzeko baimenik."));
        Set<Long> irakaslearenModuloIds = erronkaAukera.moduluAukerak().stream()
                .map(EthaziKoadernoaService.ModuluAukera::id)
                .collect(Collectors.toSet());
        var erronka = kinielaService.erronka(erronkaId);
        var moduluak = erronka.getModuluak().stream()
                .filter(modulua -> irakaslearenModuloIds.contains(modulua.getId()))
                .filter(modulua -> kinielaService.erronkaKalifikatuDezake(auth, erronkaId, modulua.getId()))
                .sorted(Comparator.comparing(modulua -> modulua.getKodea() == null ? "" : modulua.getKodea(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (moduluak.isEmpty()) {
            throw new AccessDeniedException("Ez duzu erronka honetako moduluak kalifikatzeko baimenik.");
        }
        if (moduloaId == null) moduloaId = moduluak.get(0).getId();
        final Long hautatutakoModuloaId = moduloaId;
        if (moduluak.stream().noneMatch(modulua -> modulua.getId().equals(hautatutakoModuloaId))) {
            throw new AccessDeniedException("Ez duzu modulu hau ikusteko baimenik.");
        }

        model.addAttribute("koadernoEbaluazioa", true);
        model.addAttribute("irakasleEbaluazioa", true);
        model.addAttribute("ethaziKoadernoAktiboa", koadernoa);
        model.addAttribute("erronka", erronka);
        model.addAttribute("moduluak", moduluak);
        model.addAttribute("moduloaId", moduloaId);
        model.addAttribute("gaitasunMota", gaitasunMota);
        model.addAttribute("fokukoAdierazleaId", adierazleaId);
        model.addAttribute("fokukoEbidentziaId", ebidentziaId);
        model.addAttribute("fokukoBestelakoProbaId", bestelakoProbaId);
        model.addAttribute("errubrikaEditatuDezake", gaitasunMota == GaitasunMota.TEKNIKOA);

        var ikasleTaldeak = kinielaService.errubrikaIkasleTaldeak(erronkaId);
        model.addAttribute("ikasleTaldeak", ikasleTaldeak);
        model.addAttribute("ikasleak", ikasleTaldeak.stream().flatMap(taldea -> taldea.ikasleak().stream()).toList());
        if (gaitasunMota == GaitasunMota.ZEHARKAKOA) {
            model.addAttribute("zeharkakoErrubrikak", kinielaService.zeharkakoErrubrika(erronkaId, moduloaId));
            model.addAttribute("zeharkakoForm", kinielaService.zeharkakoErrubrikaForm(erronkaId));
        } else {
            var errubrika = kinielaService.errubrika(erronkaId, moduloaId);
            model.addAttribute("errubrika", errubrika);
            model.addAttribute("form", kinielaService.errubrikaForm(erronkaId, moduloaId));
            model.addAttribute("adierazleak", kinielaService.modulukoAdierazleak(erronkaId, moduloaId));
            model.addAttribute("taldeak", kinielaService.taldeak(erronkaId));
            model.addAttribute("taldeOrokorOharrak", kinielaService.taldeOrokorOharrak(erronkaId, moduloaId));
            model.addAttribute("ikasleOrokorOharrak", kinielaService.ikasleOrokorOharrak(erronkaId, moduloaId));
            model.addAttribute("bestelakoProbakForm", kinielaService.bestelakoProbakForm(erronkaId, moduloaId));
            model.addAttribute("errubrikaIturriak",
                    errubrika.getMailak().isEmpty() && errubrika.getEbidentziak().isEmpty()
                            ? kinielaService.errubrikaIturriak(erronkaId, moduloaId)
                            : java.util.List.of());
        }
        return "irakasleak/ethazi/errubrikak";
    }
}
