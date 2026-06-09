package com.koadernoa.app.funtzionalitateak.irakasle;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.koadernoa.app.objektuak.jokabidea.service.IkasleEgunJardueraService;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/irakasle/oharrak")
@RequiredArgsConstructor
public class IkasleEgunOharraController {
    private final IkasleEgunJardueraService service;

    @GetMapping
    public ResponseEntity<?> lortu(@SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa koadernoa,
        @RequestParam Long ikasleaId, @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate data, Authentication auth) {
        try { return ResponseEntity.ok(service.lortuOharra(koadernoa, ikasleaId, data, auth)
            .<Object>map(o -> Map.of("id", o.getId(), "testua", o.getTestua(), "badago", true)).orElse(Map.of("testua", "", "badago", false))); }
        catch (SecurityException e) { return errorea(HttpStatus.FORBIDDEN, e); } catch (IllegalArgumentException e) { return errorea(HttpStatus.BAD_REQUEST, e); }
    }

    @PostMapping
    public ResponseEntity<?> gorde(@SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa koadernoa,
        @RequestParam Long ikasleaId, @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate data,
        @RequestParam String testua, Authentication auth) {
        try { var o=service.gordeOharra(koadernoa, ikasleaId, data, testua, auth); return ResponseEntity.ok(Map.of("ok",true,"id",o.getId(),"mezua","Oharra gorde da.")); }
        catch (SecurityException e) { return errorea(HttpStatus.FORBIDDEN, e); } catch (IllegalArgumentException e) { return errorea(HttpStatus.BAD_REQUEST, e); }
    }

    @DeleteMapping
    public ResponseEntity<?> ezabatu(@SessionAttribute(value="koadernoAktiboa", required=false) Koadernoa koadernoa,
        @RequestParam Long ikasleaId, @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate data, Authentication auth) {
        try { service.ezabatuOharra(koadernoa, ikasleaId, data, auth); return ResponseEntity.ok(Map.of("ok",true,"mezua","Oharra ezabatu da.")); }
        catch (SecurityException e) { return errorea(HttpStatus.FORBIDDEN, e); } catch (IllegalArgumentException e) { return errorea(HttpStatus.BAD_REQUEST, e); }
    }
    private ResponseEntity<?> errorea(HttpStatus status, RuntimeException e) { return ResponseEntity.status(status).body(Map.of("ok",false,"errorea",e.getMessage())); }
}
