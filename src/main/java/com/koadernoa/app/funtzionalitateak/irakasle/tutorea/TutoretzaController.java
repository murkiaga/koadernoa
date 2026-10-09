package com.koadernoa.app.funtzionalitateak.irakasle.tutorea;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.koadernoa.app.funtzionalitateak.irakasle.baimenak.ikasleakkontsultatu.IkasleakKontsultatuService;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.jokabidea.entitateak.JokabideDesegokia;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/irakasle/tutore")
public class TutoretzaController {

    private static final String TEMPLATE_ROOT = "irakasleak/tutorea/";

    private final TutoretzaService tutoretzaService;
    private final IkasleakKontsultatuService ikasleakKontsultatuService;

    @GetMapping("/{taldeaId}")
    public String taldea(@PathVariable Long taldeaId, Authentication authentication, Model model) {
        Taldea taldea = tutoretzaService.baimenduTaldea(taldeaId, authentication);
        Ikasturtea ikasturtea = tutoretzaService.getIkasturteAktiboa();
        model.addAttribute("taldea", taldea);
        model.addAttribute("ikasturtea", ikasturtea);
        model.addAttribute("ikasleak", tutoretzaService.getTaldekoIkasleAktiboak(taldeaId));
        return TEMPLATE_ROOT + "taldea";
    }

    @GetMapping("/{taldeaId}/{ikasleaId}")
    public String ikaslea(@PathVariable Long taldeaId,
                          @PathVariable Long ikasleaId,
                          Authentication authentication,
                          Model model) {
        Taldea taldea = tutoretzaService.baimenduTaldea(taldeaId, authentication);
        Ikaslea ikaslea = tutoretzaService.baimenduIkaslea(taldeaId, ikasleaId);
        Ikasturtea ikasturtea = tutoretzaService.getIkasturteAktiboa();
        var ebaluazioDatuak = ikasleakKontsultatuService
                .getEbaluazioDatuak(ikasleaId, ikasturtea.getId());
        model.addAttribute("taldea", taldea);
        model.addAttribute("ikaslea", ikaslea);
        model.addAttribute("ikasturtea", ikasturtea);
        model.addAttribute("ebaluazioMomentuak", ebaluazioDatuak.momentuak());
        model.addAttribute("matrikulak", ebaluazioDatuak.matrikulak());
        return TEMPLATE_ROOT + "ikaslea";
    }

    @GetMapping("/{taldeaId}/{ikasleaId}/asistentzia-kontrola")
    public String asistentziaKontrola(@PathVariable Long taldeaId,
                                      @PathVariable Long ikasleaId,
                                      @RequestParam(name = "urtea", required = false) Integer urtea,
                                      @RequestParam(name = "hilabetea", required = false) Integer hilabetea,
                                      Authentication authentication,
                                      Model model) {
        Taldea taldea = tutoretzaService.baimenduTaldea(taldeaId, authentication);
        tutoretzaService.baimenduIkaslea(taldeaId, ikasleaId);
        LocalDate gaur = LocalDate.now();
        int hautatutakoUrtea = urtea != null ? urtea : gaur.getYear();
        int hautatutakoHilabetea = hilabetea != null && hilabetea >= 1 && hilabetea <= 12
                ? hilabetea : gaur.getMonthValue();
        var datuak = ikasleakKontsultatuService
                .getAsistentziaDatuak(ikasleaId, hautatutakoUrtea, hautatutakoHilabetea);
        model.addAttribute("taldea", taldea);
        model.addAttribute("ikaslea", datuak.ikaslea());
        model.addAttribute("ikasturtea", datuak.ikasturtea());
        model.addAttribute("urtea", datuak.urtea());
        model.addAttribute("hilabetea", datuak.hilabetea());
        model.addAttribute("hilabeteUrtea", datuak.hilabeteUrtea());
        model.addAttribute("egunak", datuak.egunak());
        model.addAttribute("lerroak", datuak.lerroak());
        return TEMPLATE_ROOT + "asistentzia-kontrola";
    }

    @GetMapping("/{taldeaId}/{ikasleaId}/asistentzia-kontrola/jokabideak/{jokabideaId}/pdf")
    public ResponseEntity<?> jokabidePdfa(@PathVariable Long taldeaId,
                                          @PathVariable Long ikasleaId,
                                          @PathVariable Long jokabideaId,
                                          Authentication authentication) throws IOException {
        tutoretzaService.baimenduTaldea(taldeaId, authentication);
        tutoretzaService.baimenduIkaslea(taldeaId, ikasleaId);
        JokabideDesegokia jokabidea;
        try {
            jokabidea = ikasleakKontsultatuService.getJokabideDesegokia(ikasleaId, jokabideaId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
        if (jokabidea.getPdfPath() == null || jokabidea.getPdfPath().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        Path pdfa = Paths.get(jokabidea.getPdfPath()).normalize();
        if (!Files.isRegularFile(pdfa)) {
            return ResponseEntity.notFound().build();
        }
        String fitxategiIzena = jokabidea.getPdfFilename() != null
                ? jokabidea.getPdfFilename().replace("\"", "")
                : "jokabide-desegokia.pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fitxategiIzena + "\"")
                .body(Files.readAllBytes(pdfa));
    }
}
