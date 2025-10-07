package com.koadernoa.app.objektuak.irakasleak.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IrakasleaService {

	private final IrakasleaRepository irakasleaRepository;
	private final TaldeaRepository taldeaRepository;
	private final KoadernoaRepository koadernoaRepository;

    public Irakaslea findByEmaila(String emaila) {
        return irakasleaRepository.findByEmaila(emaila)
        		.orElseThrow(() -> new RuntimeException("Ez da aurkitu irakaslerik email honekin: " + emaila));
    }

    public Irakaslea findByIzena(String izena) {
        return irakasleaRepository.findByIzena(izena).orElseThrow();
    }
    
    public Irakaslea getLogeatutaDagoenIrakaslea(Authentication auth) {
        String emaila = null;
        if (auth.getPrincipal() instanceof OAuth2User oAuth2User) {
            emaila = oAuth2User.getAttribute("email");
        } else {
            emaila = auth.getName();
        }
        return findByEmaila(emaila);
    }
    
    
// TUTORE ATALA --------------------------------------------------------
    public Taldea lortuTutorearenTaldeaEdoThrow(Long irakasleId) {
        return taldeaRepository.findByTutorea_Id(irakasleId)
                .orElseThrow(() -> new IllegalStateException("Irakasle honek ez du tutoretzako talderik"));
    }

    public List<Koadernoa> lortuTutorearenKoadernoak(Long irakasleId) {
        return koadernoaRepository.findByModuloa_Taldea_Tutorea_Id(irakasleId);
    }
 // FIN TUTORE ATALA --------------------------------------------------------
    
}
