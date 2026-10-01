package com.koadernoa.app.funtzionalitateak.irakasle.baimenak.ikasleakkontsultatu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.jokabidea.entitateak.JokabideDesegokia;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;
import com.koadernoa.app.objektuak.zikloak.repository.ZikloaRepository;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpSession;

@Controller
@RequiredArgsConstructor
@RequestMapping("/irakasle/ikasleak-kontsultatu")
public class IkasleakKontsultatuController {

    private static final String TEMPLATE_ROOT = "irakasleak/baimenak/ikasleakKontsultatu/";
    private static final String PAGE_SIZE_SESSION_KEY = "ikasleakKontsultatuPageSize";

    private final IkasleakKontsultatuService service;
    private final ZikloaRepository zikloaRepository;
    private final TaldeaRepository taldeaRepository;

    @GetMapping
    public String ikasleak(@RequestParam(name = "zikloaId", required = false) Long zikloaId,
                           @RequestParam(name = "taldeaId", required = false) Long taldeaId,
                           @RequestParam(name = "page", defaultValue = "0") int page,
                           @RequestParam(name = "size", required = false) Integer size,
                           HttpSession session,
                           Model model) {
        int pageSize = aukeratuPageSize(size, session);
        Page<Ikaslea> emaitza = service.bilatu(zikloaId, taldeaId, page, pageSize);
        model.addAttribute("ikasleak", emaitza.getContent());
        model.addAttribute("zikloak", zikloaRepository.findAllByOrderByIzenaAsc());
        model.addAttribute("taldeak", zikloaId != null
                ? taldeaRepository.findByZikloa_IdOrderByIzenaAsc(zikloaId)
                : taldeaRepository.findAllByOrderByIzenaAsc());
        model.addAttribute("zikloaId", zikloaId);
        model.addAttribute("taldeaId", taldeaId);
        model.addAttribute("currentPage", emaitza.getNumber());
        model.addAttribute("totalPages", emaitza.getTotalPages());
        model.addAttribute("pageSize", emaitza.getSize());
        model.addAttribute("totalItems", emaitza.getTotalElements());
        model.addAttribute("pageSizes", List.of(20, 40, 60, 80, 100));
        return TEMPLATE_ROOT + "ikasleak";
    }

    @GetMapping("/bilatu")
    @ResponseBody
    public List<Map<String, Object>> bilatuIkasleak(@RequestParam(name = "q", required = false) String q) {
        if (q == null || q.trim().length() < 3) {
            return List.of();
        }
        return service.bilatuAutocomplete(q.trim()).stream()
                .map(i -> Map.<String, Object>of(
                        "id", i.getId(),
                        "izena", i.getIzenOsoa(),
                        "hna", i.getHna() != null ? i.getHna() : "",
                        "taldea", i.getTaldea() != null ? i.getTaldea().getIzena() : ""
                ))
                .toList();
    }

    @GetMapping("/taldeak/{id}/argazkiak")
    public String taldeArgazkiak(@PathVariable Long id, Model model) {
        model.addAttribute("taldea", service.getTaldea(id));
        model.addAttribute("ikasleak", service.getTaldekoIkasleak(id));
        model.addAttribute("taldeak", service.getIkasleakDituztenTaldeak());
        return TEMPLATE_ROOT + "talde-argazkiak";
    }

    @GetMapping("/{id}")
    public String ikaslea(@PathVariable Long id,
                          @RequestParam(name = "ikasturteaId", required = false) Long ikasturteaId,
                          Model model) {
        Ikaslea ikaslea = service.getIkaslea(id);
        List<Ikasturtea> ikasturteak = service.getIkasturteak(id);
        Long hautatutakoIkasturteaId = service.aukeratuIkasturtea(ikasturteaId, ikasturteak);
        model.addAttribute("ikaslea", ikaslea);
        model.addAttribute("ikasturteak", ikasturteak);
        model.addAttribute("hautatutakoIkasturteaId", hautatutakoIkasturteaId);
        IkasleakKontsultatuService.IkaslearenEbaluazioDatuak ebaluazioDatuak =
                service.getEbaluazioDatuak(id, hautatutakoIkasturteaId);
        model.addAttribute("ebaluazioMomentuak", ebaluazioDatuak.momentuak());
        model.addAttribute("matrikulak", ebaluazioDatuak.matrikulak());
        model.addAttribute("badituJokabideDesegokiak", service.badituJokabideDesegokiak(id));
        return TEMPLATE_ROOT + "ikaslea";
    }

    @GetMapping("/{id}/jokabide-desegokiak")
    public String jokabideDesegokiak(@PathVariable Long id, Model model) {
        model.addAttribute("ikaslea", service.getIkaslea(id));
        model.addAttribute("jokabideak", service.getJokabideDesegokiak(id));
        return TEMPLATE_ROOT + "jokabide-desegokiak";
    }

    @GetMapping("/{id}/asistentzia-kontrola")
    public String asistentziaKontrola(@PathVariable Long id,
                                      @RequestParam(name = "urtea", required = false) Integer urtea,
                                      @RequestParam(name = "hilabetea", required = false) Integer hilabetea,
                                      Model model) {
        LocalDate gaur = LocalDate.now();
        int hautatutakoUrtea = urtea != null ? urtea : gaur.getYear();
        int hautatutakoHilabetea = hilabetea != null && hilabetea >= 1 && hilabetea <= 12
                ? hilabetea
                : gaur.getMonthValue();
        IkasleakKontsultatuService.IkaslearenAsistentziaDatuak datuak =
                service.getAsistentziaDatuak(id, hautatutakoUrtea, hautatutakoHilabetea);
        model.addAttribute("ikaslea", datuak.ikaslea());
        model.addAttribute("ikasturtea", datuak.ikasturtea());
        model.addAttribute("urtea", datuak.urtea());
        model.addAttribute("hilabetea", datuak.hilabetea());
        model.addAttribute("hilabeteUrtea", datuak.hilabeteUrtea());
        model.addAttribute("egunak", datuak.egunak());
        model.addAttribute("lerroak", datuak.lerroak());
        return TEMPLATE_ROOT + "asistentzia-kontrola";
    }

    @GetMapping("/{id}/asistentzia-kontrola/jokabideak/{jokabideaId}/pdf")
    public ResponseEntity<?> jokabidePdfa(@PathVariable Long id,
                                          @PathVariable Long jokabideaId) throws IOException {
        JokabideDesegokia jokabidea;
        try {
            jokabidea = service.getJokabideDesegokia(id, jokabideaId);
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

    private int aukeratuPageSize(Integer eskatutakoa, HttpSession session) {
        if (eskatutakoa != null) {
            int balioa = Math.min(100, Math.max(20, eskatutakoa));
            session.setAttribute(PAGE_SIZE_SESSION_KEY, balioa);
            return balioa;
        }
        Object gordetakoa = session.getAttribute(PAGE_SIZE_SESSION_KEY);
        return gordetakoa instanceof Integer balioa ? balioa : 20;
    }
}
