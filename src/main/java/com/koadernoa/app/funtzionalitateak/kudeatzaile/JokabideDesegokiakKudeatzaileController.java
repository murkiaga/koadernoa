package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.jokabidea.entitateak.JokabideDesegokia;
import com.koadernoa.app.objektuak.jokabidea.repository.JokabideDesegokiaRepository;
import com.koadernoa.app.objektuak.jokabidea.repository.NeurriZuzentzaileaRepository;
import com.koadernoa.app.objektuak.jokabidea.repository.PortaeraArrazoiaRepository;
import com.koadernoa.app.objektuak.jokabidea.service.JokabideDesegokiaPdfService;
import com.koadernoa.app.objektuak.jokabidea.service.JokabideDesegokiaService;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;
import com.koadernoa.app.objektuak.mezuak.service.MezuaService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/kudeatzaile/jokabide-desegokiak")
@RequiredArgsConstructor
public class JokabideDesegokiakKudeatzaileController {

    private final JokabideDesegokiaRepository repository;
    private final IrakasleaService irakasleaService;
    private final IkasturteaRepository ikasturteaRepository;
    private final MatrikulaRepository matrikulaRepository;
    private final IrakasleaRepository irakasleaRepository;
    private final PortaeraArrazoiaRepository portaeraArrazoiaRepository;
    private final NeurriZuzentzaileaRepository neurriZuzentzaileaRepository;
    private final JokabideDesegokiaPdfService pdfService;
    private final JokabideDesegokiaService jokabideDesegokiaService;
    private final MezuaService mezuaService;

