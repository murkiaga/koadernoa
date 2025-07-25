package com.koadernoa.app.irakasleak.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.koadernoa.app.irakasleak.entitateak.IrakasleUserDetails;
import com.koadernoa.app.irakasleak.repository.IrakasleaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IrakasleUserDetailsService implements UserDetailsService {

    private final IrakasleaRepository irakasleaRepository;

    @Override
    public UserDetails loadUserByUsername(String erabiltzaileIzena) throws UsernameNotFoundException {
        return irakasleaRepository.findByIzena(erabiltzaileIzena)
            .map(IrakasleUserDetails::new)
            .orElseThrow(() -> new UsernameNotFoundException("Ez da aurkitu: " + erabiltzaileIzena));
    }
}