package com.koadernoa.app.objektuak.irakasleak.entitateak;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;
import lombok.Setter;
import com.koadernoa.app.security.IrakasleAuthorities;

@Getter
@Setter
public class IrakasleUserDetails implements UserDetails {

    private final Irakaslea irakaslea;

    public IrakasleUserDetails(Irakaslea irakaslea) {
        this.irakaslea = irakaslea;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return IrakasleAuthorities.from(irakaslea);
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
