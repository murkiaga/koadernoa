package com.koadernoa.app.security.audit;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.LogoutSuccessEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.koadernoa.app.objektuak.audit.entitateak.AuditEvent;
import com.koadernoa.app.objektuak.audit.service.AuditService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.IrakasleUserDetails;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SecurityAuditEventListener {

    private final AuditService auditService;

    @EventListener
    public void onLoginSuccess(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        PrincipalInfo info = resolvePrincipalInfo(auth);

        AuditEvent auditEvent = auditService.buildBaseEvent(
            info.erabiltzaileId,
            info.emaila,
            info.izena,
            info.rolak,
            getRequestUrl(),
            getHttpMethod(),
            getClientIp(),
            getUserAgent(),
            "Login arrakastatsua",
            null,
            null
        );

        auditService.recordLoginOk(auditEvent);
    }

    @EventListener
    public void onLoginFailure(AuthenticationFailureBadCredentialsEvent event) {
        Authentication auth = event.getAuthentication();
        PrincipalInfo info = resolvePrincipalInfo(auth);

        AuditEvent auditEvent = auditService.buildBaseEvent(
            null,
            firstNonBlank(info.emaila, auth != null ? auth.getName() : null),
            info.izena,
            info.rolak,
            getRequestUrl(),
            getHttpMethod(),
            getClientIp(),
            getUserAgent(),
            "Login saiakera okerra",
            null,
            null
        );

        auditService.recordLoginFail(auditEvent);
    }

    @EventListener
    public void onLogoutSuccess(LogoutSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        PrincipalInfo info = resolvePrincipalInfo(auth);

        AuditEvent auditEvent = auditService.buildBaseEvent(
            info.erabiltzaileId,
            info.emaila,
            info.izena,
            info.rolak,
            getRequestUrl(),
            getHttpMethod(),
            getClientIp(),
            getUserAgent(),
            "Logout",
            null,
            null
        );

        auditService.recordLogout(auditEvent);
    }

    private PrincipalInfo resolvePrincipalInfo(Authentication authentication) {
        if (authentication == null) {
            return new PrincipalInfo(null, null, null, null);
        }

        Object principal = authentication.getPrincipal();
        String roleNames = joinRoles(authentication.getAuthorities());

        if (principal instanceof IrakasleUserDetails iu) {
            return new PrincipalInfo(
                iu.getIrakaslea().getId(),
                iu.getIrakaslea().getEmaila(),
                iu.getIrakaslea().getIzena(),
                roleNames
            );
        }

        if (principal instanceof UserDetails ud) {
            return new PrincipalInfo(null, ud.getUsername(), ud.getUsername(), roleNames);
        }

        if (principal instanceof OidcUser oidcUser) {
            return new PrincipalInfo(
                null,
                firstNonBlank(oidcUser.getEmail(), oidcUser.getPreferredUsername()),
                firstNonBlank(oidcUser.getFullName(), oidcUser.getName()),
                roleNames
            );
        }

        if (principal instanceof OAuth2User oauth2User) {
            Map<String, Object> attrs = oauth2User.getAttributes();
            return new PrincipalInfo(
                null,
                firstNonBlank((String) attrs.get("email"), oauth2User.getName()),
                firstNonBlank((String) attrs.get("name"), oauth2User.getName()),
                roleNames
            );
        }

        return new PrincipalInfo(null, authentication.getName(), authentication.getName(), roleNames);
    }

    private String joinRoles(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            return null;
        }
        return authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));
    }

    private String getRequestUrl() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getRequestURI() : null;
    }

    private String getHttpMethod() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getMethod() : null;
    }

    private String getUserAgent() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getHeader("User-Agent") : null;
    }

    private String getClientIp() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private HttpServletRequest getCurrentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        return attrs.getRequest();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return (second != null && !second.isBlank()) ? second : null;
    }

    private record PrincipalInfo(Long erabiltzaileId, String emaila, String izena, String rolak) {}
}
