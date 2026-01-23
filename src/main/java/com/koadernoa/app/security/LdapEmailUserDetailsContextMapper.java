package com.koadernoa.app.security;

import java.util.Collection;

import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.util.StringUtils;

public class LdapEmailUserDetailsContextMapper implements UserDetailsContextMapper {

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx,
                                          String username,
                                          Collection<? extends GrantedAuthority> authorities) {
        String email = LdapUserAttributeHelper.resolveEmail(ctx, username);
        String principal = StringUtils.hasText(email) ? email : username;

        LdapUserDetailsImpl.Essence essence = new LdapUserDetailsImpl.Essence();
        essence.setUsername(principal);
        essence.setDn(ctx.getDn().toString());
        essence.setAuthorities(authorities);

        String password = ctx.getStringAttribute("userPassword");
        essence.setPassword(StringUtils.hasText(password) ? password : "N/A");

        return essence.createUserDetails();
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        throw new UnsupportedOperationException("LDAP user context mapping is not supported.");
    }
}
