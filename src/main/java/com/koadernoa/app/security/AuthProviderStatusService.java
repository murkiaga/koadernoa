package com.koadernoa.app.security;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthProviderStatusService {

    private final Environment environment;
    private final AplikazioAukeraService aukService;

    public boolean isGoogleEnabled() {
        return aukService.getBool(AplikazioAukeraService.AUTH_GOOGLE_ENABLED, true);
    }

    public boolean isAdEnabled() {
        return aukService.getBool(AplikazioAukeraService.AUTH_AD_ENABLED, false);
    }

    public boolean isGoogleConfigured() {
        return hasText("spring.security.oauth2.client.registration.google.client-id")
                && hasText("spring.security.oauth2.client.registration.google.client-secret");
    }

    public boolean isAdConfigured() {
        return hasText("spring.security.oauth2.client.registration.ad.client-id")
                && hasText("spring.security.oauth2.client.registration.ad.client-secret")
                && hasText("spring.security.oauth2.client.provider.ad.issuer-uri");
    }

    public boolean isProviderAllowed(String registrationId) {
        if (!StringUtils.hasText(registrationId)) {
            return true;
        }
        String id = registrationId.toLowerCase();
        return switch (id) {
            case "google" -> isGoogleEnabled() && isGoogleConfigured();
            case "ad" -> isAdEnabled() && isAdConfigured();
            default -> true;
        };
    }

    private boolean hasText(String key) {
        return StringUtils.hasText(environment.getProperty(key));
    }
}
