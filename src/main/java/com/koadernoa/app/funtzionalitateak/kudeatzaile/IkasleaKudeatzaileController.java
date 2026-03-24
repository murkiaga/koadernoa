package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioEgoera;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioMomentua;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioNota;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioEgoeraRepository;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioMomentuaRepository;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioNotaRepository;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.logak.entitateak.LogMota;
import com.koadernoa.app.objektuak.logak.service.LogService;
import com.koadernoa.app.objektuak.mezuak.entitateak.Mezua;
import com.koadernoa.app.objektuak.mezuak.repository.MezuaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.entitateak.MatrikulaEgoera;
import com.koadernoa.app.objektuak.modulua.repository.IkasleaRepository;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;
import com.koadernoa.app.objektuak.modulua.service.IkasleaService;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;
import com.koadernoa.app.objektuak.zikloak.repository.ZikloaRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class IkasleaKudeatzaileController {

    private final IkasleaRepository ikasleaRepository;
    private final MatrikulaRepository matrikulaRepository;
    private final IkasturteaRepository ikasturteaRepository;
    private final IkasleaService ikasleaService;
    private final EbaluazioNotaRepository ebaluazioNotaRepository;
    private final EbaluazioMomentuaRepository ebaluazioMomentuaRepository;
    private final EbaluazioEgoeraRepository ebaluazioEgoeraRepository;
    private final MezuaRepository mezuaRepository;
    private final IrakasleaRepository irakasleaRepository;
    private final ZikloaRepository zikloaRepository;
    private final TaldeaRepository taldeaRepository;
    private final LogService logService;

    @GetMapping("/kudeatzaile/ikasleak")
    public String ikasleZerrenda(@RequestParam(name = "zikloaId", required = false) Long zikloaId,
                                 @RequestParam(name = "taldeaId", required = false) Long taldeaId,
                                 @RequestParam(name = "page", defaultValue = "0") int page,
                                 @RequestParam(name = "size", defaultValue = "20") int size,
                                 Model model) {
        int tamaina = Math.min(100, Math.max(20, size));
        int orria = Math.max(0, page);
        PageRequest pageable = PageRequest.of(orria, tamaina, Sort.by("abizena1", "abizena2", "izena").ascending());

        Page<Ikaslea> ikasleak = ikasleaRepository.bilatuKudeatzaile(zikloaId, taldeaId, pageable);

        model.addAttribute("ikasleak", ikasleak.getContent());
        model.addAttribute("zikloak", zikloaRepository.findAllByOrderByIzenaAsc());
        model.addAttribute("taldeak", zikloaId != null
                ? taldeaRepository.findByZikloa_IdOrderByIzenaAsc(zikloaId)
                : taldeaRepository.findAllByOrderByIzenaAsc());
        model.addAttribute("zikloaId", zikloaId);
        model.addAttribute("taldeaId", taldeaId);
        model.addAttribute("currentPage", ikasleak.getNumber());
        model.addAttribute("totalPages", ikasleak.getTotalPages());
        model.addAttribute("pageSize", tamaina);
        model.addAttribute("totalItems", ikasleak.getTotalElements());
        model.addAttribute("pageSizes", List.of(20, 40, 60, 80, 100));

        return "kudeatzaile/ikasleak/index";
    }

    @GetMapping("/kudeatzaile/ikasleak/bilatu")
    @ResponseBody
    public List<Map<String, Object>> bilatuIkasleak(@RequestParam(name = "q", required = false) String q) {
        if (q == null || q.trim().length() < 3) {
            return List.of();
        }
        return ikasleaRepository.bilatuAutocomplete(q.trim(), PageRequest.of(0, 10)).stream()
                .map(i -> Map.<String, Object>of(
                        "id", i.getId(),
                        "izena", i.getIzenOsoa(),
                        "hna", i.getHna() != null ? i.getHna() : "",
                        "taldea", i.getTaldea() != null ? i.getTaldea().getIzena() : ""
                ))
                .toList();
    }

    @GetMapping("/kudeatzaile/ikaslea/{id}")
    public String ikasleFitxa(@PathVariable Long id,
                              @RequestParam(name = "ikasturteaId", required = false) Long ikasturteaId,
                              Model model) {
        Ikaslea ikaslea = ikasleaRepository.findById(id).orElseThrow();
        List<Ikasturtea> ikasturteak = matrikulaRepository.findIkasturteakByIkaslea(id);

        Long hautatutakoIkasturteaId = ikasturteaId;
        if (hautatutakoIkasturteaId == null) {
            hautatutakoIkasturteaId = ikasturteaRepository.findByAktiboaTrue()
                    .map(Ikasturtea::getId)
                    .orElseGet(() -> ikasturteak.isEmpty() ? null : ikasturteak.get(0).getId());
        }

        List<Matrikula> matrikulak = matrikulaRepository.findIkaslearenMatrikulakByIkasturtea(id, hautatutakoIkasturteaId);
        Map<Long, String> uko1fMap = sortuUkoMap(matrikulak, "1_FINAL");
        Map<Long, String> uko2fMap = sortuUkoMap(matrikulak, "2_FINAL");

        model.addAttribute("ikaslea", ikaslea);
        model.addAttribute("ikasturteak", ikasturteak);
        model.addAttribute("hautatutakoIkasturteaId", hautatutakoIkasturteaId);
        model.addAttribute("matrikulak", matrikulak);
        model.addAttribute("matrikulaEgoerak", MatrikulaEgoera.values());
        model.addAttribute("uko1fMap", uko1fMap);
        model.addAttribute("uko2fMap", uko2fMap);

        return "kudeatzaile/ikaslea/fitxa";
    }

    @PostMapping("/kudeatzaile/ikaslea/{id}/taldea")
    public String aldatuIkasleTaldea(@PathVariable Long id,
                                     @RequestParam("taldeaId") Long taldeaId,
                                     @RequestParam(name = "ikasturteaId", required = false) Long ikasturteaId,
                                     RedirectAttributes redirectAttributes) {
        try {
            var emaitza = ikasleaService.aldatuIkaslearenTaldea(id, taldeaId);
            if (emaitza.aldaketaEginDa()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Ikaslea " + emaitza.aurrekoTaldea() + " taldetik " + emaitza.taldeBerria() + " taldera pasatu da.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Ikaslea dagoeneko hautatutako taldean dago.");
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return redirectIkasleFitxara(id, ikasturteaId);
    }

    @PostMapping("/kudeatzaile/ikaslea/{id}/matrikulak/{matrikulaId}/egoera")
    public String aldatuMatrikulaEgoera(@PathVariable Long id,
                                        @PathVariable Long matrikulaId,
                                        @RequestParam("egoera") MatrikulaEgoera egoera,
                                        @RequestParam(name = "ikasturteaId", required = false) Long ikasturteaId,
                                        RedirectAttributes redirectAttributes) {
        Optional<Matrikula> opt = matrikulaRepository.findById(matrikulaId);
        if (opt.isEmpty() || opt.get().getIkaslea() == null || !opt.get().getIkaslea().getId().equals(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Matrikula ez da aurkitu.");
            return redirectIkasleFitxara(id, ikasturteaId);
        }

        Matrikula m = opt.get();
        m.setEgoera(egoera);
        matrikulaRepository.save(m);
        redirectAttributes.addFlashAttribute("successMessage", "Matrikula egoera eguneratu da.");
        return redirectIkasleFitxara(id, ikasturteaId);
    }

    @PostMapping("/kudeatzaile/ikaslea/{id}/matrikulak/{matrikulaId}/ezabatu")
    public String ezabatuMatrikula(@PathVariable Long id,
                                   @PathVariable Long matrikulaId,
                                   @RequestParam(name = "ikasturteaId", required = false) Long ikasturteaId,
                                   RedirectAttributes redirectAttributes) {
        Optional<Matrikula> opt = matrikulaRepository.findById(matrikulaId);
        if (opt.isEmpty() || opt.get().getIkaslea() == null || !opt.get().getIkaslea().getId().equals(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Matrikula ez da aurkitu.");
            return redirectIkasleFitxara(id, ikasturteaId);
        }

        matrikulaRepository.delete(opt.get());
        redirectAttributes.addFlashAttribute("successMessage", "Matrikula ezabatu da (lotutako notak/asistentziak barne).");
        return redirectIkasleFitxara(id, ikasturteaId);
    }

    @PostMapping("/kudeatzaile/ikaslea/{id}/matrikulak/{matrikulaId}/uko-1f")
    @Transactional
    public String markatuUko1f(@PathVariable Long id,
                               @PathVariable Long matrikulaId,
                               @RequestParam(name = "ikasturteaId", required = false) Long ikasturteaId,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        return gordeUko(id, matrikulaId, "1_FINAL", "1. Finalean", ikasturteaId, auth, redirectAttributes);
    }

    @PostMapping("/kudeatzaile/ikaslea/{id}/matrikulak/{matrikulaId}/uko-2f")
    @Transactional
    public String markatuUko2f(@PathVariable Long id,
                               @PathVariable Long matrikulaId,
                               @RequestParam(name = "ikasturteaId", required = false) Long ikasturteaId,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        return gordeUko(id, matrikulaId, "2_FINAL", "2. Finalean", ikasturteaId, auth, redirectAttributes);
    }

    private String gordeUko(Long ikasleaId,
                            Long matrikulaId,
                            String finalKodea,
                            String finalLabela,
                            Long ikasturteaId,
                            Authentication auth,
                            RedirectAttributes redirectAttributes) {
        Optional<Matrikula> opt = matrikulaRepository.findById(matrikulaId);
        if (opt.isEmpty() || opt.get().getIkaslea() == null || !opt.get().getIkaslea().getId().equals(ikasleaId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Matrikula ez da aurkitu.");
            return redirectIkasleFitxara(ikasleaId, ikasturteaId);
        }
        Matrikula matrikula = opt.get();

        if (matrikula.getKoadernoa() == null || matrikula.getKoadernoa().getModuloa() == null
                || matrikula.getKoadernoa().getModuloa().getMaila() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ezin da UKO markatu: koadernoaren datuak osatu gabe daude.");
            return redirectIkasleFitxara(ikasleaId, ikasturteaId);
        }

        EbaluazioMomentua finalMomentua = ebaluazioMomentuaRepository
                .findByMailaAndKodea(matrikula.getKoadernoa().getModuloa().getMaila(), finalKodea)
                .orElse(null);
        if (finalMomentua == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ezin da UKO markatu: " + finalKodea + " momentua ez dago konfiguratuta.");
            return redirectIkasleFitxara(ikasleaId, ikasturteaId);
        }

        EbaluazioEgoera ukoEgoera = ebaluazioEgoeraRepository.findByKodea("UKO_EGINDA")
                .orElseGet(() -> {
                    EbaluazioEgoera berria = new EbaluazioEgoera();
                    berria.setKodea("UKO_EGINDA");
                    berria.setIzena("UKO eginda");
                    berria.setEbaluatua(false);
                    return ebaluazioEgoeraRepository.save(berria);
                });

        EbaluazioNota nota = ebaluazioNotaRepository
                .findByMatrikulaAndEbaluazioMomentua(matrikula, finalMomentua)
                .orElseGet(() -> {
                    EbaluazioNota berria = new EbaluazioNota();
                    berria.setMatrikula(matrikula);
                    berria.setEbaluazioMomentua(finalMomentua);
                    return berria;
                });

        nota.setNota(null);
        nota.setEgoera(ukoEgoera);
        ebaluazioNotaRepository.save(nota);

        Irakaslea eragilea = unekoEragilea(auth);
        String ikasleIzena = matrikula.getIkaslea() != null ? matrikula.getIkaslea().getIzenOsoa() : "ikasle ezezaguna";
        String koadernoIzena = matrikula.getKoadernoa() != null ? matrikula.getKoadernoa().getIzena() : "koaderno ezezaguna";
        String deskribapena = "UKO markatua: " + finalLabela
                + " | ikaslea=" + ikasleIzena
                + " | HNA=" + (matrikula.getIkaslea() != null ? matrikula.getIkaslea().getHna() : "-")
                + " | koadernoa=" + koadernoIzena;
        logService.gorde(LogMota.UKO_EGITEA, eragilea, "Matrikula", matrikula.getId(), deskribapena);

        bidaliUkoMezua(matrikula, finalLabela, auth);
        redirectAttributes.addFlashAttribute("successMessage", "UKO markatu da " + finalLabela);
        return redirectIkasleFitxara(ikasleaId, ikasturteaId);
    }

    private void bidaliUkoMezua(Matrikula matrikula, String finalLabela, Authentication auth) {
        if (matrikula.getKoadernoa() == null || matrikula.getKoadernoa().getIrakasleak() == null
                || matrikula.getKoadernoa().getIrakasleak().isEmpty()) {
            return;
        }
        Irakaslea lehenHartzailea = matrikula.getKoadernoa().getIrakasleak().stream()
                .filter(ir -> ir != null && ir.getId() != null)
                .findFirst()
                .orElse(null);
        Irakaslea bidaltzailea = lortuSistemaEdoUnekoErabiltzailea(auth, lehenHartzailea);
        String ikasleIzena = matrikula.getIkaslea() != null ? matrikula.getIkaslea().getIzenOsoa() : "ikasle batek";
        String edukia = "[SISTEMA] " + matrikula.getKoadernoa().getIzena()
                + " koadernoan " + ikasleIzena + " ikasleak UKO egin du " + finalLabela + ".";

        for (Irakaslea hartzailea : matrikula.getKoadernoa().getIrakasleak()) {
            if (hartzailea == null || hartzailea.getId() == null) continue;
            Mezua mezua = new Mezua();
            mezua.setBidaltzailea(bidaltzailea);
            mezua.setHartzailea(hartzailea);
            mezua.setEdukia(edukia);
            mezua.setBidalketaData(java.time.LocalDateTime.now());
            mezuaRepository.save(mezua);
        }
    }

    private Irakaslea lortuSistemaEdoUnekoErabiltzailea(Authentication auth, Irakaslea fallback) {
        return irakasleaRepository.findByIzenaIgnoreCase("sistema")
                .or(() -> irakasleaRepository.findByEmailaIgnoreCase("sistema@koadernoa.local"))
                .or(() -> {
                    if (auth == null || auth.getName() == null) return Optional.empty();
                    return irakasleaRepository.findByEmailaIgnoreCase(auth.getName())
                            .or(() -> irakasleaRepository.findByIzenaIgnoreCase(auth.getName()));
                })
                .or(() -> Optional.ofNullable(fallback))
                .orElseThrow(() -> new IllegalStateException("Ez da aurkitu mezu-sistemarako bidaltzailerik."));
    }

    private String redirectIkasleFitxara(Long id, Long ikasturteaId) {
        if (ikasturteaId == null) {
            return "redirect:/kudeatzaile/ikaslea/" + id;
        }
        return "redirect:/kudeatzaile/ikaslea/" + id + "?ikasturteaId=" + ikasturteaId;
    }

    private Map<Long, String> sortuUkoMap(List<Matrikula> matrikulak, String finalKodea) {
        if (matrikulak == null || matrikulak.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = matrikulak.stream().map(Matrikula::getId).toList();
        return ebaluazioNotaRepository.findByMatrikulaIdsAndMomentuKodeak(ids, List.of("1_FINAL", "2_FINAL")).stream()
                .filter(n -> n.getEbaluazioMomentua() != null)
                .filter(n -> finalKodea.equalsIgnoreCase(n.getEbaluazioMomentua().getKodea()))
                .filter(n -> n.getEgoera() != null && "UKO_EGINDA".equalsIgnoreCase(n.getEgoera().getKodea()))
                .collect(Collectors.toMap(n -> n.getMatrikula().getId(), n -> n.getEgoera().getKodea(), (a, b) -> a));
    }

    private Irakaslea unekoEragilea(Authentication auth) {
        if (auth == null) {
            return null;
        }
        String erabiltzailea;
        if (auth.getPrincipal() instanceof OAuth2User oAuth2User) {
            erabiltzailea = oAuth2User.getAttribute("email");
        } else {
            erabiltzailea = auth.getName();
        }
        if (erabiltzailea == null || erabiltzailea.isBlank()) {
            return null;
        }
        return irakasleaRepository.findByEmailaIgnoreCase(erabiltzailea)
                .or(() -> irakasleaRepository.findByIzenaIgnoreCase(erabiltzailea))
                .orElse(null);
    }
}
