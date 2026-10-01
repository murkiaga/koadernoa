package com.koadernoa.app.ethazi.controller;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.ethazi.dto.IkaskuntzaEmaitzaImportRow;
import com.koadernoa.app.ethazi.dto.IkaskuntzaEmaitzaImportRow.Egoera;
import com.koadernoa.app.ethazi.service.IkaskuntzaEmaitzaImportService;
import com.koadernoa.app.ethazi.service.PdfIkaskuntzaEmaitzaParserService;
import com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/ethazi/ikaskuntza-emaitzak/importatu")
@RequiredArgsConstructor
public class IkaskuntzaEmaitzaPdfImportController {
    private static final String SESSION_KEY = IkaskuntzaEmaitzaPdfImportController.class.getName() + ".preview";

    private final PdfIkaskuntzaEmaitzaParserService parser;
    private final IkaskuntzaEmaitzaImportService importService;

    private record Aurrebista(String token, Hizkuntza hizkuntza,
            List<IkaskuntzaEmaitzaImportRow> lerroak, List<String> abisuak) implements Serializable {}

    @GetMapping
    public String formularioa(Model model) {
        model.addAttribute("hizkuntzak", List.of(Hizkuntza.EUSKARA, Hizkuntza.GAZTELERA));
        return "Ethazi/ikaskuntza-emaitzak/importatu";
    }

    @PostMapping("/aurrebista")
    public String aurrebista(@RequestParam Hizkuntza hizkuntza,
            @RequestParam("fitxategia") MultipartFile fitxategia, Model model, HttpSession session) {
        try {
            var parsed = parser.parse(fitxategia, hizkuntza);
            var rows = importService.aurrebista(parsed.lerroak());
            var preview = new Aurrebista(UUID.randomUUID().toString(), hizkuntza, rows, parsed.abisuak());
            session.setAttribute(SESSION_KEY, preview);
            addPreview(model, preview);
            return "Ethazi/ikaskuntza-emaitzak/aurrebista";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("aukeratutakoHizkuntza", hizkuntza);
            return formularioa(model);
        }
    }

    @PostMapping("/baieztatu")
    public String baieztatu(@RequestParam String token,
            @RequestParam(name = "aukeratuak", required = false) List<Integer> aukeratuak,
            @RequestParam(name = "deskribapenak", required = false) List<String> deskribapenak,
            HttpSession session, RedirectAttributes flash) {
        Aurrebista preview = preview(session, token);
        try {
            var result = importService.inportatu(aukeratutakoLerroak(preview, aukeratuak, deskribapenak));
            session.removeAttribute(SESSION_KEY);
            flash.addFlashAttribute("success", "PDF inportazioa osatu da: " + result.sortuak()
                    + " sortu, " + result.eguneratuak() + " eguneratu, " + result.aldaketarikGabe()
                    + " aldaketarik gabe eta " + result.arazoak() + " arazo.");
            return "redirect:/ethazi/ikaskuntza-emaitzak/importatu";
        } catch (DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", "Ezin izan da inportazioa osatu: datuak beste prozesu batek aldatu ditu edo erregistro bikoiztua dago.");
            return "redirect:/ethazi/ikaskuntza-emaitzak/importatu";
        }
    }

    @PostMapping("/csv")
    public ResponseEntity<byte[]> csv(@RequestParam String token,
            @RequestParam(name = "deskribapenak", required = false) List<String> deskribapenak,
            HttpSession session) {
        Aurrebista preview = preview(session, token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ikaskuntza-emaitzak-aurrebista.csv")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(importService.csv(editatutakoLerroak(preview, deskribapenak)));
    }

    private Aurrebista preview(HttpSession session, String token) {
        Object value = session.getAttribute(SESSION_KEY);
        if (!(value instanceof Aurrebista preview) || !preview.token().equals(token))
            throw new IllegalArgumentException("Aurrebista iraungi da. Igo PDFa berriro.");
        return preview;
    }

    private void addPreview(Model model, Aurrebista preview) {
        model.addAttribute("token", preview.token());
        model.addAttribute("hizkuntza", preview.hizkuntza());
        model.addAttribute("lerroak", preview.lerroak());
        model.addAttribute("abisuak", preview.abisuak());
        model.addAttribute("sortuak", count(preview, Egoera.SORTU));
        model.addAttribute("eguneratuak", count(preview, Egoera.EGUNERATU));
        model.addAttribute("aldaketarikGabe", count(preview, Egoera.ALDAKETARIK_EZ));
        model.addAttribute("arazoak", count(preview, Egoera.ARAZOA));
    }

    private long count(Aurrebista preview, Egoera status) {
        return preview.lerroak().stream().filter(row -> row.egoera() == status).count();
    }

    private List<IkaskuntzaEmaitzaImportRow> aukeratutakoLerroak(Aurrebista preview, List<Integer> selected,
            List<String> descriptions) {
        if (selected == null) return List.of();
        List<IkaskuntzaEmaitzaImportRow> edited = editatutakoLerroak(preview, descriptions);
        var unique = new java.util.LinkedHashSet<Integer>();
        for (Integer index : selected) {
            if (index == null || index < 0 || index >= preview.lerroak().size() || !unique.add(index))
                throw new IllegalArgumentException("Aurrebistako aukeraketa ez da baliozkoa. Igo PDFa berriro.");
        }
        return unique.stream().map(edited::get).toList();
    }

    private List<IkaskuntzaEmaitzaImportRow> editatutakoLerroak(Aurrebista preview, List<String> descriptions) {
        if (descriptions == null || descriptions.size() != preview.lerroak().size())
            throw new IllegalArgumentException("Aurrebistako deskribapenak ez dira baliozkoak. Igo PDFa berriro.");
        var edited = new java.util.ArrayList<IkaskuntzaEmaitzaImportRow>(preview.lerroak().size());
        for (int i = 0; i < preview.lerroak().size(); i++) {
            var row = preview.lerroak().get(i);
            edited.add(new IkaskuntzaEmaitzaImportRow(row.eeiKodea(), row.ordena(), row.kodea(),
                    descriptions.get(i).strip(), row.hizkuntza(), row.egoera(), row.mezua(), row.dbBalioa()));
        }
        return List.copyOf(edited);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String aurrebistaIraungita(IllegalArgumentException ex, RedirectAttributes flash) {
        flash.addFlashAttribute("error", ex.getMessage());
        return "redirect:/ethazi/ikaskuntza-emaitzak/importatu";
    }
}
