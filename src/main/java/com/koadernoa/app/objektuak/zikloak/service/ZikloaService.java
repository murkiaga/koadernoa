package com.koadernoa.app.objektuak.zikloak.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;
import com.koadernoa.app.objektuak.zikloak.repository.ZikloaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ZikloaService {

    private final ZikloaRepository zikloaRepository;

    public List<Zikloa> getAll() {
        return zikloaRepository.findAll();
    }

    public Optional<Zikloa> getById(Long id) {
        return zikloaRepository.findById(id);
    }

    public void save(Zikloa zikloa) {
    	zikloaRepository.save(zikloa);
    }

    public Zikloa update(Long id, Zikloa zikloaEguneratua) {
        return zikloaRepository.findById(id)
            .map(zikloa -> {
                zikloa.setIzena(zikloaEguneratua.getIzena());
                zikloa.setMaila(zikloaEguneratua.getMaila());
                return zikloaRepository.save(zikloa);
            })
            .orElseThrow(() -> new NoSuchElementException("Ez da aurkitu zikloa id: " + id));
    }

    public void delete(Long id) {
        zikloaRepository.deleteById(id);
    }
}

