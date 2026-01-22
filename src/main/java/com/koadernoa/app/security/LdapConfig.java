package com.koadernoa.app.security;

import org.springframework.boot.autoconfigure.ldap.LdapProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(LdapSettings.class)
public class LdapConfig {

    @Bean
    public LdapContextSource ldapContextSource(LdapProperties properties) {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrls(properties.getUrls().toArray(String[]::new));
        contextSource.setBase(properties.getBase());
        contextSource.setUserDn(properties.getUsername());
        contextSource.setPassword(properties.getPassword());
        return contextSource;
    }

    @Bean
    public LdapAuthenticationProvider ldapAuthenticationProvider(LdapContextSource contextSource,
                                                                 LdapSettings settings,
                                                                 AuthProviderStatusService statusService) {
        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        if (StringUtils.hasText(settings.getUserDnPattern())) {
            authenticator.setUserDnPatterns(new String[] { settings.getUserDnPattern() });
        } else if (StringUtils.hasText(settings.getUserSearchBase())
                && StringUtils.hasText(settings.getUserSearchFilter())) {
            FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(
                    settings.getUserSearchBase(),
                    settings.getUserSearchFilter(),
                    contextSource
            );
            authenticator.setUserSearch(userSearch);
        }

        DefaultLdapAuthoritiesPopulator authoritiesPopulator =
                new DefaultLdapAuthoritiesPopulator(contextSource, settings.getGroupSearchBase());
        if (StringUtils.hasText(settings.getGroupSearchFilter())) {
            authoritiesPopulator.setGroupSearchFilter(settings.getGroupSearchFilter());
        }

        return new StatusAwareLdapAuthenticationProvider(authenticator, authoritiesPopulator, statusService);
    }
}
