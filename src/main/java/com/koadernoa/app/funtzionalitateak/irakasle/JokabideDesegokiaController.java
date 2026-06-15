package com.koadernoa.app.funtzionalitateak.irakasle;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.koadernoa.app.objektuak.jokabidea.entitateak.NeurriZuzentzailea;
import com.koadernoa.app.objektuak.jokabidea.entitateak.PortaeraArrazoia;
import org.springframework.web.bind.annotation.*;
import com.koadernoa.app.objektuak.jokabidea.entitateak.JokabideDesegokia;
import com.koadernoa.app.objektuak.jokabidea.repository.*;
import com.koadernoa.app.objektuak.jokabidea.service.*;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/irakasle/jokabide-desegokia")
@RequiredArgsConstructor
public class JokabideDesegokiaController {
    private final PortaeraArrazoiaRepository arrazoiRepository;
    private final NeurriZuzentzaileaRepository neurriRepository;
    private final JokabideDesegokiaRepository repository;
    private final IkasleEgunJardueraService testuinguruService;
    private final JokabideDesegokiaPdfService pdfService;

    @GetMapping("/form-data")
    public ResponseEntity<?> formData() {
        var arrazoiak=arrazoiRepository.findByAktiboTrueOrderByOrdenaAscIdAsc(); var neurriak=neurriRepository.findByAktiboTrueOrderByOrdenaAscIdAsc();
        Map<String, Object> emaitza = new java.util.LinkedHashMap<>();
        emaitza.put("portaeraArrazoiak", arrazoiak.stream().map(a->Map.of("id",a.getId(),"kodea",a.getKodea(),"testua",a.getTestua())).toList());
        emaitza.put("neurriZuzentzaileak", neurriak.stream().map(n->Map.of("id",n.getId(),"kodea",n.getKodea(),"testua",n.getTestua())).toList());
        emaitza.put("portaeraArrazoiaDefektuzkoaId", arrazoiRepository.findFirstByDefektuzkoaTrueAndAktiboTrueOrderByOrdenaAscIdAsc().map(a->a.getId()).orElse(null));
        emaitza.put("neurriZuzentzaileaDefektuzkoaId", neurriRepository.findFirstByDefektuzkoaTrueAndAktiboTrueOrderByOrdenaAscIdAsc().map(n->n.getId()).orElse(null));
        return ResponseEntity.ok(emaitza);
    }


    @GetMapping
    public ResponseEntity<?> lortuEgunekoa(
        @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoa,
        @RequestParam Long ikasleaId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
        Authentication auth) {
        try {
            testuinguruService.egiaztatu(koadernoa, ikasleaId, auth);
            return repository.findFirstByIkasleaIdAndKoadernoaIdAndDataOrderByCreatedAtDesc(
                    ikasleaId, koadernoa.getId(), data)
                .<ResponseEntity<?>>map(j -> ResponseEntity.ok(jokabideDto(j)))
                .orElseGet(() -> ResponseEntity.ok(Map.of("badago", false)));
        } catch (SecurityException e) {
            return errorea(HttpStatus.FORBIDDEN, e);
        } catch (IllegalArgumentException e) {
            return errorea(HttpStatus.BAD_REQUEST, e);
        }
    }

