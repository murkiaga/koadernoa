package com.koadernoa.app.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.util.StringUtils;
import org.springframework.ldap.core.DirContextOperations;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Rola;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LdapIrakasleaAuthoritiesPopulator implements LdapAuthoritiesPopulator {

    private final IrakasleaRepository irakasleaRepository;

    @Override
    public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations userData, String username) {
        String email = LdapUserAttributeHelper.resolveEmail(userData, username);
        if (!StringUtils.hasText(email)) {
            throw new AuthenticationServiceException("LDAP erabiltzaileak ez du email atributurik.");
        }

        Irakaslea irakaslea = irakasleaRepository.findByEmailaIgnoreCase(email)
                .orElseGet(() -> sortuIrakaslea(userData, email));

        if (irakaslea.getRola() == null) {
            irakaslea.setRola(Rola.IRAKASLEA);
            irakasleaRepository.save(irakaslea);
        }

        return List.of(new SimpleGrantedAuthority("ROLE_" + irakaslea.getRola().name()));
    }

    private Irakaslea sortuIrakaslea(DirContextOperations userData, String email) {
        Irakaslea berria = new Irakaslea();
        berria.setEmaila(email);
        berria.setIzena(LdapUserAttributeHelper.resolveDisplayName(userData, email));
        berria.setKontu_mota("LDAP erabiltzailea");
        berria.setRola(Rola.IRAKASLEA);
        return irakasleaRepository.save(berria);
    }
}
