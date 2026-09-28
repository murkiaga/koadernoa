package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.mezuak.service.MezuaService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/kudeatzaile/mezuak")
@RequiredArgsConstructor
public class KudeatzaileMezuakController {

    private static final List<DateTimeFormatter> DATA_FORMATUAK = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M/d/yy"),
            DateTimeFormatter.ofPattern("M/d/yyyy"));

    private final MezuaService mezuaService;
    private final IrakasleaService irakasleaService;

    @InitBinder
    void konfiguratuDatak(WebDataBinder binder) {
        binder.registerCustomEditor(LocalDate.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.isBlank()) {
                    setValue(null);
                    return;
                }
                for (DateTimeFormatter formatua : DATA_FORMATUAK) {
                    try {
                        setValue(LocalDate.parse(text, formatua));
                        return;
                    } catch (DateTimeParseException ignored) {
                        // Hurrengo formatuarekin saiatu.
                    }
                }
                throw new IllegalArgumentException("Data-formatu baliogabea: " + text);
            }

            @Override
            public String getAsText() {
                LocalDate data = (LocalDate) getValue();
                return data == null ? "" : data.toString();
            }
        });
    }

    @GetMapping
    public String index(
            @RequestParam(name = "dataHasiera", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataHasiera,
            @RequestParam(name = "dataAmaiera", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAmaiera,
            Authentication auth, Model model) {
        Irakaslea ir = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        if (dataHasiera != null && dataAmaiera != null && dataHasiera.isAfter(dataAmaiera)) {
            model.addAttribute("error", "Hasierako data ezin da amaierako data baino geroagokoa izan.");
            model.addAttribute("mezuak", List.of());
        } else {
            model.addAttribute("mezuak", mezuaService.bidaliEtaJasotakoak(ir.getId(), dataHasiera, dataAmaiera));
        }
        model.addAttribute("dataHasiera", dataHasiera);
        model.addAttribute("dataAmaiera", dataAmaiera);
        return "kudeatzaile/mezuak/index";
    }

    @PostMapping("/aukeratuak-ezabatu")
    public String ezabatuHautatuak(
            @RequestParam(name = "mezuIds", required = false) List<Long> mezuIds,
            @RequestParam(name = "dataHasiera", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataHasiera,
            @RequestParam(name = "dataAmaiera", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAmaiera,
            Authentication auth, RedirectAttributes ra) {
        if (dataHasiera != null) ra.addAttribute("dataHasiera", dataHasiera.toString());
        if (dataAmaiera != null) ra.addAttribute("dataAmaiera", dataAmaiera.toString());
        if (mezuIds == null || mezuIds.isEmpty()) {
            ra.addFlashAttribute("error", "Aukeratu gutxienez mezu bat ezabatzeko.");
            return "redirect:/kudeatzaile/mezuak";
        }
        Irakaslea ir = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        int ezabatuak = mezuaService.ezabatuHautatuak(ir.getId(), mezuIds);
        ra.addFlashAttribute("success", ezabatuak + " mezu ezabatu dira.");
        return "redirect:/kudeatzaile/mezuak";
    }
}
