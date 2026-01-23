package com.koadernoa.app.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "koadernoa.ldap")
public class LdapSettings {

    private String userDnPattern;
    private String userSearchBase;
    private String userSearchFilter;
    private String groupSearchBase = "";
    private String groupSearchFilter;
}
