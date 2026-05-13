package com.koadernoa.app.funtzionalitateak.irakasle;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.function.Function;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioMomentua;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioNota;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioMomentuaRepository;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioNotaRepository;
import com.koadernoa.app.objektuak.ebaluazioa.service.EbaluazioNotaService;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.entitateak.MatrikulaEgoera;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle/notak")
@SessionAttributes("koadernoAktiboa")
@RequiredArgsConstructor
public class NotakController {

    private final MatrikulaRepository matrikulaRepository;
    private final EbaluazioMomentuaRepository ebaluazioMomentuaRepository;
    private final EbaluazioNotaRepository ebaluazioNotaRepository;
    private final EbaluazioNotaService ebaluazioNotaService;

    @GetMapping
    public String notakPantaila(@ModelAttribute("koadernoAktiboa") Koadernoa koadernoa,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (koadernoa == null || koadernoa.getModuloa() == null) {
            redirectAttributes.addFlashAttribute("error",
                    "Ez dago koaderno aktiborik aukeratuta.");
            return "redirect:/irakasle";
        }

        Maila maila = koadernoa.getModuloa().getMaila();

        // 1) Maila honetako ebaluazio momentuak (aktiboak)
        List<EbaluazioMomentua> momentuak =
                ebaluazioMomentuaRepository.findByMailaAndAktiboTrueOrderByOrdenaAsc(maila);

        // 2) Koaderno honetako ikasle matrikulatuak eta pendienteak, bereizita
        List<Matrikula> matrikulak =
                matrikulaRepository.findByKoadernoaIdAndEgoeraFetchIkasleaOrderByIzena(
                        koadernoa.getId(), MatrikulaEgoera.MATRIKULATUA);
        List<Matrikula> pendienteak =
                matrikulaRepository.findByKoadernoaIdAndEgoeraFetchIkasleaOrderByIzena(
                        koadernoa.getId(), MatrikulaEgoera.PENDIENTE_AURREKO_URTETIK);
        List<EbaluazioMomentua> pendienteMomentuak = momentuak.stream()
                .filter(this::daFinalMomentua)
                .toList();

        // 3) Jadanik gordeta dauden notak kargatu
        List<EbaluazioNota> notak = ebaluazioNotaRepository.findByMatrikulaKoadernoa(koadernoa);

        Map<String, EbaluazioNota> notaMap = notak.stream()
                .collect(Collectors.toMap(
                        n -> buildKey(n.getMatrikula().getId(), n.getEbaluazioMomentua().getId()),
                        Function.identity()
                ));
        Map<Long, Boolean> bigarrenFinalaErakutsiMap = matrikulak.stream()
                .collect(Collectors.toMap(Matrikula::getId,
                        m -> {
                            if (m.getNotak() == null || m.getNotak().isEmpty()) {
                                return false;
                            }
                            boolean lehenFinaleanBaliorikBadago = m.getNotak().stream()
                                    .filter(Objects::nonNull)
                                    .filter(n -> n.getEbaluazioMomentua() != null
                                            && "1_FINAL".equalsIgnoreCase(n.getEbaluazioMomentua().getKodea()))
                                    .anyMatch(n -> n.getNota() != null || n.getEgoera() != null);
                            if (!lehenFinaleanBaliorikBadago) {
                                return false;
                            }
                            boolean lehenFinalaGaindituta = m.getNotak().stream()
                                    .filter(Objects::nonNull)
                                    .filter(n -> n.getEbaluazioMomentua() != null
                                            && "1_FINAL".equalsIgnoreCase(n.getEbaluazioMomentua().getKodea()))
                                    .map(EbaluazioNota::getNota)
                                    .filter(Objects::nonNull)
                                    .anyMatch(n -> n >= 5.0);
                            return !lehenFinalaGaindituta;
                        }));

        model.addAttribute("momentuak", momentuak);
        model.addAttribute("matrikulak", matrikulak);
        model.addAttribute("pendienteak", pendienteak);
        model.addAttribute("pendienteMomentuak", pendienteMomentuak);
        model.addAttribute("notaMap", notaMap);
        model.addAttribute("bigarrenFinalaErakutsiMap", bigarrenFinalaErakutsiMap);

        return "irakasleak/notak/index";
    }

    @PostMapping
    public String gordeNotak(@ModelAttribute("koadernoAktiboa") Koadernoa koadernoa,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        if (koadernoa == null || koadernoa.getModuloa() == null) {
            redirectAttributes.addFlashAttribute("error",
                    "Ez dago koaderno aktiborik aukeratuta.");
            return "redirect:/irakasle";
        }

        Maila maila = koadernoa.getModuloa().getMaila();

        List<EbaluazioMomentua> momentuak =
                ebaluazioMomentuaRepository.findByMailaAndAktiboTrueOrderByOrdenaAsc(maila);

        List<Matrikula> matrikulak =
                matrikulaRepository.findByKoadernoaIdAndEgoera(koadernoa.getId(), MatrikulaEgoera.MATRIKULATUA);
        List<Matrikula> pendienteak =
                matrikulaRepository.findByKoadernoaIdAndEgoera(koadernoa.getId(), MatrikulaEgoera.PENDIENTE_AURREKO_URTETIK);
        List<EbaluazioMomentua> pendienteMomentuak = momentuak.stream()
                .filter(this::daFinalMomentua)
                .toList();

        for (Matrikula m : matrikulak) {
            String oh = request.getParameter("oharra_" + m.getId());
            m.setOharra(oh != null ? oh.trim() : null);
        }
        for (Matrikula m : pendienteak) {
            String oh = request.getParameter("oharra_" + m.getId());
            m.setOharra(oh != null ? oh.trim() : null);
        }
        matrikulaRepository.saveAll(matrikulak);
        matrikulaRepository.saveAll(pendienteak);

        String errorHtml = ebaluazioNotaService.gordeNotak(koadernoa, momentuak, matrikulak, request);
        String pendienteErrorHtml = ebaluazioNotaService.gordeNotak(
                koadernoa, pendienteMomentuak, pendienteak, request, false);
        if (pendienteErrorHtml != null && !pendienteErrorHtml.isBlank()) {
            errorHtml = (errorHtml == null || errorHtml.isBlank())
                    ? pendienteErrorHtml
                    : errorHtml + "\n" + pendienteErrorHtml;
        }

        if (errorHtml != null && !errorHtml.isBlank()) {
            redirectAttributes.addFlashAttribute("error", errorHtml);
        } else {
            redirectAttributes.addFlashAttribute("success", "Notak ondo gorde dira.");
        }

        return "redirect:/irakasle/notak";
    }

    private String buildKey(Long matrikulaId, Long momentuaId) {
        return matrikulaId + "_" + momentuaId;
    }

    private boolean daFinalMomentua(EbaluazioMomentua momentua) {
        if (momentua == null || momentua.getKodea() == null) {
            return false;
        }
        return "1_FINAL".equalsIgnoreCase(momentua.getKodea())
                || "2_FINAL".equalsIgnoreCase(momentua.getKodea());
    }
}
