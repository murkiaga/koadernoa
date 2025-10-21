package com.koadernoa.app.funtzionalitateak.irakasle;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Ebaluaketa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoaService;
import com.koadernoa.app.objektuak.koadernoak.service.ProgramazioaService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/irakasle/programazioa/api")
@SessionAttributes("koadernoAktiboa")
@RequiredArgsConstructor
public class ProgramazioaApiController {

    private final ProgramazioaService programazioaService;
    private final IrakasleaService irakasleaService;
    private final KoadernoaService koadernoaService;

    // ======== ACCESS ========
    private void checkAccess(Authentication auth, Koadernoa k) {
        var irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        if (k == null || !koadernoaService.irakasleakBadaukaSarbidea(irakaslea, k)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        }
    }

    private Koadernoa resolveKoadernoa(Authentication auth,
                                       @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa sessionK,
                                       Long koadernoIdBody) {
        if (koadernoIdBody != null) return koadernoaService.findById(koadernoIdBody);
        if (sessionK != null && sessionK.getId() != null) return koadernoaService.findById(sessionK.getId());
        var irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        if (irakaslea == null) return null;
        return irakaslea.getKoadernoak().stream()
                .filter(k -> k.getEgutegia() != null
                        && k.getEgutegia().getIkasturtea() != null
                        && k.getEgutegia().getIkasturtea().isAktiboa())
                .findFirst()
                .orElse(null);
    }

