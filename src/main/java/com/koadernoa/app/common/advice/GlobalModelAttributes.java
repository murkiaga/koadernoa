package com.koadernoa.app.common.advice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.security.core.Authentication;

import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.mezuak.service.MezuaService;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

	private final AplikazioAukeraService aukService;
	private final IrakasleaService irakasleaService;
	private final MezuaService mezuaService;
	
    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }
    
    @ModelAttribute("appLogoUrl")
    public String appLogoUrl() {
      return aukService.get(AplikazioAukeraService.APP_LOGO_URL, "");
    }

    @ModelAttribute("irakurriGabekoMezuKop")
    public long irakurriGabekoMezuKop(Authentication auth) {
        if (auth == null) return 0L;
        try {
            var ir = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
            if (ir == null || ir.getId() == null) return 0L;
            return mezuaService.irakurriGabekoKopurua(ir.getId());
        } catch (Exception ex) {
            return 0L;
        }
    }
}
