package com.koadernoa.app.security;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

public class StatusAwareLdapAuthenticationProvider extends LdapAuthenticationProvider {

    private final AuthProviderStatusService statusService;

    public StatusAwareLdapAuthenticationProvider(LdapAuthenticator authenticator,
                                                 LdapAuthoritiesPopulator authoritiesPopulator,
                                                 AuthProviderStatusService statusService) {
        super(authenticator, authoritiesPopulator);
        this.statusService = statusService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!statusService.isLdapEnabled() || !statusService.isLdapConfigured()) {
            throw new DisabledException("LDAP ez dago aktibo edo konfiguratuta.");
        }
        return super.authenticate(authentication);
    }
}