    // ======== GET (modaletarako datuak) ========
    @GetMapping("/ud/{id}")
    public Map<String,Object> getUd(@PathVariable Long id,
                                    @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa k,
                                    Authentication auth) {
        checkAccess(auth, k);
        if (!programazioaService.udDagokioKoadernoari(id, k.getId()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        var ud = programazioaService.findUd(id);
        return Map.of(
            "id", ud.getId(),
            "ebaluaketaId", ud.getEbaluaketa() != null ? ud.getEbaluaketa().getId() : null,
            "kodea", ud.getKodea(),
            "izenburua", ud.getIzenburua(),
            "orduak", ud.getOrduak()
        );
    }

    @GetMapping("/jp/{id}")
    public Map<String,Object> getJp(@PathVariable Long id,
                                    @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa k,
                                    Authentication auth) {
        checkAccess(auth, k);
        if (!programazioaService.jpDagokioKoadernoari(id, k.getId()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        var jp = programazioaService.findJarduera(id);
        return Map.of("id", jp.getId(), "udId", jp.getUnitatea().getId(), "izenburua", jp.getIzenburua(), "orduak", jp.getOrduak());
    }

 // ======== EBALUAKETA: GET/CREATE/UPDATE/DELETE ========

    @GetMapping("/ebal/{id}")
    public Map<String,Object> getEbal(@PathVariable Long id,
                                      @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa k,
                                      Authentication auth) {
        checkAccess(auth, k);
        if (!programazioaService.ebalDagokioKoadernoari(id, k.getId()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);

        Ebaluaketa e = programazioaService.findEbaluaketa(id);
        return Map.of(
            "id", e.getId(),
            "izena", e.getIzena(),
            "hasieraData", e.getHasieraData() != null ? e.getHasieraData().toString() : null,
            "bukaeraData", e.getBukaeraData() != null ? e.getBukaeraData().toString() : null,
            "ordena", e.getOrdena()
        );
    }

    @PostMapping("/ebal")
    public ResponseEntity<?> createEbal(@RequestBody Map<String,Object> body,
                                        @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa kSession,
                                        Authentication auth) {
        Long koadernoIdBody = body.get("koadernoId") instanceof Number ? ((Number) body.get("koadernoId")).longValue() : null;
        Koadernoa k = resolveKoadernoa(auth, kSession, koadernoIdBody);
        checkAccess(auth, k);

        String izena = (String) body.getOrDefault("izena", "Ebaluaketa");
        String sHas = (String) body.get("hasieraData");
        String sBuk = (String) body.get("bukaeraData");

        java.time.LocalDate has = (sHas == null || sHas.isBlank()) ? null : java.time.LocalDate.parse(sHas);
        java.time.LocalDate buk = (sBuk == null || sBuk.isBlank()) ? null : java.time.LocalDate.parse(sBuk);

        programazioaService.addEbaluaketaForKoadernoa(k.getId(), izena, has, buk);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PutMapping("/ebal/{id}")
    public ResponseEntity<?> updateEbal(@PathVariable Long id,
                                        @RequestBody Map<String,Object> body,
                                        @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa k,
                                        Authentication auth) {
        checkAccess(auth, k);
        if (!programazioaService.ebalDagokioKoadernoari(id, k.getId()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);

        String izena = (String) body.get("izena");
        String sHas = (String) body.get("hasieraData");
        String sBuk = (String) body.get("bukaeraData");

        java.time.LocalDate has = (sHas == null || sHas.isBlank()) ? null : java.time.LocalDate.parse(sHas);
        java.time.LocalDate buk = (sBuk == null || sBuk.isBlank()) ? null : java.time.LocalDate.parse(sBuk);

        programazioaService.updateEbaluaketa(id, izena, has, buk);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/ebal/{id}")
    public ResponseEntity<?> deleteEbal(@PathVariable Long id,
                                        @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa k,
                                        Authentication auth) {
        checkAccess(auth, k);
        if (!programazioaService.ebalDagokioKoadernoari(id, k.getId()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);

        programazioaService.deleteEbaluaketa(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }
    
 // ======== UD: CREATE / UPDATE / DELETE ========
    @PostMapping("/ud")
    public ResponseEntity<?> createUd(@RequestBody Map<String,Object> body,
                                      @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa kSession,
                                      Authentication auth) {
        Long koadernoIdBody = body.get("koadernoId") instanceof Number ? ((Number) body.get("koadernoId")).longValue() : null;
        Koadernoa k = resolveKoadernoa(auth, kSession, koadernoIdBody);
        checkAccess(auth, k);

        Long ebaluaketaId = ((Number) body.get("ebaluaketaId")).longValue();

        // Sarbide-guardia sinplea: UD ez dugu sortzen baina ebaluaketa koaderno horrena dela egiaztatu nahi baduzu,
        // sartu ProgramazioaService-n existsByIdAndProgramazioa_Koadernoa_Id antzeko bat Ebaluaketarako.

        programazioaService.addUdToEbaluaketa(
            ebaluaketaId,
            (String) body.get("kodea"),
            (String) body.get("izenburua"),
            ((Number) body.getOrDefault("orduak", 0)).intValue(),
            999
        );
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PutMapping("/ud/{id}")
    public ResponseEntity<?> updateUd(@PathVariable Long id, @RequestBody Map<String,Object> body,
                                      @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa k,
                                      Authentication auth) {
        checkAccess(auth, k);
        if (!programazioaService.udDagokioKoadernoari(id, k.getId()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        programazioaService.updateUd(
            id,
            (String) body.get("kodea"),
            (String) body.get("izenburua"),
            ((Number) body.getOrDefault("orduak", 0)).intValue(),
            null // posizioa ez ukitu
        );
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/ud/{id}")
    public ResponseEntity<?> deleteUd(@PathVariable Long id,
                                      @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa k,
                                      Authentication auth) {
        checkAccess(auth, k);
        if (!programazioaService.udDagokioKoadernoari(id, k.getId()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        programazioaService.deleteUd(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ======== JP: CREATE / UPDATE / MOVE / DELETE ========
    @PostMapping("/jp")
    public ResponseEntity<?> createJp(@RequestBody Map<String,Object> body,
                                      @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa k,
                                      Authentication auth) {
        checkAccess(auth, k);
        Long udId = ((Number) body.get("udId")).longValue();
        if (!programazioaService.udDagokioKoadernoari(udId, k.getId()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        programazioaService.addJardueraPlanifikatua(udId,
                (String) body.get("izenburua"),
                ((Number) body.getOrDefault("orduak", 0)).intValue());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PutMapping("/jp/{id}")
    public ResponseEntity<?> updateJp(@PathVariable Long id, @RequestBody Map<String,Object> body,
                                      @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa k,
                                      Authentication auth) {
        checkAccess(auth, k);
        if (!programazioaService.jpDagokioKoadernoari(id, k.getId()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        programazioaService.updateJardueraPlanifikatua(id,
                (String) body.get("izenburua"),
                ((Number) body.getOrDefault("orduak", 0)).intValue());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/jp/mugitu")
    public ResponseEntity<?> moveJp(@RequestBody Map<String,Object> body,
                                    @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa k,
                                    Authentication auth) {
        checkAccess(auth, k);
        Long jpId = ((Number) body.get("jpId")).longValue();
        Long toUdId = ((Number) body.get("toUdId")).longValue();
        int newIndex = ((Number) body.get("newIndex")).intValue();
        // Egiaztatu source eta target koaderno berekoak direla
        if (!programazioaService.jpDagokioKoadernoari(jpId, k.getId())
         || !programazioaService.udDagokioKoadernoari(toUdId, k.getId()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        programazioaService.moveOrReorderJarduera(jpId, toUdId, newIndex);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/jp/{id}")
    public ResponseEntity<?> deleteJp(@PathVariable Long id,
                                      @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa k,
                                      Authentication auth) {
        checkAccess(auth, k);
        if (!programazioaService.jpDagokioKoadernoari(id, k.getId()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        programazioaService.deleteJardueraPlanifikatua(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ======== Error JSON garbiak ========
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<?> status(org.springframework.web.server.ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason()));
    }
}