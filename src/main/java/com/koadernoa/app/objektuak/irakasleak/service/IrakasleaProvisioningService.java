package com.koadernoa.app.objektuak.irakasleak.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Rola;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IrakasleaProvisioningService {

    private final IrakasleaRepository irakasleaRepository;

    @Transactional
    public Irakaslea bilatuEdoSortu(String emaila, String izena, String kontuMota) {
        String emailNormalizatua = normalizatuEmaila(emaila);
        Irakaslea irakaslea = irakasleaRepository.findByEmailaIgnoreCase(emailNormalizatua)
                .orElseGet(() -> sortu(emailNormalizatua, izena, kontuMota, null));
        if (irakaslea.getRola() == null) {
            irakaslea.setRola(Rola.IRAKASLEA);
            return irakasleaRepository.save(irakaslea);
        }
        return irakaslea;
    }

    @Transactional
    public synchronized Irakaslea sortuEskuz(String emaila, String izena, Familia mintegia) {
        String emailNormalizatua = normalizatuEmaila(emaila);
        if (irakasleaRepository.findByEmailaIgnoreCase(emailNormalizatua).isPresent()) {
            throw new IrakasleaDagoenekoBadagoException();
        }
        try {
            Irakaslea sortua = sortu(emailNormalizatua, izena, "LDAP erabiltzailea", mintegia);
            irakasleaRepository.flush();
            return sortua;
        } catch (DataIntegrityViolationException ex) {
            throw new IrakasleaDagoenekoBadagoException();
        }
    }

    private Irakaslea sortu(String emaila, String izena, String kontuMota, Familia mintegia) {
        Irakaslea berria = new Irakaslea();
        berria.setEmaila(emaila);
        berria.setIzena(StringUtils.hasText(izena) ? izena.trim() : emaila);
        berria.setKontu_mota(kontuMota);
        berria.setRola(Rola.IRAKASLEA);
        berria.setMintegia(mintegia);
        return irakasleaRepository.save(berria);
    }

    private String normalizatuEmaila(String emaila) {
        if (!StringUtils.hasText(emaila)) {
            throw new IllegalArgumentException("Irakaslearen helbide elektronikoa falta da.");
        }
        return emaila.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public static class IrakasleaDagoenekoBadagoException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
