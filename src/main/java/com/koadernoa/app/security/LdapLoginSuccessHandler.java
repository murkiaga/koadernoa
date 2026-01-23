package com.koadernoa.app.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LdapLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final IrakasleaRepository irakasleaRepository;

    @PostConstruct
    public void init() {
        setDefaultTargetUrl("/aukeratu-mintegia");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        String email = authentication.getName();
        irakasleaRepository.findByEmailaIgnoreCase(email).ifPresent(irakaslea -> {
            if (irakaslea.getMintegia() == null) {
                request.getSession(true).setAttribute("irakasleaId", irakaslea.getId());
            }
        });
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
