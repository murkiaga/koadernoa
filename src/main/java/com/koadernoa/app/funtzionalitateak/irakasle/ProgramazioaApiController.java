package com.koadernoa.app.funtzionalitateak.irakasle;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.koadernoa.app.irakasleak.service.IrakasleaService;
import com.koadernoa.app.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.koadernoak.service.KoadernoaService;
import com.koadernoa.app.koadernoak.service.ProgramazioaService;

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

    private void checkAccess(Authentication auth, Koadernoa koadernoa) {
        var irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        if (!koadernoaService.irakasleakBadaukaSarbidea(irakaslea, koadernoa)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        }
    }

    // ---- GET UD / JP datuak modaletarako
    @GetMapping("/ud/{id}")
    public Map<String,Object> getUd(@PathVariable Long id,
                                    @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa k,
                                    Authentication auth) {
        checkAccess(auth, k);
        var ud = programazioaService.findUd(id);
        return Map.of("id", ud.getId(), "kodea", ud.getKodea(), "izenburua", ud.getIzenburua(), "orduak", ud.getOrduak());
    }

    @GetMapping("/jp/{id}")
    public Map<String,Object> getJp(@PathVariable Long id,
                                    @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa k,
                                    Authentication auth) {
        checkAccess(auth, k);
        var jp = programazioaService.findJarduera(id);
        return Map.of("id", jp.getId(), "udId", jp.getUnitatea().getId(), "izenburua", jp.getIzenburua(), "orduak", jp.getOrduak());
    }

    // ---- CREATE / UPDATE UD
    @PostMapping("/ud")
    public ResponseEntity<?> createUd(@RequestBody Map<String,Object> body,
                                      @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa k,
                                      Authentication auth) {
        checkAccess(auth, k);
        programazioaService.addUd(programazioaService.getOrCreateForKoadernoa(k).getId(),
                (String) body.get("kodea"), (String) body.get("izenburua"),
                ((Number)body.getOrDefault("orduak",0)).intValue(), /*pos*/ 999);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PutMapping("/ud/{id}")
    public ResponseEntity<?> updateUd(@PathVariable Long id, @RequestBody Map<String,Object> body,
                                      @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa k,
                                      Authentication auth) {
        checkAccess(auth, k);
        programazioaService.updateUd(id, (String) body.get("kodea"), (String) body.get("izenburua"),
                ((Number)body.getOrDefault("orduak",0)).intValue(), null);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ---- CREATE / UPDATE JP
    @PostMapping("/jp")
    public ResponseEntity<?> createJp(@RequestBody Map<String,Object> body,
                                      @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa k,
                                      Authentication auth) {
        checkAccess(auth, k);
        Long udId = ((Number) body.get("udId")).longValue();
        programazioaService.addJardueraPlanifikatua(udId, (String) body.get("izenburua"),
                ((Number)body.getOrDefault("orduak",0)).intValue());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PutMapping("/jp/{id}")
    public ResponseEntity<?> updateJp(@PathVariable Long id, @RequestBody Map<String,Object> body,
                                      @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa k,
                                      Authentication auth) {
        checkAccess(auth, k);
        programazioaService.updateJardueraPlanifikatua(id, (String) body.get("izenburua"),
                ((Number)body.getOrDefault("orduak",0)).intValue());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ---- Reorder UD
    @PostMapping("/ud/ordenatu")
    public ResponseEntity<?> reorderUd(@RequestBody Map<String,List<Long>> body,
                                       @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa k,
                                       Authentication auth) {
        checkAccess(auth, k);
        programazioaService.reorderUd(k.getId(), body.get("udIds"));
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ---- Move/Reorder JP (cross-UD onartzen da)
    @PostMapping("/jp/mugitu")
    public ResponseEntity<?> moveJp(@RequestBody Map<String,Object> body,
                                    @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa k,
                                    Authentication auth) {
        checkAccess(auth, k);
        Long jpId = ((Number)body.get("jpId")).longValue();
        Long toUdId = ((Number)body.get("toUdId")).longValue();
        int newIndex = ((Number)body.get("newIndex")).intValue();
        programazioaService.moveOrReorderJarduera(jpId, toUdId, newIndex);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
