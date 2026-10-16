package com.koadernoa.app.objektuak.koadernoak.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioMomentua;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioNota;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioMomentuaRepository;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioNotaRepository;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Ebaluaketa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Jarduera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JardueraNotaIraulketaService {

    private final JardueraKalifikazioService jardueraKalifikazioService;
    private final EbaluazioMomentuaRepository ebaluazioMomentuaRepository;
    private final EbaluazioNotaRepository ebaluazioNotaRepository;

    @Transactional(readOnly = true)
    public boolean irauliDaiteke(Long koadernoId, Long ebaluaketaId) {
        if (koadernoId == null || ebaluaketaId == null) return false;
        return !iraulDaitezkeenMatrikulak(
                jardueraKalifikazioService.prestatu(koadernoId, ebaluaketaId, null)).isEmpty();
    }

    @Transactional
    public int irauli(Koadernoa koadernoa, Long ebaluaketaId) {
        if (koadernoa == null || koadernoa.getId() == null || koadernoa.getModuloa() == null
                || koadernoa.getModuloa().getMaila() == null) {
            throw new IllegalArgumentException("Ez dago koaderno aktibo baliodunik.");
        }

        var data = jardueraKalifikazioService.prestatu(koadernoa.getId(), ebaluaketaId, null);
        List<Matrikula> iraulDaitezkeenMatrikulak = iraulDaitezkeenMatrikulak(data);
        if (iraulDaitezkeenMatrikulak.isEmpty()) {
            throw new IllegalArgumentException(
                    "Gutxienez ikasle batek pisua duten jarduera guztietan nota izan behar du kalkulatutako notak irauli aurretik.");
        }

        Ebaluaketa ebaluaketa = data.ebaluaketak().stream()
                .filter(e -> ebaluaketaId.equals(e.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ebaluaketa ez da koaderno honetan aurkitu."));
        if (ebaluaketa.getOrdena() == null) {
            throw new IllegalArgumentException("Ebaluaketak ez du ordenarik eta ezin da nota-momentuarekin lotu.");
        }

        String momentuKodea = ebaluaketa.getOrdena() + "_EBAL";
        EbaluazioMomentua momentua = ebaluazioMomentuaRepository
                .findByMailaAndKodea(koadernoa.getModuloa().getMaila(), momentuKodea)
                .filter(m -> Boolean.TRUE.equals(m.getAktibo()) && Boolean.TRUE.equals(m.getOnartuNotaZenbakizkoa()))
                .orElseThrow(() -> new IllegalArgumentException(
                        momentuKodea + " ebaluazio-momentu aktibo eta zenbakizkoa ez da aurkitu."));

        List<EbaluazioNota> gordetzekoak = new ArrayList<>();
        iraulDaitezkeenMatrikulak.forEach(matrikula -> {
            EbaluazioNota nota = ebaluazioNotaRepository
                    .findByMatrikulaAndEbaluazioMomentua(matrikula, momentua)
                    .orElseGet(EbaluazioNota::new);
            nota.setMatrikula(matrikula);
            nota.setEbaluazioMomentua(momentua);
            nota.setNota(egokituNota(data.guztirakoak().get(matrikula.getId()), momentua));
            nota.setEgoera(null);
            gordetzekoak.add(nota);
        });
        ebaluazioNotaRepository.saveAll(gordetzekoak);
        return gordetzekoak.size();
    }

    private double egokituNota(BigDecimal nota, EbaluazioMomentua momentua) {
        if (Boolean.TRUE.equals(momentua.getOnartuHamartarrak())) {
            return nota.doubleValue();
        }
        if (nota.compareTo(new BigDecimal("5")) < 0) {
            return nota.setScale(0, RoundingMode.FLOOR).doubleValue();
        }

        BigDecimal behekoa = nota.setScale(0, RoundingMode.FLOOR);
        BigDecimal zatikia = nota.subtract(behekoa);
        return zatikia.compareTo(new BigDecimal("0.50")) <= 0
                ? behekoa.doubleValue()
                : behekoa.add(BigDecimal.ONE).doubleValue();
    }

    private List<Matrikula> iraulDaitezkeenMatrikulak(JardueraKalifikazioService.PantailaData data) {
        List<Jarduera> pisudunak = data.hautatutakoJarduerak().stream()
                .filter(j -> j.getPisua() != null && j.getPisua().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        if (pisudunak.isEmpty() || data.matrikulak().isEmpty()) return List.of();

        return data.matrikulak().stream()
                .filter(matrikula -> pisudunak.stream().allMatch(jarduera ->
                        data.notak().get(matrikula.getId() + "_" + jarduera.getId()) != null))
                .toList();
    }
}
