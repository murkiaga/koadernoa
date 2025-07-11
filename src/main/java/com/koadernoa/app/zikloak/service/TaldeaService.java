package com.koadernoa.app.zikloak.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.koadernoa.app.zikloak.entitateak.Taldea;
import com.koadernoa.app.zikloak.repository.TaldeaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaldeaService {
	private final TaldeaRepository taldeaRepository;

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
}
