package com.koadernoa.app.objektuak.egutegia.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;

@Service
@RequiredArgsConstructor
public class IkasturteaService {

	
	private final IkasturteaRepository ikasturteaRepository;
	
	public Ikasturtea getIkasturteAktiboa() {
	    return ikasturteaRepository.findByAktiboaTrue()
	        .orElseThrow(() -> new IllegalStateException("Ez dago ikasturte aktiborik"));
	}

    @Transactional
    public void gordeIkasturtea(Ikasturtea ikasturtea) {
        ikasturteaRepository.findAll().forEach(i -> {
            i.setAktiboa(false);
            ikasturteaRepository.save(i);
        });
        ikasturteaRepository.save(ikasturtea);
    }

    public Optional<Ikasturtea> getAktiboa() {
        return ikasturteaRepository.findByAktiboaTrue().stream().findFirst();
    }
    
    public Ikasturtea getById(Long id) {
        return ikasturteaRepository.findById(id).orElse(null);
    }
    
    public List<Ikasturtea> getAll() {
        return ikasturteaRepository.findAll();
    }

    public List<Ikasturtea> getAllOrderedDesc() {
        return ikasturteaRepository.findAllByOrderByIzenaDesc();
    }
    
    public Ikasturtea save(Ikasturtea ikasturtea) {
        return ikasturteaRepository.save(ikasturtea);
    }
    
    @Transactional
    public void desaktibatuDenak() {
        ikasturteaRepository.findAll().forEach(i -> {
            if (i.isAktiboa()) {
                i.setAktiboa(false);
                ikasturteaRepository.save(i);
            }
        });
    }
	
	
}
