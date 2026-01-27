package com.koadernoa.app.funtzionalitateak.irakasle;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.objektuak.koadernoak.entitateak.EstatistikaEbaluazioan;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EzadostasunFitxa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EzadostasunMota;
import com.koadernoa.app.objektuak.koadernoak.repository.EzadostasunFitxaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.EstatistikaService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle/estatistikak")
@RequiredArgsConstructor
@SessionAttributes("koadernoAktiboa") //<-KOADERNOA SESIOAN GORDE
public class EstatistikakController {
	
	private final EstatistikaService estatistikaService;
    private final EzadostasunFitxaRepository ezadostasunFitxaRepository;

	
	@GetMapping({"/",""})
    public String estatistikakPantaila(
            @SessionAttribute(name = "koadernoAktiboa", required = false) Koadernoa koadernoAktiboa,
            Model model) {

        if (koadernoAktiboa == null || koadernoAktiboa.getId() == null) {
            model.addAttribute("koadernoAktiboDago", false);
            return "irakasleak/estatistikak/index";
        }

        List<EstatistikaEbaluazioan> estatistikak =
                estatistikaService.lortuKoadernoarenEstatistikak(koadernoAktiboa);

        model.addAttribute("koadernoAktiboDago", true);
        model.addAttribute("koadernoAktiboa", koadernoAktiboa);
        model.addAttribute("estatistikak", estatistikak);

        return "irakasleak/estatistikak/index";
    }
	
	@PostMapping("/{estatId}/berkalkulatu")
    @ResponseBody
    public Map<String, Object> berkalkulatuEstatistika(
            @PathVariable Long estatId,
            @SessionAttribute("koadernoAktiboa") Koadernoa koadernoAktiboa) {

        estatistikaService.berkalkulatuEstatistika(koadernoAktiboa, estatId);

        // fetch bidez deituko dugu; JSON txiki bat bueltatu
        Map<String, Object> resp = new HashMap<>();
        resp.put("ok", true);
        return resp;
    }
	
    @PostMapping("/eguneratu")
    @Transactional
    public String eguneratuEstatistikak(
            @SessionAttribute("koadernoAktiboa") Koadernoa koadernoAktiboa,
            HttpServletRequest request,
            RedirectAttributes ra) {

        if (koadernoAktiboa == null || koadernoAktiboa.getId() == null) {
            ra.addFlashAttribute("error", "Ez dago koaderno aktiborik.");
            return "redirect:/irakasle/estatistikak";
        }

        List<EstatistikaEbaluazioan> estatistikak =
                estatistikaService.lortuKoadernoarenEstatistikak(koadernoAktiboa);

        for (EstatistikaEbaluazioan est : estatistikak) {
            Long id = est.getId();
            String udEmandaStr   = request.getParameter("unitateakEmanda_" + id);
            String orduEmandaStr = request.getParameter("orduakEmanda_" + id);

            if (udEmandaStr != null && !udEmandaStr.isBlank()) {
                try {
                    est.setUnitateakEmanda(Integer.parseInt(udEmandaStr));
                } catch (NumberFormatException ignored) {}
            }
            if (orduEmandaStr != null && !orduEmandaStr.isBlank()) {
                try {
                    est.setOrduakEmanda(Integer.parseInt(orduEmandaStr));
                } catch (NumberFormatException ignored) {}
            }
            est.setAzkenKalkulua(LocalDateTime.now());
        }

        // batch gordeta
        // (estatRepo.saveAll(estatistikak) edo @Transactional + entity-managed nahikoa)
        ra.addFlashAttribute("success", "Estatistikak eguneratu dira.");
        return "redirect:/irakasle/estatistikak";
    }

