package com.koadernoa.app.egutegia.service;

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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.koadernoa.app.egutegia.entitateak.Astegunak;
import com.koadernoa.app.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.egutegia.entitateak.EgunMota;
import com.koadernoa.app.egutegia.entitateak.Egutegia;
import com.koadernoa.app.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.egutegia.entitateak.Maila;
import com.koadernoa.app.egutegia.repository.EgutegiaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EgutegiaService {
	
	private final EgutegiaRepository egutegiaRepository;

	public void sortuLektiboEgunak(Egutegia egutegia) {
	    List<EgunBerezi> egunBereziak = new ArrayList<>();
	    LocalDate eguna = egutegia.getHasieraData();
	    LocalDate bukaera = egutegia.getBukaeraData();

	    while (!eguna.isAfter(bukaera)) {
	        DayOfWeek asteEguna = eguna.getDayOfWeek();
	        // Astelehena - Ostirala = Lektibo
	        if (asteEguna != DayOfWeek.SATURDAY && asteEguna != DayOfWeek.SUNDAY) {
	            EgunBerezi lektiboa = new EgunBerezi();
	            lektiboa.setData(eguna);
	            lektiboa.setMota(EgunMota.LEKTIBOA);
	            lektiboa.setDeskribapena("Lektiboa");
	            lektiboa.setEgutegia(egutegia);
	            egunBereziak.add(lektiboa);
	        }
	        eguna = eguna.plusDays(1);
	    }

	    egutegia.setEgunBereziak(egunBereziak);
	    egutegiaRepository.save(egutegia);
	}
	
	public Map<String, List<List<LocalDate>>> prestatuHilabetekoEgutegiak(Egutegia egutegia) {
	    Map<String, List<List<LocalDate>>> emaitza = new LinkedHashMap<>();
	    LocalDate hasiera = egutegia.getHasieraData();
	    LocalDate bukaera = egutegia.getBukaeraData();

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
	
	public Map<LocalDate, EgunBerezi> getEgunBereziakMap(Egutegia egutegia) {
        return egutegia.getEgunBereziak().stream()
                .collect(Collectors.toMap(EgunBerezi::getData, Function.identity()));
    }
	
	public Map<LocalDate, EgunBerezi> mapatuEgunBereziak(Egutegia egutegia) {
        if (egutegia.getEgunBereziak() == null) {
            return new HashMap<>();
        }

        return egutegia.getEgunBereziak().stream()
            .collect(Collectors.toMap(
                EgunBerezi::getData,
                Function.identity(),
                (a, b) -> a, // berdinak badira, lehenengoa gorde
                LinkedHashMap::new
            ));
    }
	
	public Map<String, String> kalkulatuKlaseak(Egutegia egutegia){
        Map<String, String> klaseak = new LinkedHashMap<>();

        if (egutegia.getEgunBereziak() != null) {
            for (EgunBerezi eb : egutegia.getEgunBereziak()) {
                switch (eb.getMota()) {
                    case JAIEGUNA -> klaseak.put(eb.getData().toString(), "jaieguna");
                    case ORDEZKATUA -> klaseak.put(eb.getData().toString(), "ordezkatua");
                    case EZ_LEKTIBOA -> klaseak.put(eb.getData().toString(), "ezlektiboa");
                    case LEKTIBOA -> {
                        LocalDate data = eb.getData();
                        if (egutegia.getLehenEbalBukaera() != null && !data.isAfter(egutegia.getLehenEbalBukaera())) {
                            klaseak.put(data.toString(), "lektibo1");
                        } else if (egutegia.getBigarrenEbalBukaera() != null && !data.isAfter(egutegia.getBigarrenEbalBukaera())) {
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
    public void aldatuEgunMota(Egutegia egutegia, LocalDate data, EgunMota mota, Astegunak ordezkatua) {
        for (EgunBerezi eb : egutegia.getEgunBereziak()) {
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
        berria.setEgutegia(egutegia);
        egutegia.getEgunBereziak().add(berria);
    }
	
	public Egutegia getById(Long id) {
	    return egutegiaRepository.findById(id).orElseThrow(() ->
	        new IllegalArgumentException("Egutegia ez da aurkitu: " + id));
	}
	
	public Optional<Egutegia> getAktiboEtaMaila(Maila maila) {
	    return egutegiaRepository.findByIkasturtea_AktiboaTrueAndMaila(maila);
	}

}
