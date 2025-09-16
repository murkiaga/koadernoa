package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.koadernoa.app.egutegia.entitateak.Maila;
import com.koadernoa.app.egutegia.repository.MailaRepository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/kudeatzaile/konfigurazioa")
@RequiredArgsConstructor
public class KonfigurazioaController {

    private final MailaRepository mailaRepository;

    // ---- GET: orria ----
    @GetMapping
    public String index(Model model) {
        // Ordenatuta lortu nahi badituzu
        List<Maila> mailak = mailaRepository.findAll(
            Sort.by(Sort.Order.asc("ordena"), Sort.Order.asc("izena"))
        );

        ActiveMailakForm form = new ActiveMailakForm();
        form.setAktiboIds(mailak.stream().filter(Maila::getAktibo).map(Maila::getId).toList());

        model.addAttribute("mailak", mailak);
        model.addAttribute("activeForm", form);
        model.addAttribute("sortuForm", new SortuMailaForm());
        return "kudeatzaile/konfigurazioa/index";
    }

    // ---- POST: aktibo/desaktibo bulk ----
    @PostMapping("/mailak/aktiboak")
    public String gordeAktiboak(@ModelAttribute("activeForm") ActiveMailakForm form) {
        List<Long> aktiboIds = form.getAktiboIds() != null ? form.getAktiboIds() : List.of();
        List<Maila> denak = mailaRepository.findAll();
        for (Maila m : denak) {
            boolean aktibo = aktiboIds.contains(m.getId());
            if (Boolean.TRUE.equals(m.getAktibo()) != aktibo) {
                m.setAktibo(aktibo);
            }
        }
        mailaRepository.saveAll(denak);
        return "redirect:/kudeatzaile/konfigurazioa";
    }

    // ---- POST: maila berria sortu ----
    @PostMapping("/mailak/sortu")
    public String sortuMaila(@ModelAttribute("sortuForm") SortuMailaForm form, BindingResult br, Model model) {
        if (form.getKodea() == null || form.getKodea().isBlank()) {
            br.rejectValue("kodea", "kodea.blank", "Kodea beharrezkoa da");
        }
        if (br.hasErrors()) {
            model.addAttribute("mailak", mailaRepository.findAll(
                Sort.by(Sort.Order.asc("ordena"), Sort.Order.asc("izena"))
            ));
            model.addAttribute("activeForm", new ActiveMailakForm());
            return "kudeatzaile/konfigurazioa/index";
        }

        mailaRepository.findByKodea(form.getKodea().trim()).ifPresent(m ->
            { throw new IllegalArgumentException("Kode hori dagoeneko existitzen da: " + form.getKodea()); });

        Maila m = new Maila();
        m.setKodea(form.getKodea().trim());
        m.setIzena(form.getIzena() == null ? form.getKodea().trim() : form.getIzena().trim());
        m.setOrdena(form.getOrdena() != null ? form.getOrdena() : 999);
        m.setAktibo(true);
        mailaRepository.save(m);

        return "redirect:/kudeatzaile/konfigurazioa";
    }

    // ===== DTO txikiak =====
    @Data
    public static class ActiveMailakForm {
        private List<Long> aktiboIds;
    }

    @Data
    public static class SortuMailaForm {
        @NotBlank
        private String kodea;
        private String izena;
        @NotNull
        private Integer ordena;
    }
}
