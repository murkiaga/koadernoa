package com.koadernoa.app.security;


import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.List;

import com.koadernoa.app.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.irakasleak.entitateak.Rola;
import com.koadernoa.app.irakasleak.repository.IrakasleaRepository;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final IrakasleaRepository irakasleaRepository;
    private final HttpSession httpSession;
    
    @PostConstruct
    public void printMe() {
        System.out.println("✅ CustomOAuth2UserService kargatu da!");
    }
    
    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUserService delegate = new OidcUserService();
        OidcUser oidcUser = delegate.loadUser(userRequest);

        String email = oidcUser.getEmail();
        log.info("➡️ Google login jasota. Email: {}", email);

        if (email == null) {
            log.error("❌ EMAIL NULL DA! Attributuak: {}", oidcUser.getAttributes());
            throw new OAuth2AuthenticationException("Email ez da bueltatu Google-tik.");
        }

        // Irakaslea bilatu edo sortu
        Irakaslea irakaslea = irakasleaRepository.findByEmaila(email)
            .orElseGet(() -> {
                Irakaslea berria = new Irakaslea();
                berria.setEmaila(email);
                berria.setIzena(oidcUser.getAttribute("name"));
                berria.setKontu_mota("Google erabiltzailea");
                berria.setRola(Rola.IRAKASLEA);
                return irakasleaRepository.save(berria);
            });

        //Gorde irakasleaId sessionean mintegia falta bada
        if (irakaslea.getMintegia() == null) {
            httpSession.setAttribute("irakasleaId", irakaslea.getId());
        }

        //Autoritatea sortu (Spring Security-k "ROLE_" + rola izena espero du)
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + irakaslea.getRola().name());

        //Autoritateak eta atributuak erabiliz OidcUser berria sortu
        OidcUser user = new DefaultOidcUser(
            List.of(authority),
            oidcUser.getIdToken(),
            oidcUser.getUserInfo()
        );

        return user;
    }
}

