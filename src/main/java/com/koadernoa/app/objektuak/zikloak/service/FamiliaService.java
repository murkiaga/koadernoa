package com.koadernoa.app.objektuak.zikloak.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;
import com.koadernoa.app.objektuak.zikloak.repository.FamiliaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FamiliaService {

	private final FamiliaRepository familiaRepository;

    @Transactional(readOnly = true)
    public List<Familia> lortuAktiboakOrdenatuta() {
        return familiaRepository.findAllByAktiboTrueOrderByIzenaAsc();
    }
}
