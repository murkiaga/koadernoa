package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaProvisioningService;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaProvisioningService.IrakasleaDagoenekoBadagoException;
import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;
import com.koadernoa.app.objektuak.zikloak.repository.FamiliaRepository;
import com.koadernoa.app.security.ActiveDirectoryBilaketaService;
import com.koadernoa.app.security.AdErabiltzailea;
import com.koadernoa.app.security.AuthProviderStatusService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/kudeatzaile/irakasleak/ad")
public class AdIrakasleKudeatzaileController {

    private final ActiveDirectoryBilaketaService adService;
    private final AuthProviderStatusService authStatus;
    private final FamiliaRepository familiaRepository;
    private final IrakasleaProvisioningService provisioningService;

    @GetMapping
    public String formularioa(Model model, RedirectAttributes ra) {
        if (!adErabilgarri()) return ezErabilgarri(ra);
        prestatu(model, "", List.of());
        return "kudeatzaile/irakasleak/ad-gehitu";
    }

    @PostMapping("/bilatu")
    public String bilatu(@RequestParam(name = "bilaketa", defaultValue = "") String bilaketa,
                         Model model, RedirectAttributes ra) {
        if (!adErabilgarri()) return ezErabilgarri(ra);
        try {
            List<AdErabiltzailea> emaitzak = adService.bilatu(bilaketa);
            prestatu(model, bilaketa, emaitzak);
            if (emaitzak.isEmpty()) model.addAttribute("error", "Ez da erabiltzailerik aurkitu Active Directoryn.");
            return "kudeatzaile/irakasleak/ad-gehitu";
        } catch (IllegalArgumentException ex) {
            prestatu(model, bilaketa, List.of());
            model.addAttribute("error", ex.getMessage());
            return "kudeatzaile/irakasleak/ad-gehitu";
        } catch (RuntimeException ex) {
            prestatu(model, bilaketa, List.of());
            model.addAttribute("error", "Ezin izan da Active Directory zerbitzaria kontsultatu. Egiaztatu konexioa eta bind konfigurazioa.");
            return "kudeatzaile/irakasleak/ad-gehitu";
        }
    }

    @PostMapping("/gehitu")
    public String gehitu(@RequestParam(name = "identifikatzailea", required = false) String identifikatzailea,
                         @RequestParam(name = "mintegiaId", required = false) Long mintegiaId,
                         RedirectAttributes ra) {
        if (!adErabilgarri()) return ezErabilgarri(ra);
        if (!StringUtils.hasText(identifikatzailea)) {
            ra.addFlashAttribute("error", "Aukeratu Active Directoryko erabiltzaile bat.");
            return "redirect:/kudeatzaile/irakasleak/ad";
        }
        if (mintegiaId == null) {
            ra.addFlashAttribute("error", "Mintegia aukeratzea derrigorrezkoa da.");
            return "redirect:/kudeatzaile/irakasleak/ad";
        }
        Familia mintegia = familiaRepository.findById(mintegiaId).filter(Familia::isAktibo).orElse(null);
        if (mintegia == null) {
            ra.addFlashAttribute("error", "Aukeratutako Mintegia ez da baliozkoa.");
            return "redirect:/kudeatzaile/irakasleak/ad";
        }
        AdErabiltzailea erabiltzailea;
        try {
            erabiltzailea = adService.bilatuZehatza(identifikatzailea);
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", "Ezin izan da Active Directory zerbitzaria kontsultatu. Saiatu berriro geroago.");
            return "redirect:/kudeatzaile/irakasleak/ad";
        }
        if (erabiltzailea == null) {
            ra.addFlashAttribute("error", "Active Directoryko erabiltzailea ez da existitzen edo jada ez dago erabilgarri.");
            return "redirect:/kudeatzaile/irakasleak/ad";
        }
        if (!StringUtils.hasText(erabiltzailea.emaila())) {
            ra.addFlashAttribute("error", "Active Directoryko erabiltzaileak ez du email edo userPrincipalName balio erabilgarririk.");
            return "redirect:/kudeatzaile/irakasleak/ad";
        }
        try {
            provisioningService.sortuEskuz(erabiltzailea.emaila(), erabiltzailea.izenOsoa(), mintegia);
            ra.addFlashAttribute("success", "Irakaslea ondo gehitu da Active Directorytik.");
            return "redirect:/kudeatzaile/irakasleak";
        } catch (IrakasleaDagoenekoBadagoException ex) {
            ra.addFlashAttribute("error", "Irakaslea dagoeneko Koadernoan erregistratuta dago.");
            return "redirect:/kudeatzaile/irakasleak/ad";
        } catch (DataAccessException ex) {
            ra.addFlashAttribute("error", "Ezin izan da irakaslea Koadernoan gorde. Saiatu berriro geroago.");
            return "redirect:/kudeatzaile/irakasleak/ad";
        }
    }

    private void prestatu(Model model, String bilaketa, List<AdErabiltzailea> emaitzak) {
        model.addAttribute("bilaketa", bilaketa);
        model.addAttribute("emaitzak", emaitzak);
        model.addAttribute("familiaGuztiak", familiaRepository.findAllByAktiboTrueOrderByIzenaAsc());
    }

    private boolean adErabilgarri() {
        return authStatus.isLdapEnabled() && authStatus.isLdapConfigured();
    }

    private String ezErabilgarri(RedirectAttributes ra) {
        ra.addFlashAttribute("error", "Active Directory autentifikazioa ez dago gaituta edo konfiguratuta.");
        return "redirect:/kudeatzaile/irakasleak";
    }
}
