package com.koadernoa.app.security;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthProviderEnabledFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_PREFIX = "/oauth2/authorization/";
    private static final String CALLBACK_PREFIX = "/login/oauth2/code/";

    private final AuthProviderStatusService statusService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String registrationId = extractRegistrationId(request.getRequestURI());
        if (registrationId != null && !statusService.isProviderAllowed(registrationId)) {
            response.sendRedirect("/login?disabled=" + registrationId);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String extractRegistrationId(String uri) {
        if (uri == null) {
            return null;
        }
        if (uri.startsWith(AUTHORIZATION_PREFIX)) {
            return trimPathSuffix(uri.substring(AUTHORIZATION_PREFIX.length()));
        }
        if (uri.startsWith(CALLBACK_PREFIX)) {
            return trimPathSuffix(uri.substring(CALLBACK_PREFIX.length()));
        }
        return null;
    }

    private String trimPathSuffix(String value) {
        int slash = value.indexOf('/');
        if (slash > -1) {
            return value.substring(0, slash);
        }
        return value;
    }
}
