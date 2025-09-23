package com.koadernoa.app.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public final class SecurityUtils {
    private SecurityUtils() {}

    public static boolean hasAnyRole(Authentication auth, String... roles) {
        if (auth == null) return false;
        for (String role : roles) {
            String expected = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            for (GrantedAuthority a : auth.getAuthorities()) {
                if (expected.equals(a.getAuthority())) return true;
            }
        }
        return false;
    }

    /** KUDEATZAILEA edo ADMIN bada, egia. */
    public static boolean isKudeatzailea(Authentication auth) {
        return hasAnyRole(auth, "KUDEATZAILEA", "ADMIN");
    }
}

