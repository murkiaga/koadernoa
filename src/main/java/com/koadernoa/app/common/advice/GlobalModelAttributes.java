package com.koadernoa.app.common.advice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

	private final AplikazioAukeraService aukService;
	
    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }
    
    @ModelAttribute("appLogoUrl")
    public String appLogoUrl() {
      return aukService.get(AplikazioAukeraService.APP_LOGO_URL, "");
    }
}