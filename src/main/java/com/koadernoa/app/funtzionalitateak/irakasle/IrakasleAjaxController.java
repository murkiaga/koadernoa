package com.koadernoa.app.funtzionalitateak.irakasle;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle")
@RequiredArgsConstructor
public class IrakasleAjaxController {

    private final IrakasleaService irakasleaService;

    @GetMapping("/irakasle-aukera")
    @ResponseBody
    public List<Map<String, String>> irakasleAukerak(@RequestParam("q") String q) {
        return irakasleaService.bilatuAukerak(q).stream()
                .map(ira -> Map.of(
                        "id", ira.getId().toString(),
                        "label", buildLabel(ira),          // pantailarako testua
                        "value", pickValue(ira)            // inputean sartuko den balioa
                ))
                .toList();
    }

    private String buildLabel(Irakaslea ira) {
        String label = ira.getIzena();
        if (ira.getEmaila() != null && !ira.getEmaila().isBlank()) {
            label += " (" + ira.getEmaila() + ")";
        }
        return label;
    }

    private String pickValue(Irakaslea ira) {
        // ident gisa emaila nahi baduzu lehenetsi:
        if (ira.getEmaila() != null && !ira.getEmaila().isBlank()) {
            return ira.getEmaila();
        }
        return ira.getIzena();
    }
}
