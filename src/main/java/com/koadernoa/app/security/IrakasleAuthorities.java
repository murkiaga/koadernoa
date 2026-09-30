package com.koadernoa.app.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;

public final class IrakasleAuthorities {

    private IrakasleAuthorities() {
    }

    public static Collection<? extends GrantedAuthority> from(Irakaslea irakaslea) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (irakaslea.getRola() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + irakaslea.getRola().name()));
        }
        if (irakaslea.getBaimenak() != null) {
            irakaslea.getBaimenak().stream()
                    .map(baimena -> new SimpleGrantedAuthority(baimena.getAuthority()))
                    .forEach(authorities::add);
        }
        return List.copyOf(authorities);
    }
}
