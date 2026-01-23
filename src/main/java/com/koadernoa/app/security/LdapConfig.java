package com.koadernoa.app.security;

import org.springframework.boot.autoconfigure.ldap.LdapProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.util.StringUtils;

import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;

@Configuration
@EnableConfigurationProperties(LdapSettings.class)
public class LdapConfig {

    @Bean
    public LdapContextSource ldapContextSource(LdapProperties properties) {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrls(properties.getUrls());
        contextSource.setBase(properties.getBase());
        contextSource.setUserDn(properties.getUsername());
        contextSource.setPassword(properties.getPassword());
        return contextSource;
    }

    @Bean
    public LdapAuthenticationProvider ldapAuthenticationProvider(LdapContextSource contextSource,
                                                                 LdapSettings settings,
                                                                 AuthProviderStatusService statusService,
                                                                 IrakasleaRepository irakasleaRepository) {
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
            userSearch.setSearchSubtree(true);
            authenticator.setUserSearch(userSearch);
        }

        LdapIrakasleaAuthoritiesPopulator authoritiesPopulator =
                new LdapIrakasleaAuthoritiesPopulator(irakasleaRepository);

        LdapAuthenticationProvider provider =
                new StatusAwareLdapAuthenticationProvider(authenticator, authoritiesPopulator, statusService);
        provider.setUserDetailsContextMapper(new LdapEmailUserDetailsContextMapper());
        return provider;
    }
}
