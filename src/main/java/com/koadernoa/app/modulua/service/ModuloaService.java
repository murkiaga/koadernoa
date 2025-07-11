package com.koadernoa.app.modulua.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.koadernoa.app.modulua.entitateak.Moduloa;
import com.koadernoa.app.modulua.repository.ModuloaRepository;

@Service
public class ModuloaService {

    @Autowired
    private ModuloaRepository moduloaRepository;

    public List<Moduloa> getAll() {
        return moduloaRepository.findAll();
    }

    public Optional<Moduloa> getById(Long id) {
        return moduloaRepository.findById(id);
    }

    public void save(Moduloa moduloa) {
        moduloaRepository.save(moduloa);
    }

    public void delete(Long id) {
        moduloaRepository.deleteById(id);
    }
    
    public List<Moduloa> getByTaldeaId(Long taldeaId) {
        return moduloaRepository.findByTaldeaId(taldeaId);
    }

}