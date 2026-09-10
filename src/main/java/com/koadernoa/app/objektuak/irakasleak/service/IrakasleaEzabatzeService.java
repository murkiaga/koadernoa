package com.koadernoa.app.objektuak.irakasleak.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.mezuak.repository.MezuaRepository;
import com.koadernoa.app.objektuak.ordutegiak.repository.IrakasleOrdutegiaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IrakasleaEzabatzeService {

    private final MezuaRepository mezuaRepository;
    private final IrakasleOrdutegiaRepository irakasleOrdutegiaRepository;
    private final IrakasleaRepository irakasleaRepository;

    @Transactional
    public void ezabatu(Irakaslea irakaslea) {
        Long irakasleId = irakaslea.getId();
        mezuaRepository.deleteByIrakasleaId(irakasleId);
        irakasleOrdutegiaRepository.deleteByIrakasleaId(irakasleId);
        irakasleaRepository.delete(irakaslea);
        irakasleaRepository.flush();
    }
}