    @PostMapping
    @Transactional(rollbackFor=Exception.class)
    public ResponseEntity<?> sortu(@SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa koadernoa,
        @RequestParam Long ikasleaId, @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate data,
        @RequestParam Long portaeraArrazoiaId, @RequestParam Long neurriZuzentzaileaId,
        @RequestParam String deskribapenZehatza, Authentication auth) {
        JokabideDesegokiaPdfService.SortutakoPdfa pdf=null;
        try {
            if (deskribapenZehatza==null || deskribapenZehatza.trim().isEmpty()) throw new IllegalArgumentException("Deskribapen zehatza beharrezkoa da.");
            var t=testuinguruService.egiaztatu(koadernoa, ikasleaId, auth);
            if (repository.findFirstByIkasleaIdAndKoadernoaIdAndDataOrderByCreatedAtDesc(
                    ikasleaId, koadernoa.getId(), data).isPresent()) {
                throw new IllegalArgumentException("Egun horretan jokabide desegoki bat badago dagoeneko; editatu lehendik dagoena.");
            }
            var arrazoia=arrazoiRepository.findById(portaeraArrazoiaId).filter(a->a.isAktibo()).orElseThrow(()->new IllegalArgumentException("Portaera arrazoia ez da baliozkoa."));
            var neurria=neurriRepository.findById(neurriZuzentzaileaId).filter(n->n.isAktibo()).orElseThrow(()->new IllegalArgumentException("Neurri zuzentzailea ez da baliozkoa."));
            JokabideDesegokia j=new JokabideDesegokia(); j.setIkaslea(t.matrikula().getIkaslea()); j.setKoadernoa(t.matrikula().getKoadernoa()); j.setIrakaslea(t.irakaslea());
            j.setModuloa(t.matrikula().getKoadernoa().getModuloa()); j.setData(data); j.setPortaeraArrazoia(arrazoia); j.setNeurriZuzentzailea(neurria); j.setDeskribapenZehatza(deskribapenZehatza.trim());
            pdf=pdfService.sortu(j); j.setPdfPath(pdf.path()); j.setPdfFilename(pdf.filename()); repository.saveAndFlush(j);
            return ResponseEntity.ok(Map.of("ok",true,"id",j.getId(),"pdfUrl","/irakasle/jokabide-desegokia/"+j.getId()+"/pdf","mezua","Jokabide desegokiaren ohartarazpena sortu da."));
        } catch (SecurityException e) { if(pdf!=null)pdfService.ezabatuIsilean(pdf.path()); return errorea(HttpStatus.FORBIDDEN,e); }
          catch (IllegalArgumentException e) { if(pdf!=null)pdfService.ezabatuIsilean(pdf.path()); return errorea(HttpStatus.BAD_REQUEST,e); }
          catch (Exception e) { if(pdf!=null)pdfService.ezabatuIsilean(pdf.path()); return ResponseEntity.status(500).body(Map.of("ok",false,"errorea","Ezin izan da ohartarazpena eta PDFa sortu.")); }
    }


