package com.koadernoa.app.security;

import java.util.Collection;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.util.StringUtils;
import org.springframework.ldap.core.DirContextOperations;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaProvisioningService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LdapIrakasleaAuthoritiesPopulator implements LdapAuthoritiesPopulator {

    private final IrakasleaProvisioningService provisioningService;

    @Override
    public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations userData, String username) {
        String email = LdapUserAttributeHelper.resolveEmail(userData, username);
        if (!StringUtils.hasText(email)) {
            throw new AuthenticationServiceException("LDAP erabiltzaileak ez du email atributurik.");
        }

        Irakaslea irakaslea = provisioningService.bilatuEdoSortu(
                email, LdapUserAttributeHelper.resolveDisplayName(userData, email), "LDAP erabiltzailea");

        return IrakasleAuthorities.from(irakaslea);
    }

}