    @GetMapping("/{estatId}/ezadostasuna")
    @Transactional
    public String ezadostasunFitxa(
            @PathVariable Long estatId,
            @SessionAttribute("koadernoAktiboa") Koadernoa koadernoAktiboa,
            @RequestParam(required = false) EzadostasunMota mota,
            Model model,
            RedirectAttributes ra) {

        EstatistikaEbaluazioan estatistika =
                estatistikaService.lortuKoadernoarenEstatistika(koadernoAktiboa, estatId);
        if (estatistika == null) {
            ra.addFlashAttribute("error", "Estatistika ez da aurkitu.");
            return "redirect:/irakasle/estatistikak";
        }

        List<EzadostasunMota> motak = kalkulatuEzadostasunMotak(estatistika);
        if (mota == null) {
            if (motak.isEmpty()) {
                ra.addFlashAttribute("error", "Ez dago ezadostasun aktiborik.");
                return "redirect:/irakasle/estatistikak";
            }
            mota = motak.get(0);
        }
        if (!motak.contains(mota)) {
            ra.addFlashAttribute("error", "Ezadostasun mota hori ez dago aktibo.");
            return "redirect:/irakasle/estatistikak";
        }

        EzadostasunFitxa fitxa = ezadostasunFitxaRepository
                .findByEstatistikaIdAndMota(estatId, mota)
                .orElse(null);
        if (fitxa == null) {
            EzadostasunFitxa zaharra = ezadostasunFitxaRepository
                    .findFirstByEstatistikaIdOrderById(estatId)
                    .orElse(null);
            if (zaharra != null && zaharra.getMota() == null) {
                zaharra.setMota(mota);
                fitxa = ezadostasunFitxaRepository.save(zaharra);
            }
        }
        if (fitxa == null) {
            fitxa = sortuFitxaAutomatikoa(estatistika, mota);
            ezadostasunFitxaRepository.save(fitxa);
        }

        model.addAttribute("koadernoAktiboa", koadernoAktiboa);
        model.addAttribute("estatistika", estatistika);
        model.addAttribute("fitxa", fitxa);
        model.addAttribute("ezadostasunMota", mota);
        model.addAttribute("ezadostasunMotaLabel", kalkulatuEzadostasunLabel(estatistika, mota));
        return "irakasleak/estatistikak/ezadostasun-fitxa";
    }

    @PostMapping("/{estatId}/ezadostasuna")
    @Transactional
    public String gordeEzadostasunFitxa(
            @PathVariable Long estatId,
            @SessionAttribute("koadernoAktiboa") Koadernoa koadernoAktiboa,
            @RequestParam EzadostasunMota mota,
            @RequestParam(required = false) String zuzentzeJarduerak,
            @RequestParam(required = false) String zuzentzeJarduerakArduraduna,
            @RequestParam(required = false) String jarraipenData,
            @RequestParam(required = false) String jarraipenArduradunak,
            @RequestParam(required = false) String hartutakoErabakiak,
            @RequestParam(required = false) String itxieraData,
            @RequestParam(required = false) String itxieraArduraduna,
            @RequestParam(required = false) String ezadostasunaZuzenduta,
            RedirectAttributes ra,
            @RequestParam(required = false) String postAction) {

        EstatistikaEbaluazioan estatistika =
                estatistikaService.lortuKoadernoarenEstatistika(koadernoAktiboa, estatId);
        if (estatistika == null) {
            ra.addFlashAttribute("error", "Estatistika ez da aurkitu.");
            return "redirect:/irakasle/estatistikak";
        }

        EzadostasunFitxa fitxa = ezadostasunFitxaRepository
                .findByEstatistikaIdAndMota(estatId, mota)
                .orElse(null);
        if (fitxa == null) {
            EzadostasunFitxa zaharra = ezadostasunFitxaRepository
                    .findFirstByEstatistikaIdOrderById(estatId)
                    .orElse(null);
            if (zaharra != null && zaharra.getMota() == null) {
                zaharra.setMota(mota);
                fitxa = zaharra;
            }
        }
        if (fitxa == null) {
            fitxa = sortuFitxaAutomatikoa(estatistika, mota);
        }

        fitxa.setZuzentzeJarduerak(zuzentzeJarduerak);
        fitxa.setZuzentzeJarduerakArduraduna(zuzentzeJarduerakArduraduna);
        fitxa.setJarraipenData(parseDate(jarraipenData));
        fitxa.setJarraipenArduradunak(jarraipenArduradunak);
        fitxa.setHartutakoErabakiak(hartutakoErabakiak);
        fitxa.setItxieraData(parseDate(itxieraData));
        fitxa.setItxieraArduraduna(itxieraArduraduna);
        if (StringUtils.hasText(ezadostasunaZuzenduta)) {
            fitxa.setEzadostasunaZuzenduta(Boolean.parseBoolean(ezadostasunaZuzenduta));
        } else {
            fitxa.setEzadostasunaZuzenduta(null);
        }

        ezadostasunFitxaRepository.save(fitxa);
        ra.addFlashAttribute("success", "Ezadostasun fitxa gorde da.");
        //"X" sakatu bada, gorde eta itxi
        if ("close".equals(postAction)) {
            return "redirect:/irakasle/estatistikak";
        }
        return "redirect:/irakasle/estatistikak/" + estatId + "/ezadostasuna?mota=" + mota;
    }

