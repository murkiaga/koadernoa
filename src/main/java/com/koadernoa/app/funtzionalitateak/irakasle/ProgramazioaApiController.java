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

    private void checkAccess(Authentication auth, Koadernoa k) {
        var irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        if (k == null || !koadernoaService.irakasleakBadaukaSarbidea(irakaslea, k)) {
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
                                      @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa kSession,
                                      Authentication auth) {
        Long koadernoIdBody = body.get("koadernoId") instanceof Number ? ((Number) body.get("koadernoId")).longValue() : null;
        Koadernoa k = resolveKoadernoa(auth, kSession, koadernoIdBody);
        checkAccess(auth, k);

        var prog = programazioaService.getOrCreateForKoadernoa(k);
        programazioaService.addUd(prog.getId(),
                (String) body.get("kodea"),
                (String) body.get("izenburua"),
                ((Number) body.getOrDefault("orduak", 0)).intValue(),
                999);
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
    public ResponseEntity<?> reorderUd(@RequestBody Map<String,Object> body,
                                       @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa kSession,
                                       Authentication auth) {
        Long koadernoIdBody = body.get("koadernoId") instanceof Number ? ((Number) body.get("koadernoId")).longValue() : null;
        Koadernoa k = resolveKoadernoa(auth, kSession, koadernoIdBody);
        checkAccess(auth, k);

        @SuppressWarnings("unchecked")
        var udIds = (java.util.List<Number>) body.get("udIds");
        var ids = udIds.stream().map(n -> n.longValue()).toList();
        programazioaService.reorderUd(k.getId(), ids);
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
    
    private Koadernoa resolveKoadernoa(Authentication auth,
            @SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa sessionK,
            Long koadernoIdBody) {
		// 1) Body > 2) Session > 3) Lehen aktiboa irakaslearen koadernoetatik
		if (koadernoIdBody != null) {
			return koadernoaService.findById(koadernoIdBody);
		}
		if (sessionK != null && sessionK.getId() != null) {
			return koadernoaService.findById(sessionK.getId()); // freskatu DBtik
		}
		var irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
		if (irakaslea == null) return null;
		return irakaslea.getKoadernoak().stream()
				.filter(k -> k.getEgutegia() != null
					&& k.getEgutegia().getIkasturtea() != null
					&& k.getEgutegia().getIkasturtea().isAktiboa())
				.findFirst()
				.orElse(null);
	}
    
    
//---Errore mezu garbiagoak jasotzeko
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<?> status(org.springframework.web.server.ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason()));
    }
}
