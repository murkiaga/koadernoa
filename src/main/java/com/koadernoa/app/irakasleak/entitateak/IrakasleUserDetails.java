package com.koadernoa.app.irakasleak.entitateak;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class IrakasleUserDetails implements UserDetails {

    private final Irakaslea irakaslea;

    public IrakasleUserDetails(Irakaslea irakaslea) {
        this.irakaslea = irakaslea;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + irakaslea.getRola().name()));
    }

    @Override
    public String getPassword() {
        return irakaslea.getPasahitza();
    }

    @Override
    public String getUsername() {
        return irakaslea.getIzena();
    }

    // Besteak beti true:
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
