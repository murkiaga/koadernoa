package com.koadernoa.app.koadernoak.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.koadernoa.app.egutegia.entitateak.Egutegia;
import com.koadernoa.app.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.egutegia.entitateak.Maila;
import com.koadernoa.app.egutegia.repository.EgutegiaRepository;
import com.koadernoa.app.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.koadernoak.entitateak.EbaluazioMota;
import com.koadernoa.app.koadernoak.entitateak.EstatistikaEbaluazioan;
import com.koadernoa.app.koadernoak.entitateak.KoadernoaSortuDto;
import com.koadernoa.app.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.modulua.entitateak.Moduloa;
import com.koadernoa.app.modulua.repository.ModuloaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KoadernoaService {

	private final ModuloaRepository moduloaRepository;
    private final EgutegiaRepository egutegiaRepository;
    private final IrakasleaRepository irakasleaRepository;
    private final KoadernoaRepository koadernoaRepository;
    private final IkasturteaRepository ikasturteaRepository;
	
    public void sortuKoadernoakIkasturteBerrirako(Ikasturtea ikasturtea) {
        for (Egutegia egutegia : ikasturtea.getEgutegiak()) {
            Maila maila = egutegia.getMaila();
            List<Moduloa> moduluak = moduloaRepository.findByMaila(maila);

            for (Moduloa moduloa : moduluak) {
                Koadernoa koadernoa = new Koadernoa();
                koadernoa.setModuloa(moduloa);
                koadernoa.setEgutegia(egutegia);
                koadernoaRepository.save(koadernoa);
            }
        }
    }
    
    public Koadernoa findById(Long id) {
        return koadernoaRepository.findById(id).orElseThrow();
    }

    public List<Koadernoa> findByIrakaslea(Irakaslea irakaslea) {
        return koadernoaRepository.findByIrakasleakContaining(irakaslea);
    }
    
    public List<Moduloa> lortuErabilgarriDaudenModuluak(Irakaslea irakaslea) {
        return moduloaRepository.findByTaldea_Zikloa_Familia(irakaslea.getMintegia());
    }

    public List<Egutegia> lortuEgutegiGuztiak() {
        return egutegiaRepository.findAll();
    }

    public List<Irakaslea> lortuFamiliaBerekoIrakasleak(Irakaslea irakaslea) {
        return irakasleaRepository.findByMintegia(irakaslea.getMintegia())
            .stream()
            .filter(i -> !i.getId().equals(irakaslea.getId()))
            .toList();
    }

    @Transactional
    public Koadernoa sortuKoadernoa(KoadernoaSortuDto dto, Irakaslea irakaslea) {
        Moduloa moduloa = moduloaRepository.findById(dto.getModuloaId())
                .orElseThrow(() -> new IllegalArgumentException("Ez da modulu hori aurkitu."));

        Egutegia egutegia = egutegiaRepository
                .findByIkasturtea_AktiboaTrueAndMaila(moduloa.getMaila())
                .orElseThrow(() -> new IllegalStateException("Ez da egutegirik aurkitu aktibo dagoen ikasturterako eta maila horretarako."));

        if (!moduloa.getTaldea().getZikloa().getFamilia().equals(irakaslea.getMintegia())) {
            throw new AccessDeniedException("Beste familiako modulua aukeratu da.");
        }

        if (!moduloa.getMaila().equals(egutegia.getMaila())) {
            throw new IllegalArgumentException("Moduluaren eta egutegiaren maila ez datoz bat.");
        }

        List<Irakaslea> irakasleak = new ArrayList<>();

        // Beti gehitu irakasle sortzailea
        irakasleak.add(irakaslea);

        // Beste irakasle batzuk baldin badaude, gehitu
        if (dto.getIrakasleIdZerrenda() != null && !dto.getIrakasleIdZerrenda().isEmpty()) {
            List<Irakaslea> besteIrakasleak = irakasleaRepository.findAllById(dto.getIrakasleIdZerrenda());
            // Bakarra ez bada, sortzailea berriro ez gehitzeko
            for (Irakaslea i : besteIrakasleak) {
                if (!i.getId().equals(irakaslea.getId())) {
                    irakasleak.add(i);
                }
            }
        }
        Koadernoa k = new Koadernoa();
        k.setModuloa(moduloa);
        k.setEgutegia(egutegia);
        k.setIrakasleak(irakasleak);
        k.setJarduerak(List.of());
        k.setNotaFitxategiak(List.of());
        k.setEstatistikak(new ArrayList<>()); //hasieran hutsik
        
        List<EstatistikaEbaluazioan> estatistikak = sortuEstatistikak(k);
        k.setEstatistikak(estatistikak);

        return koadernoaRepository.save(k);
    }
    
    @Transactional
    private List<EstatistikaEbaluazioan> sortuEstatistikak(Koadernoa koadernoa) {
        Maila maila = koadernoa.getModuloa().getMaila();

        List<EbaluazioMota> ebaluazioMotak = switch (maila) {
            case LEHENENGOA -> List.of(
                EbaluazioMota.LEHENENGO_EBALUAZIOA,
                EbaluazioMota.BIGARREN_EBALUAZIOA,
                EbaluazioMota.LEHENENGO_FINALA,
                EbaluazioMota.BIGARREN_FINALA
            );
            case BIGARRENA -> List.of(
                EbaluazioMota.LEHENENGO_EBALUAZIOA,
                EbaluazioMota.LEHENENGO_FINALA,
                EbaluazioMota.BIGARREN_FINALA
            );
        };

        return ebaluazioMotak.stream()
            .map(mota -> {
                EstatistikaEbaluazioan e = new EstatistikaEbaluazioan();
                e.setEbaluazioMota(mota);
                e.setKoadernoa(koadernoa);
                return e;
            })
            .toList();
    }


}
