package com.koadernoa.app.objektuak.zikloak.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;
import com.koadernoa.app.objektuak.zikloak.repository.ZikloaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ZikloaService {

    private final ZikloaRepository zikloaRepository;

    public List<Zikloa> getAll() {
        return zikloaRepository.findAllByOrderByIzenaAsc();
    }

    public Optional<Zikloa> getById(Long id) {
        return zikloaRepository.findById(id);
    }

    public List<Zikloa> getByFamiliaId(Long familiaId) {
        return zikloaRepository.findByFamilia_IdOrderByIzenaAsc(familiaId);
    }

    public void save(Zikloa zikloa) {
        if (zikloa.getId() != null) {
            zikloaRepository.findById(zikloa.getId()).ifPresent(aurrekoa -> {
                if (zikloa.getEthaziTeknikoenPisua() == null) {
                    zikloa.setEthaziTeknikoenPisua(aurrekoa.getEthaziTeknikoenPisua());
                }
                if (zikloa.getEthaziZeharkakoenPisua() == null) {
                    zikloa.setEthaziZeharkakoenPisua(aurrekoa.getEthaziZeharkakoenPisua());
                }
                if (zikloa.getEthaziHeteroebaluazioPisua() == null) zikloa.setEthaziHeteroebaluazioPisua(aurrekoa.getEthaziHeteroebaluazioPisua());
                if (zikloa.getEthaziKoebaluazioPisua() == null) zikloa.setEthaziKoebaluazioPisua(aurrekoa.getEthaziKoebaluazioPisua());
                if (zikloa.getEthaziAutoebaluazioPisua() == null) zikloa.setEthaziAutoebaluazioPisua(aurrekoa.getEthaziAutoebaluazioPisua());
            });
        }
        zikloaRepository.save(zikloa);
    }

    @Transactional
    public void eguneratuEthaziPisuak(Long id, BigDecimal teknikoenPisua, BigDecimal zeharkakoenPisua) {
        eguneratuEthaziPisuak(id, teknikoenPisua, zeharkakoenPisua,
            Zikloa.DEFEKTUZKO_HETEROEBALUAZIO_PISUA, Zikloa.DEFEKTUZKO_KOEBALUAZIO_PISUA,
            Zikloa.DEFEKTUZKO_AUTOEBALUAZIO_PISUA);
    }

    @Transactional
    public void eguneratuEthaziPisuak(Long id, BigDecimal teknikoenPisua, BigDecimal zeharkakoenPisua,
            BigDecimal heteroebaluazioPisua, BigDecimal koebaluazioPisua, BigDecimal autoebaluazioPisua) {
        BigDecimal teknikoak = normalizatuPisua(teknikoenPisua, "Teknikoen pisua");
        BigDecimal zeharkakoak = normalizatuPisua(zeharkakoenPisua, "Zeharkakoen pisua");
        if (teknikoak.add(zeharkakoak).compareTo(new BigDecimal("100.00")) != 0) {
            throw new IllegalArgumentException("Teknikoen eta zeharkakoen pisuen baturak %100 izan behar du.");
        }
        BigDecimal hetero = normalizatuPisua(heteroebaluazioPisua, "Heteroebaluazioaren pisua");
        BigDecimal ko = normalizatuPisua(koebaluazioPisua, "Koebaluazioaren pisua");
        BigDecimal auto = normalizatuPisua(autoebaluazioPisua, "Autoebaluazioaren pisua");
        if (hetero.add(ko).add(auto).compareTo(new BigDecimal("100.00")) != 0)
            throw new IllegalArgumentException("Heteroebaluazioaren, koebaluazioaren eta autoebaluazioaren pisuen baturak %100 izan behar du.");

        Zikloa zikloa = zikloaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Ez da aurkitu zikloa id: " + id));
        zikloa.setEthaziTeknikoenPisua(teknikoak);
        zikloa.setEthaziZeharkakoenPisua(zeharkakoak);
        zikloa.setEthaziHeteroebaluazioPisua(hetero);
        zikloa.setEthaziKoebaluazioPisua(ko);
        zikloa.setEthaziAutoebaluazioPisua(auto);
        zikloaRepository.save(zikloa);
    }

    private BigDecimal normalizatuPisua(BigDecimal pisua, String eremua) {
        if (pisua == null) {
            throw new IllegalArgumentException(eremua + " derrigorrezkoa da.");
        }
        final BigDecimal normalizatua;
        try {
            normalizatua = pisua.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(eremua + " gehienez bi hamartarrekin adierazi behar da.");
        }
        if (normalizatua.compareTo(BigDecimal.ZERO) < 0
                || normalizatua.compareTo(new BigDecimal("100.00")) > 0) {
            throw new IllegalArgumentException(eremua + " 0 eta 100 artean egon behar da.");
        }
        return normalizatua;
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
