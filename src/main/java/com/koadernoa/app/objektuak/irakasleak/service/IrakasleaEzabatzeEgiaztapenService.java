package com.koadernoa.app.objektuak.irakasleak.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.jokabidea.repository.IkasleEgunOharraRepository;
import com.koadernoa.app.objektuak.jokabidea.repository.JokabideDesegokiaRepository;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.ProgramazioTxantiloiRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IrakasleaEzabatzeEgiaztapenService {

    private final KoadernoaRepository koadernoaRepository;
    private final TaldeaRepository taldeaRepository;
    private final IrakasleaRepository irakasleaRepository;
    private final ProgramazioTxantiloiRepository programazioTxantiloiRepository;
    private final JokabideDesegokiaRepository jokabideDesegokiaRepository;
    private final IkasleEgunOharraRepository ikasleEgunOharraRepository;

    @Transactional(readOnly = true)
    public Optional<String> ezabatzeErrorea(Irakaslea irakaslea) {
        Long id = irakaslea.getId();
        // Jabe gisa zein partekatutako irakasle gisa lotutako koadernoak, ikasturte guztietan.
        List<Koadernoa> koadernoak = koadernoaRepository.findIrakaslearenKoadernoak(id, irakaslea);
        if (!koadernoak.isEmpty()) {
            Koadernoa koadernoa = koadernoak.get(0);
            String mezua = "Ezin izan da irakaslea ezabatu, koadernoren batekin lotuta dagoelako.";
            if (koadernoa.getEgutegia() != null && koadernoa.getEgutegia().getIkasturtea() != null) {
                String ikasturtea = koadernoa.getEgutegia().getIkasturtea().getIzena();
                if (ikasturtea != null && !ikasturtea.isBlank()) {
                    mezua += " Ikasturtea: " + ikasturtea + ".";
                }
            }
            return Optional.of(mezua);
        }
        Optional<Taldea> taldea = taldeaRepository.findByTutorea_Id(id);
        if (taldea.isPresent()) {
            return Optional.of("Ezin izan da irakaslea ezabatu, " + taldea.get().getIzena()
                    + " taldeko tutorea delako.");
        }
        if (irakasleaRepository.existsByOrdezkoa_Id(id)) {
            return Optional.of("Ezin izan da irakaslea ezabatu, beste irakasle baten ordezko gisa esleituta dagoelako.");
        }
        if (programazioTxantiloiRepository.existsByIrakaslea_Id(id)) {
            return Optional.of("Ezin izan da irakaslea ezabatu, programazio-txantiloiren batekin lotuta dagoelako.");
        }
        if (jokabideDesegokiaRepository.existsByIrakaslea_IdOrJasotaNork_Id(id, id)) {
            return Optional.of("Ezin izan da irakaslea ezabatu, jokabide desegokien erregistroren batekin lotuta dagoelako.");
        }
        if (ikasleEgunOharraRepository.existsByIrakaslea_Id(id)) {
            return Optional.of("Ezin izan da irakaslea ezabatu, ikasleren baten eguneko ohar batekin lotuta dagoelako.");
        }
        return Optional.empty();
    }
}
