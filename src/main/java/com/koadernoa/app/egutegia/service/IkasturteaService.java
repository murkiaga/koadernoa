package com.koadernoa.app.egutegia.service;

import com.koadernoa.app.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.egutegia.entitateak.Maila;
import com.koadernoa.app.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.zikloak.repository.TaldeaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.koadernoa.app.egutegia.entitateak.Astegunak;
import com.koadernoa.app.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.egutegia.entitateak.EgunMota;

@Service
@RequiredArgsConstructor
public class IkasturteaService {

	
	private final IkasturteaRepository ikasturteaRepository;
	
	public void sortuLektiboEgunak(Ikasturtea ikasturtea) {
	    List<EgunBerezi> egunBereziak = new ArrayList<>();
	    LocalDate eguna = ikasturtea.getHasieraData();
	    LocalDate bukaera = ikasturtea.getBukaeraData();

	    while (!eguna.isAfter(bukaera)) {
	        DayOfWeek asteEguna = eguna.getDayOfWeek();
	        // Astelehena - Ostirala = Lektibo
	        if (asteEguna != DayOfWeek.SATURDAY && asteEguna != DayOfWeek.SUNDAY) {
	            EgunBerezi lektiboa = new EgunBerezi();
	            lektiboa.setData(eguna);
	            lektiboa.setMota(EgunMota.LEKTIBOA);
	            lektiboa.setDeskribapena("Lektiboa");
	            lektiboa.setIkasturtea(ikasturtea);
	            egunBereziak.add(lektiboa);
	        }
	        eguna = eguna.plusDays(1);
	    }

	    ikasturtea.setEgunBereziak(egunBereziak);
	    ikasturteaRepository.save(ikasturtea);
	}
	