    @GetMapping
    @Transactional(readOnly = true)
    public String index(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataHasiera,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAmaiera,
            @RequestParam(required = false) Long ikasleaId,
            @RequestParam(required = false) String ikaslea,
            @RequestParam(required = false) Long moduloaId,
            @RequestParam(required = false) Long taldeaId,
            @RequestParam(required = false) Boolean jasota,
            HttpServletRequest request,
            Model model) {
        // Parametroa ez badator bakarrik aplikatu defektua. `?dataHasiera=`
        // erabiltzaileak data-iragazkia berariaz hustu duela esan nahi du.
        if (dataHasiera == null && !request.getParameterMap().containsKey("dataHasiera")) {
            dataHasiera = ikasturteaRepository.findFirstByAktiboaTrueOrderByIdDesc()
                    .map(ikasturtea -> ikasturtekoIrailarenLehena(ikasturtea.getIzena()))
                    .orElse(null);
        }
        String ikasleFiltroa = ikaslea == null ? null : ikaslea.trim();
        model.addAttribute("jokabideak", repository.bilatuKudeatzailearentzat(
                dataHasiera, dataAmaiera, ikasleaId, ikasleFiltroa, moduloaId, taldeaId, jasota));
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

    @GetMapping("/berria")
    @Transactional(readOnly = true)
    public String berria(Model model) {
        prestatuFormularioa(model);
        model.addAttribute("data", LocalDate.now());
        return "kudeatzaile/jokabide-desegokiak/berria";
    }

    @PostMapping("/berria")
    @Transactional(rollbackFor = Exception.class)
    public String sortu(
            @RequestParam(required = false) Long ikasleaId,
            @RequestParam(required = false) Long irakasleaId,
            @RequestParam(required = false) String ikasleaBilaketa,
            @RequestParam(required = false) String irakasleaBilaketa,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam Long portaeraArrazoiaId,
            @RequestParam Long neurriZuzentzaileaId,
            @RequestParam String deskribapenZehatza,
            @RequestParam(required = false) String aplikatutakoNeurria,
            Authentication auth,
            Model model) throws IOException {
        JokabideDesegokiaPdfService.SortutakoPdfa pdfa = null;
        try {
            if (ikasleaId == null) throw new IllegalArgumentException("Aukeratu ikasle bat bilaketako emaitzetatik.");
            if (irakasleaId == null) throw new IllegalArgumentException("Aukeratu irakasle bat bilaketako emaitzetatik.");
            String deskribapena = beharrezkoTestua(deskribapenZehatza, "Deskribapen zehatza beharrezkoa da.");
            String neurriaTestua = aukerakoTestua(aplikatutakoNeurria);
            var matrikula = matrikulaRepository.findMatrikulatuakAktiboIkasturteanByIkaslea(ikasleaId).stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Ikasleak ez du matrikula aktiborik ikasturte honetan."));
            var irakaslea = irakasleaRepository.findById(irakasleaId)
                    .orElseThrow(() -> new IllegalArgumentException("Irakaslea ez da baliozkoa."));
            var arrazoia = portaeraArrazoiaRepository.findById(portaeraArrazoiaId)
                    .filter(a -> a.isAktibo())
                    .orElseThrow(() -> new IllegalArgumentException("Portaera arrazoia ez da baliozkoa."));
            var neurriZuzentzailea = neurriZuzentzaileaRepository.findById(neurriZuzentzaileaId)
                    .filter(n -> n.isAktibo())
                    .orElseThrow(() -> new IllegalArgumentException("Neurri zuzentzailea ez da baliozkoa."));
            if (repository.findFirstByIkasleaIdAndKoadernoaIdAndDataOrderByCreatedAtDesc(
                    matrikula.getIkaslea().getId(), matrikula.getKoadernoa().getId(), data).isPresent()) {
                throw new IllegalArgumentException("Ikasle eta modulu horrek egun horretan jokabide desegoki bat dauka dagoeneko.");
            }

            JokabideDesegokia jokabidea = new JokabideDesegokia();
            jokabidea.setIkaslea(matrikula.getIkaslea());
            jokabidea.setKoadernoa(matrikula.getKoadernoa());
            jokabidea.setModuloa(null);
            jokabidea.setIrakaslea(irakaslea);
            jokabidea.setData(data);
            jokabidea.setPortaeraArrazoia(arrazoia);
            jokabidea.setDeskribapenZehatza(deskribapena);
            jokabidea.setNeurriZuzentzailea(neurriZuzentzailea);
            jokabidea.setAplikatutakoNeurria(neurriaTestua);
            pdfa = pdfService.sortu(jokabidea);
            jokabidea.setPdfPath(pdfa.path());
            jokabidea.setPdfFilename(pdfa.filename());
            jokabideDesegokiaService.sortu(jokabidea);
            Irakaslea kudeatzailea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
            mezuaService.bidaliKudeatzaileakSortutakoJokabidearenAbisua(
                    jokabidea, kudeatzailea, irakaslea,
                    "/irakasle/jokabide-desegokia/" + jokabidea.getId() + "/nire-pdf");
            return "redirect:/kudeatzaile/jokabide-desegokiak?sortuta=true";
        } catch (IllegalArgumentException e) {
            if (pdfa != null) pdfService.ezabatuIsilean(pdfa.path());
            prestatuFormularioa(model);
            model.addAttribute("errorea", e.getMessage());
            model.addAttribute("ikasleaId", ikasleaId);
            model.addAttribute("irakasleaId", irakasleaId);
            model.addAttribute("ikasleaBilaketa", ikasleaBilaketa);
            model.addAttribute("irakasleaBilaketa", irakasleaBilaketa);
            model.addAttribute("data", data);
            model.addAttribute("portaeraArrazoiaId", portaeraArrazoiaId);
            model.addAttribute("neurriZuzentzaileaId", neurriZuzentzaileaId);
            model.addAttribute("deskribapenZehatza", deskribapenZehatza);
            model.addAttribute("aplikatutakoNeurria", aplikatutakoNeurria);
            return "kudeatzaile/jokabide-desegokiak/berria";
        } catch (IOException e) {
            if (pdfa != null) pdfService.ezabatuIsilean(pdfa.path());
            prestatuFormularioa(model);
            model.addAttribute("errorea", "Ezin izan da PDF dokumentua sortu.");
            model.addAttribute("data", data);
            return "kudeatzaile/jokabide-desegokiak/berria";
        } catch (RuntimeException e) {
            if (pdfa != null) pdfService.ezabatuIsilean(pdfa.path());
            throw e;
        }
    }

    private void prestatuFormularioa(Model model) {
        model.addAttribute("portaeraArrazoiak", portaeraArrazoiaRepository.findByAktiboTrueOrderByOrdenaAscIdAsc());
        model.addAttribute("neurriZuzentzaileak", neurriZuzentzaileaRepository.findByAktiboTrueOrderByOrdenaAscIdAsc());
        model.addAttribute("portaeraArrazoiaDefektuzkoaId", portaeraArrazoiaRepository
                .findFirstByDefektuzkoaTrueAndAktiboTrueOrderByOrdenaAscIdAsc().map(a -> a.getId()).orElse(null));
        model.addAttribute("neurriZuzentzaileaDefektuzkoaId", neurriZuzentzaileaRepository
                .findFirstByDefektuzkoaTrueAndAktiboTrueOrderByOrdenaAscIdAsc().map(n -> n.getId()).orElse(null));
    }

    @GetMapping("/irakasleak/bilatu")
    @ResponseBody
    public List<Map<String, Object>> bilatuIrakasleak(@RequestParam(name = "q", required = false) String q) {
        return irakasleaService.bilatuAukerak(q).stream()
                .map(i -> Map.<String, Object>of(
                        "id", i.getId(),
                        "izena", i.getIzena() == null ? "" : i.getIzena(),
                        "emaila", i.getEmaila() == null ? "" : i.getEmaila()))
                .toList();
    }

    private String beharrezkoTestua(String testua, String errorea) {
        if (testua == null || testua.isBlank()) throw new IllegalArgumentException(errorea);
        String garbia = testua.trim();
        if (garbia.length() > 10000) throw new IllegalArgumentException("Testua luzeegia da (gehienez 10.000 karaktere).");
        return garbia;
    }

    private String aukerakoTestua(String testua) {
        if (testua == null || testua.isBlank()) return null;
        String garbia = testua.trim();
        if (garbia.length() > 10000) throw new IllegalArgumentException("Testua luzeegia da (gehienez 10.000 karaktere).");
        return garbia;
    }

    private LocalDate ikasturtekoIrailarenLehena(String ikasturteIzena) {
        if (ikasturteIzena == null) return null;
        try {
            return LocalDate.of(Integer.parseInt(ikasturteIzena.trim().substring(0, 4)), 9, 1);
        } catch (RuntimeException ex) {
            return null;
        }
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
