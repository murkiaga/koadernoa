package com.koadernoa.app.egutegia.service;

import com.koadernoa.app.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.egutegia.entitateak.Maila;
import com.koadernoa.app.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.zikloak.repository.TaldeaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.koadernoa.app.egutegia.entitateak.Astegunak;
import com.koadernoa.app.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.egutegia.entitateak.EgunMota;

@Service
@RequiredArgsConstructor
public class IkasturteaService {

	
	private final IkasturteaRepository ikasturteaRepository;
	
	public Ikasturtea getIkasturteAktiboa() {
		List<Ikasturtea> aktiboak = ikasturteaRepository.findByAktiboaTrue();
	    if (aktiboak.isEmpty()) return null;
	    return aktiboak.get(0); // Lehena
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
