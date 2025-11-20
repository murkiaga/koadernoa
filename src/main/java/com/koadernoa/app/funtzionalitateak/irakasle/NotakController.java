package com.koadernoa.app.funtzionalitateak.irakasle;

import java.util.List;
import java.util.Map;
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

        // 2) Koaderno honetako ikasle matrikulatuak
        List<Matrikula> matrikulak =
                matrikulaRepository.findByKoadernoaIdAndEgoera(koadernoa.getId(), MatrikulaEgoera.MATRIKULATUA);

        // 3) Jadanik gordeta dauden notak kargatu
        List<EbaluazioNota> notak = ebaluazioNotaRepository.findByMatrikulaKoadernoa(koadernoa);

        Map<String, EbaluazioNota> notaMap = notak.stream()
                .collect(Collectors.toMap(
                        n -> buildKey(n.getMatrikula().getId(), n.getEbaluazioMomentua().getId()),
                        Function.identity()
                ));

        model.addAttribute("momentuak", momentuak);
        model.addAttribute("matrikulak", matrikulak);
        model.addAttribute("notaMap", notaMap);

        return "irakasleak/notak/index";
    }

    @PostMapping
    @Transactional
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

        // Konbinazio guztien gainetik pasatu (ikasle x ebaluazio momentua)
        for (Matrikula matrikula : matrikulak) {
            for (EbaluazioMomentua momentua : momentuak) {
                String paramName = "nota_" + matrikula.getId() + "_" + momentua.getId();
                String value = request.getParameter(paramName);

                if (value == null) continue; // ez dago inputik

                value = value.trim();
                // Komak puntuz ordezkatu (eus-es daukazun formatuarengatik)
                value = value.replace(',', '.');

                // Bilatu aurreko nota bat badagoen
                var existingOpt = ebaluazioNotaRepository
                        .findByMatrikulaAndEbaluazioMomentua(matrikula, momentua);

                if (value.isEmpty()) {
                    // Hutsa → nota ezabatu
                    existingOpt.ifPresent(ebaluazioNotaRepository::delete);
                } else {
                    Double notaZenbaki = null;
                    try {
                        notaZenbaki = Double.valueOf(value);
                    } catch (NumberFormatException ex) {
                        // Nota okerra: momentuz saltatu, nahi baduzu log bat idatzi
                        continue;
                    }

                    EbaluazioNota nota = existingOpt.orElseGet(() -> {
                        EbaluazioNota berria = new EbaluazioNota();
                        berria.setMatrikula(matrikula);
                        berria.setEbaluazioMomentua(momentua);
                        return berria;
                    });

                    nota.setNota(notaZenbaki);
                    ebaluazioNotaRepository.save(nota);
                }
            }
        }

        redirectAttributes.addFlashAttribute("success", "Notak ondo gorde dira.");
        return "redirect:/irakasle/notak";
    }

    private String buildKey(Long matrikulaId, Long momentuaId) {
        return matrikulaId + "_" + momentuaId;
    }
}
