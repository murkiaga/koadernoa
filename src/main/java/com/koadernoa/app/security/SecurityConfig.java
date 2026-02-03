package com.koadernoa.app.security;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;

import jakarta.annotation.PostConstruct;


@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final CustomOAuth2UserService customOAuth2UserService;
    private final AuthProviderEnabledFilter authProviderEnabledFilter;
    private final LdapAuthenticationProvider ldapAuthenticationProvider;
    private final LdapLoginSuccessHandler ldapLoginSuccessHandler;
    
    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService,
            AuthProviderEnabledFilter authProviderEnabledFilter,
            LdapAuthenticationProvider ldapAuthenticationProvider,
            LdapLoginSuccessHandler ldapLoginSuccessHandler) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.authProviderEnabledFilter = authProviderEnabledFilter;
        this.ldapAuthenticationProvider = ldapAuthenticationProvider;
        this.ldapLoginSuccessHandler = ldapLoginSuccessHandler;
    }
   
    
    @PostConstruct
    public void printMe() {
        System.out.println("✅ SecurityConfig kargatu da!");
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
    		.csrf(csrf -> csrf
    	            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    	            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()) //csrf atributua jartzen du
    	        )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/kudeatzaile/**").hasAnyRole("ADMIN", "KUDEATZAILEA")
                .requestMatchers("/irakasle", "/irakasle/**").hasAnyRole("ADMIN", "KUDEATZAILEA", "IRAKASLEA")
                .requestMatchers("/login", "/login/oauth2/**", "/oauth2/**", "/css/**", "/js/**", "/uploads/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth -> oauth
            	    .loginPage("/login")
            	    .userInfoEndpoint(userInfo -> userInfo
            	        .oidcUserService(customOAuth2UserService)
            	    )
            	    .defaultSuccessUrl("/aukeratu-mintegia", true)
            	    .failureUrl("/login?error")
            )
            .formLogin(form -> form
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .successHandler(ldapLoginSuccessHandler)
                    .failureHandler(ldapFailureHandler())
            )
            .logout(logout -> logout
                    .logoutUrl("/logout")                 // endpoint real de logout (POST)
                    .logoutSuccessUrl("/login?logout")    // a dónde se redirige después
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll()
             )
            .exceptionHandling(ex -> ex
            	    .authenticationEntryPoint((request, response, authException) -> {
            	        System.out.println("🚨 EXCEPTION: " + authException.getMessage());
            	        authException.printStackTrace();
            	        response.sendRedirect("/login");
            	    })
            	)
            .authenticationProvider(ldapAuthenticationProvider)
            .addFilterBefore(authProviderEnabledFilter, OAuth2AuthorizationRequestRedirectFilter.class)
            .build();
    }
    
    private AuthenticationFailureHandler ldapFailureHandler() {
        return (request, response, exception) -> {
            if (exception instanceof DisabledException) {
                response.sendRedirect("/login?disabled=ldap");
                return;
            }
            response.sendRedirect("/login?error");
        };
    }
}
