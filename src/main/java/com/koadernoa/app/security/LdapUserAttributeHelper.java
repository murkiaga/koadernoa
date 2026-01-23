package com.koadernoa.app.security;

import org.springframework.ldap.core.DirContextOperations;
import org.springframework.util.StringUtils;

final class LdapUserAttributeHelper {

    private LdapUserAttributeHelper() {
    }

    static String resolveEmail(DirContextOperations ctx, String fallback) {
        String email = firstNonBlank(
                ctx.getStringAttribute("mail"),
                ctx.getStringAttribute("userPrincipalName"),
                ctx.getStringAttribute("email")
        );
        if (!StringUtils.hasText(email) && StringUtils.hasText(fallback) && fallback.contains("@")) {
            email = fallback;
        }
        return email;
    }

    static String resolveDisplayName(DirContextOperations ctx, String fallback) {
        String displayName = firstNonBlank(
                ctx.getStringAttribute("displayName"),
                ctx.getStringAttribute("cn")
        );
        if (!StringUtils.hasText(displayName)) {
            String givenName = ctx.getStringAttribute("givenName");
            String surname = ctx.getStringAttribute("sn");
            if (StringUtils.hasText(givenName) && StringUtils.hasText(surname)) {
                displayName = givenName + " " + surname;
            } else {
                displayName = firstNonBlank(givenName, surname);
            }
        }
        if (!StringUtils.hasText(displayName)) {
            displayName = fallback;
        }
        return displayName;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