	public Map<String, List<List<LocalDate>>> prestatuHilabetekoEgutegiak(Ikasturtea ikasturtea) {
	    Map<String, List<List<LocalDate>>> emaitza = new LinkedHashMap<>();
	    LocalDate hasiera = ikasturtea.getHasieraData();
	    LocalDate bukaera = ikasturtea.getBukaeraData();

	    for (LocalDate hil = hasiera.withDayOfMonth(1); !hil.isAfter(bukaera); hil = hil.plusMonths(1)) {
	        List<List<LocalDate>> asteak = new ArrayList<>();

	        // ✅ Hona hemen aldaketa: lehen eguna hilabeteko 1etik atzera astelehenetik hasita
	        LocalDate lehenEguna = hil.withDayOfMonth(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	        // Azken eguna: hilabetearen amaiera eta igandea arte
	        LocalDate azkenEguna = hil.withDayOfMonth(hil.lengthOfMonth()).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

	        List<LocalDate> astea = new ArrayList<>();
	        for (LocalDate eguna = lehenEguna; !eguna.isAfter(azkenEguna); eguna = eguna.plusDays(1)) {
	            astea.add(eguna.getMonth() == hil.getMonth() ? eguna : null);
	            if (astea.size() == 7) {
	                asteak.add(astea);
	                astea = new ArrayList<>();
	            }
	        }

	        // Hilabetearen izena euskaraz
	        String hilabeteIzena = hil.getMonth().getDisplayName(TextStyle.FULL, new Locale("eu"));
	        emaitza.put(hilabeteIzena, asteak);
	    }

	    return emaitza;
	}


	
	
	public Ikasturtea getIkasturteAktiboa() {
		List<Ikasturtea> aktiboak = ikasturteaRepository.findByAktiboaTrue();
	    if (aktiboak.isEmpty()) return null;
	    return aktiboak.get(0); // Lehena
    }

    @Transactional
    public void gordeIkasturtea(Ikasturtea ikasturtea) {
        ikasturteaRepository.findAll().forEach(i -> {
            i.setAktiboa(false);
            ikasturteaRepository.save(i);
        });
        ikasturteaRepository.save(ikasturtea);
    }
    
    public List<Ikasturtea> getAktiboakByMaila(int mailaZenbakia) {
    	Maila mailaEnum = (mailaZenbakia == 1) ? Maila.LEHENENGOA : Maila.BIGARRENA;
        return ikasturteaRepository.findByAktiboaTrueAndMaila(mailaEnum);
    }
    
    public Map<LocalDate, EgunBerezi> getEgunBereziakMap(Ikasturtea ikasturtea) {
        return ikasturtea.getEgunBereziak().stream()
                .collect(Collectors.toMap(EgunBerezi::getData, Function.identity()));
    }
    
    public Map<LocalDate, EgunBerezi> mapatuEgunBereziak(Ikasturtea ikasturtea) {
        if (ikasturtea.getEgunBereziak() == null) {
            return new HashMap<>();
        }

        return ikasturtea.getEgunBereziak().stream()
            .collect(Collectors.toMap(
                EgunBerezi::getData,
                Function.identity(),
                (a, b) -> a, // berdinak badira, lehenengoa gorde
                LinkedHashMap::new
            ));
    }

    
    public Map<String, String> kalkulatuKlaseak(Ikasturtea ikasturtea){
        Map<String, String> klaseak = new LinkedHashMap<>();

        if (ikasturtea.getEgunBereziak() != null) {
            for (EgunBerezi eb : ikasturtea.getEgunBereziak()) {
                switch (eb.getMota()) {
                    case JAIEGUNA -> klaseak.put(eb.getData().toString(), "jaieguna");
                    case ORDEZKATUA -> klaseak.put(eb.getData().toString(), "ordezkatua");
                    case EZ_LEKTIBOA -> klaseak.put(eb.getData().toString(), "ezlektiboa");
                    case LEKTIBOA -> {
                        LocalDate data = eb.getData();
                        if (ikasturtea.getLehenEbalBukaera() != null && !data.isAfter(ikasturtea.getLehenEbalBukaera())) {
                            klaseak.put(data.toString(), "lektibo1");
                        } else if (ikasturtea.getBigarrenEbalBukaera() != null && !data.isAfter(ikasturtea.getBigarrenEbalBukaera())) {
                            klaseak.put(data.toString(), "lektibo2");
                        } else {
                            klaseak.put(data.toString(), "lektibo3");
                        }
                    }
                }
            }
        }

        return klaseak;
    }
    
    @Transactional
    public void aldatuEgunMota(Ikasturtea ikasturtea, LocalDate data, EgunMota mota, Astegunak ordezkatua) {
        for (EgunBerezi eb : ikasturtea.getEgunBereziak()) {
            if (eb.getData().equals(data)) {
                eb.setMota(mota);
                if (mota == EgunMota.ORDEZKATUA) {
                    eb.setOrdezkatua(ordezkatua);
                    eb.setDeskribapena("Ordezkatua: " + (ordezkatua != null ? ordezkatua.name() : ""));
                } else {
                    eb.setOrdezkatua(null); // garbitu aurreko balioa
                    eb.setDeskribapena("Eskuz aldatuta");
                }
                return;
            }
        }

        EgunBerezi berria = new EgunBerezi();
        berria.setData(data);
        berria.setMota(mota);
        if (mota == EgunMota.ORDEZKATUA) {
            berria.setOrdezkatua(ordezkatua);
            berria.setDeskribapena("Ordezkatua: " + (ordezkatua != null ? ordezkatua.name() : ""));
        } else {
            berria.setDeskribapena("Eskuz aldatuta");
        }
        berria.setIkasturtea(ikasturtea);
        ikasturtea.getEgunBereziak().add(berria);
    }

    public List<Ikasturtea> getAktiboak() {
        return ikasturteaRepository.findByAktiboaTrue();
    }
    public Ikasturtea getById(Long id) {
        return ikasturteaRepository.findById(id).orElse(null);
    }
	
	
}
