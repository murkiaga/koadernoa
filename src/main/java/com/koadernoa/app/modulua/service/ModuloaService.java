package com.koadernoa.app.modulua.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.koadernoa.app.egutegia.entitateak.Maila;
import com.koadernoa.app.egutegia.repository.MailaRepository;
import com.koadernoa.app.modulua.entitateak.Moduloa;
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
    public Moduloa gordeModuloaKontrolekin(Moduloa incoming) {
        // 1) Resolve Maila
        Long mailaId = incoming.getMaila() != null ? incoming.getMaila().getId() : null;
        if (mailaId == null) throw new IllegalArgumentException("Maila falta da");
        Maila maila = mailaRepository.findById(mailaId)
                .orElseThrow(() -> new IllegalArgumentException("Maila ez da existitzen: " + mailaId));

        // 2) Resolve Taldea
        Long taldeaId = incoming.getTaldea() != null ? incoming.getTaldea().getId() : null;
        if (taldeaId == null) throw new IllegalArgumentException("Taldea falta da");
        Taldea taldea = taldeaService.getById(taldeaId)
                .orElseThrow(() -> new IllegalArgumentException("Taldea ez da existitzen: " + taldeaId));

        // 3) Edit vs Create: beti entitate kudeatua gorde
        Moduloa target;
        if (incoming.getId() != null) {
            target = moduloaRepository.findById(incoming.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Moduloa ez da existitzen: " + incoming.getId()));
        } else {
            target = new Moduloa();
        }

        // 4) Eremuak kopiatu
        target.setIzena(incoming.getIzena());
        target.setKodea(incoming.getKodea());
        target.setMaila(maila);
        target.setTaldea(taldea);

        return moduloaRepository.save(target);
    }

}