    private EzadostasunFitxa sortuFitxaAutomatikoa(EstatistikaEbaluazioan estatistika, EzadostasunMota mota) {
        EzadostasunFitxa fitxa = new EzadostasunFitxa();
        fitxa.setEstatistika(estatistika);
        fitxa.setMota(mota);
        estatistika.getEzadostasunFitxak().add(fitxa);
        fitxa.setEmandakoBlokeKopurua(estatistika.getUnitateakEmanda());
        fitxa.setEmandakoOrduKopurua(estatistika.getOrduakEmanda());
        fitxa.setIkasleenBertaratzePortzentaia(estatistika.getBertaratzePortzentaia());
        fitxa.setGaindituPortzentaia(estatistika.getGaindituPortzentaia());
        return fitxa;
    }

    private List<EzadostasunMota> kalkulatuEzadostasunMotak(EstatistikaEbaluazioan estatistika) {
        List<EzadostasunMota> emaitza = new ArrayList<>();
        if (estatistika == null || estatistika.getEbaluazioMomentua() == null ||
                estatistika.getEbaluazioMomentua().getEzadostasunKonfig() == null) {
            return emaitza;
        }

        com.koadernoa.app.objektuak.ebaluazioa.entitateak.EzadostasunKonfig konfig =
                estatistika.getEbaluazioMomentua().getEzadostasunKonfig();
        if (estatistika.getUdPortzentaia() != null &&
                estatistika.getUdPortzentaia() < konfig.getMinBlokePortzentaia()) {
            emaitza.add(EzadostasunMota.UD_EMANDA);
        }
        if (estatistika.getOrduPortzentaia() != null &&
                estatistika.getOrduPortzentaia() < konfig.getMinOrduPortzentaia()) {
            emaitza.add(EzadostasunMota.ORDU_EMANDA);
        }
        if (estatistika.getGaindituPortzentaia() != null &&
                estatistika.getGaindituPortzentaia() < konfig.getMinGaindituPortzentaia()) {
            emaitza.add(EzadostasunMota.GAINDITU);
        }
        if (estatistika.getBertaratzePortzentaia() != null &&
                estatistika.getBertaratzePortzentaia() < konfig.getMinBertaratzePortzentaia()) {
            emaitza.add(EzadostasunMota.BERTARATZE);
        }
        return emaitza;
    }

    private String kalkulatuEzadostasunLabel(EstatistikaEbaluazioan estatistika, EzadostasunMota mota) {
        if (estatistika == null || estatistika.getEbaluazioMomentua() == null ||
                estatistika.getEbaluazioMomentua().getEzadostasunKonfig() == null || mota == null) {
            return "—";
        }
        com.koadernoa.app.objektuak.ebaluazioa.entitateak.EzadostasunKonfig konfig =
                estatistika.getEbaluazioMomentua().getEzadostasunKonfig();
        return switch (mota) {
            case UD_EMANDA -> "UD-ak emanda < %" + konfig.getMinBlokePortzentaia();
            case ORDU_EMANDA -> "Orduak emanda < %" + konfig.getMinOrduPortzentaia();
            case GAINDITU -> "Gainditu duten ikasleak < %" + konfig.getMinGaindituPortzentaia();
            case BERTARATZE -> "Ikasleen bertaratzea < %" + konfig.getMinBertaratzePortzentaia();
        };
    }

    private java.time.LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return java.time.LocalDate.parse(value);
    }
}
