package com.koadernoa.app.modulua.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.koadernoa.app.egutegia.entitateak.Maila;
import com.koadernoa.app.egutegia.repository.MailaRepository;
import com.koadernoa.app.modulua.entitateak.Moduloa;
import com.koadernoa.app.modulua.entitateak.ModuloaFormDto;
import com.koadernoa.app.modulua.repository.ModuloaRepository;
import com.koadernoa.app.zikloak.entitateak.Taldea;
import com.koadernoa.app.zikloak.repository.TaldeaRepository;
import com.koadernoa.app.zikloak.service.TaldeaService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModuloaService {

    private final ModuloaRepository moduloaRepository;
    private final MailaRepository mailaRepository;
    
    private final TaldeaService taldeaService;

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
    
    @Transactional
    public Moduloa saveFromDto(ModuloaFormDto dto) {
        // Resolve erlazioak
        Maila maila = mailaRepository.findById(dto.getMailaId())
            .orElseThrow(() -> new IllegalArgumentException("Maila ez da existitzen: " + dto.getMailaId()));
        Taldea taldea = taldeaService.getById(dto.getTaldeaId())
            .orElseThrow(() -> new IllegalArgumentException("Taldea ez da existitzen: " + dto.getTaldeaId()));

        // Edit vs create
        Moduloa target = (dto.getId() != null)
            ? moduloaRepository.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Moduloa ez da existitzen: " + dto.getId()))
            : new Moduloa();

        // Set eremuak
        target.setIzena(dto.getIzena().trim());
        target.setKodea(dto.getKodea().trim());
        target.setMaila(maila);
        target.setTaldea(taldea);

        return moduloaRepository.save(target);
    }

}