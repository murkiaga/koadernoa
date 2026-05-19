package com.koadernoa.app.common.web;

import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class FaviconController {

    private final AplikazioAukeraService aukService;

    @GetMapping("/favicon.ico")
    public String favicon() {
        String faviconUrl = aukService.get(AplikazioAukeraService.APP_FAVICON_URL, "");
        if (faviconUrl != null && !faviconUrl.isBlank()) {
            return "redirect:" + faviconUrl;
        }
        return "redirect:/uploads/logoa/logo.png";
    }
}
