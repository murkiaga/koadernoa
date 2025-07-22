package com.koadernoa.app.koadernoak.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.koadernoa.app.egutegia.entitateak.Egutegia;
import com.koadernoa.app.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.egutegia.entitateak.Maila;
import com.koadernoa.app.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.modulua.entitateak.Moduloa;
import com.koadernoa.app.modulua.repository.ModuloaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KoadernoaService {

    private final ModuloaRepository moduloaRepository;
    private final KoadernoaRepository koadernoaRepository;
	
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

}
