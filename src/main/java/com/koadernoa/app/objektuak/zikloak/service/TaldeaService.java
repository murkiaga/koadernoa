package com.koadernoa.app.objektuak.zikloak.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.modulua.repository.IkasleaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaldeaService {
	
	private final TaldeaRepository taldeaRepository;
	private final IrakasleaRepository irakasleaRepository;
	private final IkasleaRepository ikasleaRepository;

    public List<Taldea> getAll() {
        return taldeaRepository.findAll();
    }

    public Optional<Taldea> getById(Long id) {
        return taldeaRepository.findById(id);
    }

    public Taldea save(Taldea taldea) {
        return taldeaRepository.save(taldea);
    }

    public void deleteById(Long id) {
        taldeaRepository.deleteById(id);
    }
    
    public List<Taldea> getByZikloaId(Long zikloaId) {
        return taldeaRepository.findByZikloa_Id(zikloaId);
    }
    
    @Transactional
    public void eguneratuTutorea(Long taldeaId, Long irakasleIdEdoNull) {
        Taldea taldea = taldeaRepository.findById(taldeaId)
                .orElseThrow(() -> new IllegalArgumentException("Taldea ez da existitzen: " + taldeaId));

        // Kenduta utzi (irakasleIdEdoNull == null)
        if (irakasleIdEdoNull == null) {
            taldea.setTutorea(null);
            return;
        }

        Irakaslea irakaslea = irakasleaRepository.findById(irakasleIdEdoNull)
                .orElseThrow(() -> new IllegalArgumentException("Irakaslea ez da existitzen: " + irakasleIdEdoNull));

        // OneToOne koherentzia: irakasle hori beste talde bateko tutore bada, debekatu
        taldeaRepository.findByTutorea_Id(irakaslea.getId()).ifPresent(bestetalde -> {
            if (!bestetalde.getId().equals(taldeaId)) {
                throw new IllegalStateException("Irakasle hau jada " + bestetalde.getIzena() + " taldearen tutorea da.");
            }
        });

        taldea.setTutorea(irakaslea);
        //@Transactional dela eta, ez da save() beharrezkoa
    }
    
    public List<Taldea> getAllWithStudents() {
        return ikasleaRepository.findDistinctTaldeakWithStudents()
                .stream()
                .sorted(Comparator.comparing(Taldea::getIzena)) // aukeran: ordenatu izenez
                .toList();
    }
}
