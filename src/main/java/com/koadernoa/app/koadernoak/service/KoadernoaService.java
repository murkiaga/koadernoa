package com.koadernoa.app.koadernoak.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.koadernoa.app.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.egutegia.repository.IkasturteaRepository;
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
	    List<Moduloa> moduluak = moduloaRepository.findByMaila(ikasturtea.getMaila());
	    for (Moduloa m : moduluak) {
	        Koadernoa k = new Koadernoa();
	        k.setIkasturtea(ikasturtea);
	        k.setModuloa(m);
	        // Beste eremu batzuk inplementatu behar badira (irakasleak, estatistikak…)
	        koadernoaRepository.save(k);
	    }
	}

}
