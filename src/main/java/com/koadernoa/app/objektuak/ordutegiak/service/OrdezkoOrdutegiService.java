package com.koadernoa.app.objektuak.ordutegiak.service;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.IrakasleOrdutegiLerroa;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.IrakasleOrdutegia;
import com.koadernoa.app.objektuak.ordutegiak.repository.IrakasleOrdutegiaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrdezkoOrdutegiService {

    private final IrakasleOrdutegiaRepository repository;

    @Transactional
    public void heredatu(Irakaslea titularra, Irakaslea ordezkoa) {
        kendu(titularra.getId(), ordezkoa.getId());
        for (IrakasleOrdutegia jatorrizkoa : repository.findAllByIrakasleaId(titularra.getId())) {
            IrakasleOrdutegia helburua = repository
                    .findByIrakasleaIdAndIkasturteaId(ordezkoa.getId(), jatorrizkoa.getIkasturtea().getId())
                    .orElseGet(() -> sortuHelburua(jatorrizkoa, ordezkoa, titularra.getId()));
            if (helburua.getLerroak() == null) {
                helburua.setLerroak(new ArrayList<>());
            }
            for (IrakasleOrdutegiLerroa lerroa : jatorrizkoa.getLerroak()) {
                helburua.gehituLerroa(kopiatu(lerroa, titularra.getId()));
            }
            repository.save(helburua);
        }
    }

    @Transactional
    public void kendu(Long titularraId, Long ordezkoaId) {
        for (IrakasleOrdutegia ordutegia : repository.findAllByIrakasleaId(ordezkoaId)) {
            if (ordutegia.getLerroak() == null) continue;
            ordutegia.getLerroak().removeIf(lerroa -> titularraId.equals(lerroa.getOrdezkapenTitularraId()));
            if (ordutegia.getLerroak().isEmpty()
                    && ("ORDEZKAPENA:" + titularraId).equals(ordutegia.getJatorria())) {
                repository.delete(ordutegia);
            } else {
                repository.save(ordutegia);
            }
        }
    }

    private IrakasleOrdutegia sortuHelburua(IrakasleOrdutegia jatorrizkoa, Irakaslea ordezkoa, Long titularraId) {
        IrakasleOrdutegia helburua = new IrakasleOrdutegia();
        helburua.setIkasturtea(jatorrizkoa.getIkasturtea());
        helburua.setIrakaslea(ordezkoa);
        helburua.setXmlIrakasleKodea(ordezkoa.getEmaila());
        helburua.setXmlIrakasleIzena(ordezkoa.getIzena());
        helburua.setJatorria("ORDEZKAPENA:" + titularraId);
        helburua.setInportazioData(LocalDateTime.now());
        return helburua;
    }

    private IrakasleOrdutegiLerroa kopiatu(IrakasleOrdutegiLerroa j, Long titularraId) {
        IrakasleOrdutegiLerroa b = new IrakasleOrdutegiLerroa();
        b.setAsteguna(j.getAsteguna()); b.setOrduZenbakia(j.getOrduZenbakia()); b.setSaioKopurua(j.getSaioKopurua());
        b.setModuluKodea(j.getModuluKodea()); b.setModuluIzena(j.getModuluIzena()); b.setTaldeKodea(j.getTaldeKodea()); b.setTaldea(j.getTaldea());
        b.setGelaKodea(j.getGelaKodea()); b.setGelaIzena(j.getGelaIzena()); b.setCurso(j.getCurso()); b.setGrupo(j.getGrupo());
        b.setNivel(j.getNivel()); b.setTurno(j.getTurno()); b.setMarco(j.getMarco()); b.setTarea(j.getTarea());
        b.setOrdezkapenTitularraId(titularraId);
        return b;
    }
}