    @PutMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<?> eguneratu(
        @PathVariable Long id,
        @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoa,
        @RequestParam Long ikasleaId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
        @RequestParam Long portaeraArrazoiaId,
        @RequestParam Long neurriZuzentzaileaId,
        @RequestParam String deskribapenZehatza,
        Authentication auth) {
        JokabideDesegokiaPdfService.SortutakoPdfa pdfBerria = null;
        JokabideDesegokia j = null;
        PortaeraArrazoia aurrekoArrazoia = null;
        NeurriZuzentzailea aurrekoNeurria = null;
        String aurrekoDeskribapena = null;
        String aurrekoPdfPath = null;
        String aurrekoPdfFilename = null;
        try {
            if (deskribapenZehatza == null || deskribapenZehatza.trim().isEmpty()) {
                throw new IllegalArgumentException("Deskribapen zehatza beharrezkoa da.");
            }
            testuinguruService.egiaztatu(koadernoa, ikasleaId, auth);
            j = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Jokabide desegokia ez da aurkitu."));
            if (!j.getKoadernoa().getId().equals(koadernoa.getId())
                    || !j.getIkaslea().getId().equals(ikasleaId)
                    || !j.getData().equals(data)) {
                throw new SecurityException("Ez duzu jokabide desegoki hau editatzeko baimenik.");
            }
            Long unekoArrazoiaId = j.getPortaeraArrazoia().getId();
            Long unekoNeurriaId = j.getNeurriZuzentzailea().getId();
            var arrazoia = arrazoiRepository.findById(portaeraArrazoiaId)
                .filter(a -> a.isAktibo() || a.getId().equals(unekoArrazoiaId))
                .orElseThrow(() -> new IllegalArgumentException("Portaera arrazoia ez da baliozkoa."));
            var neurria = neurriRepository.findById(neurriZuzentzaileaId)
                .filter(n -> n.isAktibo() || n.getId().equals(unekoNeurriaId))
                .orElseThrow(() -> new IllegalArgumentException("Neurri zuzentzailea ez da baliozkoa."));

            aurrekoArrazoia = j.getPortaeraArrazoia();
            aurrekoNeurria = j.getNeurriZuzentzailea();
            aurrekoDeskribapena = j.getDeskribapenZehatza();
            aurrekoPdfPath = j.getPdfPath();
            aurrekoPdfFilename = j.getPdfFilename();

            j.setPortaeraArrazoia(arrazoia);
            j.setNeurriZuzentzailea(neurria);
            j.setDeskribapenZehatza(deskribapenZehatza.trim());
            pdfBerria = pdfService.sortu(j);
            j.setPdfPath(pdfBerria.path());
            j.setPdfFilename(pdfBerria.filename());
            repository.saveAndFlush(j);
            antolatuPdfOrdezkapena(aurrekoPdfPath, pdfBerria.path());
            return ResponseEntity.ok(Map.of(
                "ok", true,
                "id", j.getId(),
                "pdfUrl", "/irakasle/jokabide-desegokia/" + j.getId() + "/pdf",
                "mezua", "Jokabide desegokiaren ohartarazpena eguneratu da."
            ));
        } catch (SecurityException e) {
            leheneratu(j, aurrekoArrazoia, aurrekoNeurria, aurrekoDeskribapena, aurrekoPdfPath, aurrekoPdfFilename);
            if (pdfBerria != null) pdfService.ezabatuIsilean(pdfBerria.path());
            return errorea(HttpStatus.FORBIDDEN, e);
        } catch (IllegalArgumentException e) {
            leheneratu(j, aurrekoArrazoia, aurrekoNeurria, aurrekoDeskribapena, aurrekoPdfPath, aurrekoPdfFilename);
            if (pdfBerria != null) pdfService.ezabatuIsilean(pdfBerria.path());
            return errorea(HttpStatus.BAD_REQUEST, e);
        } catch (Exception e) {
            leheneratu(j, aurrekoArrazoia, aurrekoNeurria, aurrekoDeskribapena, aurrekoPdfPath, aurrekoPdfFilename);
            if (pdfBerria != null) pdfService.ezabatuIsilean(pdfBerria.path());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("ok", false, "errorea", "Ezin izan da ohartarazpena eta PDFa eguneratu."));
        }
    }

    private Map<String, Object> jokabideDto(JokabideDesegokia j) {
        return Map.of(
            "badago", true,
            "id", j.getId(),
            "portaeraArrazoiaId", j.getPortaeraArrazoia().getId(),
            "neurriZuzentzaileaId", j.getNeurriZuzentzailea().getId(),
            "deskribapenZehatza", j.getDeskribapenZehatza(),
            "pdfUrl", "/irakasle/jokabide-desegokia/" + j.getId() + "/pdf"
        );
    }

    private void leheneratu(
        JokabideDesegokia j,
        PortaeraArrazoia arrazoia,
        NeurriZuzentzailea neurria,
        String deskribapena,
        String pdfPath,
        String pdfFilename) {
        if (j == null || arrazoia == null || neurria == null) return;
        j.setPortaeraArrazoia(arrazoia);
        j.setNeurriZuzentzailea(neurria);
        j.setDeskribapenZehatza(deskribapena);
        j.setPdfPath(pdfPath);
        j.setPdfFilename(pdfFilename);
    }

    private void antolatuPdfOrdezkapena(String aurrekoPath, String pathBerria) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                pdfService.ezabatuIsilean(aurrekoPath);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    pdfService.ezabatuIsilean(pathBerria);
                }
            }
        });
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<?> pdf(@PathVariable Long id, @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa koadernoa, Authentication auth) throws IOException {
        var j=repository.findById(id).orElseThrow(()->new IllegalArgumentException("Ohartarazpena ez da aurkitu."));
        testuinguruService.egiaztatu(koadernoa,j.getIkaslea().getId(),auth);
        if(!j.getKoadernoa().getId().equals(koadernoa.getId())) return ResponseEntity.status(403).body(Map.of("errorea","Ez duzu dokumentu hau ikusteko baimenik."));
        Path p=Paths.get(j.getPdfPath()).normalize(); if(!Files.isRegularFile(p)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=\""+j.getPdfFilename()+"\"").body(Files.readAllBytes(p));
    }
    private ResponseEntity<?> errorea(HttpStatus s, RuntimeException e){return ResponseEntity.status(s).body(Map.of("ok",false,"errorea",e.getMessage()));}
}
