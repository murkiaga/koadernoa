package com.koadernoa.app.security.audit;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.koadernoa.app.objektuak.audit.entitateak.AuditAtala;
import com.koadernoa.app.objektuak.audit.entitateak.AuditEvent;
import com.koadernoa.app.objektuak.audit.service.AuditService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.IrakasleUserDetails;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PageViewAuditInterceptor implements HandlerInterceptor {

    private final AuditService auditService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return;
        }

        int status = response.getStatus();
        if (status < 200 || status >= 400) {
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return;
        }

        AuditAtala atala = mapAtala(request.getRequestURI());
        if (atala == null) {
            return;
        }

        PrincipalInfo info = resolvePrincipalInfo(auth);
        AuditEvent event = auditService.buildBaseEvent(
            info.erabiltzaileId,
            info.emaila,
            info.izena,
            info.rola,
            request.getRequestURI(),
            request.getMethod(),
            getClientIp(request),
            request.getHeader("User-Agent"),
            null,
            atala,
            null
        );

        auditService.recordPageView(event);
    }

    private AuditAtala mapAtala(String uri) {
        if (uri == null) {
            return null;
        }
        if (uri.equals("/irakasle")) return AuditAtala.IRAKASLE;
        if (uri.equals("/irakasle/programazioa")) return AuditAtala.PROGRAMAZIOA;
        if (uri.equals("/irakasle/denboralizazioa")) return AuditAtala.DENBORALIZAZIOA;
        if (uri.equals("/irakasle/notak")) return AuditAtala.NOTAK;
        if (uri.equals("/irakasle/estatistikak")) return AuditAtala.ESTATISTIKAK;
        if (uri.startsWith("/kudeatzaile/")) return AuditAtala.KUDEATZAILE;
        if (uri.equals("/kudeatzaile")) return AuditAtala.KUDEATZAILE;
        if (uri.startsWith("/admin/")) return AuditAtala.ADMIN;
        if (uri.equals("/admin")) return AuditAtala.ADMIN;
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private PrincipalInfo resolvePrincipalInfo(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof IrakasleUserDetails iu) {
            String rola = iu.getIrakaslea().getRola() != null ? iu.getIrakaslea().getRola().name() : null;
            return new PrincipalInfo(iu.getIrakaslea().getId(), iu.getIrakaslea().getEmaila(), iu.getIrakaslea().getIzena(), rola);
        }

        if (principal instanceof OidcUser oidcUser) {
            return new PrincipalInfo(null, firstNonBlank(oidcUser.getEmail(), oidcUser.getPreferredUsername()),
                firstNonBlank(oidcUser.getFullName(), oidcUser.getName()), null);
        }

        if (principal instanceof OAuth2User oauth2User) {
            Object email = oauth2User.getAttributes().get("email");
            Object name = oauth2User.getAttributes().get("name");
            return new PrincipalInfo(null, firstNonBlank((String) email, oauth2User.getName()), firstNonBlank((String) name, oauth2User.getName()), null);
        }

        if (principal instanceof UserDetails ud) {
            return new PrincipalInfo(null, ud.getUsername(), ud.getUsername(), null);
        }

        return new PrincipalInfo(null, authentication.getName(), authentication.getName(), null);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        return (second != null && !second.isBlank()) ? second : null;
    }

    private record PrincipalInfo(Long erabiltzaileId, String emaila, String izena, String rola) {}
}
