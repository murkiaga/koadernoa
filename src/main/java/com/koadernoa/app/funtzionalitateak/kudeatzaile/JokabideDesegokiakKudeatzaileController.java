package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.jokabidea.entitateak.JokabideDesegokia;
import com.koadernoa.app.objektuak.jokabidea.repository.JokabideDesegokiaRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/kudeatzaile/jokabide-desegokiak")
@RequiredArgsConstructor
public class JokabideDesegokiakKudeatzaileController {

    private final JokabideDesegokiaRepository repository;
    private final IrakasleaService irakasleaService;

    @GetMapping
    @Transactional(readOnly = true)
    public String index(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataHasiera,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAmaiera,
            @RequestParam(required = false) String ikaslea,
            @RequestParam(required = false) Long moduloaId,
            @RequestParam(required = false) Long taldeaId,
            @RequestParam(required = false) Boolean jasota,
            Model model) {
        String ikasleFiltroa = ikaslea == null ? null : ikaslea.trim();
        model.addAttribute("jokabideak", repository.bilatuKudeatzailearentzat(
                dataHasiera, dataAmaiera, ikasleFiltroa, moduloaId, taldeaId, jasota));
        model.addAttribute("moduloak", repository.findDistinctModuloakOrderByIzena());
        model.addAttribute("taldeak", repository.findDistinctTaldeakOrderByIzena());
        model.addAttribute("dataHasiera", dataHasiera);
        model.addAttribute("dataAmaiera", dataAmaiera);
        model.addAttribute("ikaslea", ikasleFiltroa);
        model.addAttribute("moduloaId", moduloaId);
        model.addAttribute("taldeaId", taldeaId);
        model.addAttribute("jasota", jasota);
        return "kudeatzaile/jokabide-desegokiak/index";
    }

    @PostMapping("/{id}/jasota")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> aldatuJasota(
            @PathVariable Long id,
            @RequestParam boolean jasota,
            Authentication auth) {
        JokabideDesegokia jokabidea = repository.findById(id).orElse(null);
        if (jokabidea == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("ok", false, "errorea", "Jokabide desegokia ez da aurkitu."));
        }
        markatuJasota(jokabidea, jasota, auth);
        repository.save(jokabidea);
        return ResponseEntity.ok(Map.of("ok", true, "jasota", jokabidea.isJasota()));
    }

    @GetMapping("/{id}/pdf")
    @Transactional
    public ResponseEntity<?> deskargatuPdfa(@PathVariable Long id, Authentication auth) throws IOException {
        JokabideDesegokia jokabidea = repository.findById(id).orElse(null);
        if (jokabidea == null) {
            return ResponseEntity.notFound().build();
        }
        Path pdfa = Paths.get(jokabidea.getPdfPath()).normalize();
        if (!Files.isRegularFile(pdfa)) {
            return ResponseEntity.notFound().build();
        }
        markatuJasota(jokabidea, true, auth);
        repository.saveAndFlush(jokabidea);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + jokabidea.getPdfFilename() + "\"")
                .body(Files.readAllBytes(pdfa));
    }

    private void markatuJasota(JokabideDesegokia jokabidea, boolean jasota, Authentication auth) {
        jokabidea.setJasota(jasota);
        if (jasota) {
            Irakaslea kudeatzailea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
            jokabidea.setJasotaAt(LocalDateTime.now());
            jokabidea.setJasotaNork(kudeatzailea);
        } else {
            jokabidea.setJasotaAt(null);
            jokabidea.setJasotaNork(null);
        }
    }
}
