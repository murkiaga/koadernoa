package com.koadernoa.app.irakasleak.service;

import org.springframework.stereotype.Service;

import com.koadernoa.app.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.irakasleak.repository.IrakasleaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IrakasleaService {

	private final IrakasleaRepository irakasleaRepository;

    public Irakaslea findByEmaila(String emaila) {
        return irakasleaRepository.findByEmaila(emaila)
        		.orElseThrow(() -> new RuntimeException("Ez da aurkitu irakaslerik email honekin: " + emaila));
    }

    public Irakaslea findByIzena(String izena) {
        return irakasleaRepository.findByIzena(izena).orElseThrow();
    }
    
    
}
