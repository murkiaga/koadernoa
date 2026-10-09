package com.koadernoa.app.security;

import java.util.List;

import javax.naming.directory.SearchControls;

import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.LikeFilter;
import org.springframework.ldap.filter.OrFilter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActiveDirectoryBilaketaService {

    private static final int GEHIENEKO_EMAITZAK = 20;
    private static final String[] ATRIBUTUAK = {
            "sAMAccountName", "userPrincipalName", "displayName", "givenName", "sn", "mail"
    };

    private final LdapTemplate ldapTemplate;
    private final LdapSettings settings;

    public List<AdErabiltzailea> bilatu(String terminoa) {
        if (!StringUtils.hasText(terminoa) || terminoa.trim().length() < 2) {
            throw new IllegalArgumentException("Idatzi gutxienez 2 karaktere bilatzeko.");
        }
        String balioa = terminoa.trim();
        OrFilter eremuak = new OrFilter()
                .or(new LikeFilter("sAMAccountName", "*" + balioa + "*"))
                .or(new LikeFilter("userPrincipalName", "*" + balioa + "*"))
                .or(new LikeFilter("displayName", "*" + balioa + "*"));
        AndFilter filter = new AndFilter()
                .and(new EqualsFilter("objectCategory", "person"))
                .and(new EqualsFilter("objectClass", "user"))
                .and(eremuak);
        return search(filter.encode());
    }

    public AdErabiltzailea bilatuZehatza(String identifikatzailea) {
        if (!StringUtils.hasText(identifikatzailea)) {
            return null;
        }
        String balioa = identifikatzailea.trim();
        OrFilter ident = new OrFilter()
                .or(new EqualsFilter("sAMAccountName", balioa))
                .or(new EqualsFilter("userPrincipalName", balioa));
        AndFilter filter = new AndFilter()
                .and(new EqualsFilter("objectCategory", "person"))
                .and(new EqualsFilter("objectClass", "user"))
                .and(ident);
        List<AdErabiltzailea> emaitzak = search(filter.encode());
        return emaitzak.size() == 1 ? emaitzak.get(0) : null;
    }

    private List<AdErabiltzailea> search(String filter) {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(settings.isUserSearchSubtree()
                ? SearchControls.SUBTREE_SCOPE : SearchControls.ONELEVEL_SCOPE);
        controls.setCountLimit(GEHIENEKO_EMAITZAK);
        controls.setReturningAttributes(ATRIBUTUAK);
        return ldapTemplate.search(settings.getUserSearchBase(), filter, controls,
                (AttributesMapper<AdErabiltzailea>) attrs -> {
                    String sam = attr(attrs, "sAMAccountName");
                    String upn = attr(attrs, "userPrincipalName");
                    String izena = attr(attrs, "givenName");
                    String abizenak = attr(attrs, "sn");
                    String osoa = firstNonBlank(attr(attrs, "displayName"), batu(izena, abizenak), upn, sam);
                    String emaila = firstNonBlank(attr(attrs, "mail"), upn);
                    return new AdErabiltzailea(firstNonBlank(sam, upn), sam, upn, izena, abizenak, osoa, emaila);
                });
    }

    private static String attr(javax.naming.directory.Attributes attrs, String name) throws javax.naming.NamingException {
        return attrs.get(name) == null ? null : String.valueOf(attrs.get(name).get());
    }

    private static String batu(String lehena, String bigarrena) {
        return StringUtils.hasText(lehena) && StringUtils.hasText(bigarrena) ? lehena + " " + bigarrena
                : firstNonBlank(lehena, bigarrena);
    }

    private static String firstNonBlank(String... balioak) {
        for (String balioa : balioak) if (StringUtils.hasText(balioa)) return balioa;
        return null;
    }
}
