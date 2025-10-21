package com.koadernoa.app.objektuak.koadernoak.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunMota;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Asistentzia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Asistentzia.AsistentziaEgoera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoOrdutegiBlokea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa;
import com.koadernoa.app.objektuak.koadernoak.repository.AsistentziaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoOrdutegiBlokeaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.SaioaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AsistentziaService {
  private final SaioaRepository saioaRepo;
  private final AsistentziaRepository asisRepo;
  private final MatrikulaRepository matrRepo; 
  private final KoadernoOrdutegiBlokeaRepository koadernoOrdutegiBlokeaRepository;

  @Transactional
  private Saioa ensureSaioa(Long koadernoaId, LocalDate data, int hasieraSlot, int iraupenaSlot, KoadernoOrdutegiBlokea iturburu){
    return saioaRepo.findByKoadernoaIdAndData(koadernoaId, data).stream()
      .filter(s -> s.getHasieraSlot()==hasieraSlot).findFirst()
      .orElseGet(() -> {
        var s = new Saioa();
        var k = new Koadernoa(); k.setId(koadernoaId);
        s.setKoadernoa(k);
        s.setData(data); s.setHasieraSlot(hasieraSlot); s.setIraupenaSlot(iraupenaSlot);
        s.setIturburuBlokea(iturburu);
        return saioaRepo.save(s);
      });
  }
  
  @Transactional
  public void ensureSaioakForDate(Koadernoa koadernoa, LocalDate data) {
	  if (!isEgunLektiboa(koadernoa.getEgutegia(), data)) return;

	  var asteGuna = mapAsteguna(data.getDayOfWeek());
	  var blokeak = koadernoOrdutegiBlokeaRepository
	                  .findByKoadernoaIdAndAsteguna(koadernoa.getId(), asteGuna);

	  for (var b : blokeak) {
	    // slot bakoitzeko saio bana
	    for (int slot = b.getHasieraSlot(); slot <= b.bukaeraSlot(); slot++) {
	      ensureSaioa(
	          koadernoa.getId(),
	          data,
	          slot,
	          1,     // iraupenaSlot beti 1: saio = ordu
	          b
	      );
	    }
	  }
	}

  @Transactional
  public Asistentzia markatu(Long saioaId, Long matrikulaId, Asistentzia.AsistentziaEgoera egoera,
                             String justTestu, String justFitx) {
    var a = asisRepo.findBySaioaIdAndMatrikulaId(saioaId, matrikulaId)
      .orElseGet(() -> {
        var n = new Asistentzia();
        n.setSaioa(saioaRepo.getReferenceById(saioaId));
        var m = new Matrikula(); m.setId(matrikulaId);
        n.setMatrikula(m);
        return n;
      });
    a.setEgoera(egoera);
    a.setJustifikazioTestu(egoera==Asistentzia.AsistentziaEgoera.JUSTIFIKATUA ? justTestu : null);
    return asisRepo.save(a);
  }
  
  public boolean isEgunLektiboa(Egutegia egutegia, LocalDate data){
    // Errespetatu JAIEGUNA/EZ_LEKTIBOA; ORDEZKATUA bada, lektiboa izaten jarraitzen du
    return egutegia.getEgunBereziak().stream()
      .filter(e -> data.equals(e.getData()))
      .noneMatch(e -> e.getMota()==EgunMota.EZ_LEKTIBOA || e.getMota()==EgunMota.JAIEGUNA);
  }
  
  /** Completa en DB los que no vinieron en el POST como ETORRI */
  @Transactional
  public void marcarEtorriCuandoFalta(Long koadernoaId, LocalDate data){
    var saioak = saioaRepo.findByKoadernoaIdAndData(koadernoaId, data);
    for (var s: saioak){
      var asis = asisRepo.findBySaioaId(s.getId());
      var ya = asis.stream().map(a->a.getMatrikula().getId()).collect(Collectors.toSet());
      // aquí deberías inyectar MatrikulaRepository o pasar la colección de matrículas activas
      // y para las que no existan registros, crear ETORRI
    }
  }

  /** Mapa inicial para pintar checks */
  @Transactional(readOnly = true)
  public Map<Long, Map<Long, Asistentzia.AsistentziaEgoera>> mapEgoerak(List<Saioa> saioak, List<Matrikula> matrikulak){
    Map<Long, Map<Long, Asistentzia.AsistentziaEgoera>> out = new HashMap<>();
    for (var s: saioak){
      var inner = new HashMap<Long, Asistentzia.AsistentziaEgoera>();
      asisRepo.findBySaioaId(s.getId()).forEach(a-> inner.put(a.getMatrikula().getId(), a.getEgoera()));
      out.put(s.getId(), inner);
    }
    return out;
  }
  
  public Astegunak mapAsteguna(java.time.DayOfWeek dow){
      return switch (dow){
          case MONDAY    -> Astegunak.ASTELEHENA;
          case TUESDAY   -> Astegunak.ASTEARTEA;
          case WEDNESDAY -> Astegunak.ASTEAZKENA;
          case THURSDAY  -> Astegunak.OSTEGUNA;
          default        -> Astegunak.OSTIRALA; // FRIDAY
      };
  }
  
  public Map<String, Boolean> kalkulatuAsistentziaEgunak(
	        Set<Astegunak> blokAstegunak,
	        Egutegia egutegia, int urtea, int hilabetea) {

	    Map<String, Boolean> map = new HashMap<>();

	    LocalDate hasieraHil = LocalDate.of(urtea, hilabetea, 1);
	    LocalDate amaieraHil = hasieraHil.withDayOfMonth(hasieraHil.lengthOfMonth());

	    // Egutegiaren muga-datak (defentsiboa)
	    LocalDate egHas = egutegia.getHasieraData();
	    LocalDate egBuk = egutegia.getBukaeraData();

	    Map<LocalDate, EgunBerezi> bereziak = egutegia.getEgunBereziak() == null
	        ? Map.of()
	        : egutegia.getEgunBereziak().stream()
	            .collect(Collectors.toMap(EgunBerezi::getData, eb -> eb, (a,b)->a));

	    for (LocalDate d = hasieraHil; !d.isAfter(amaieraHil); d = d.plusDays(1)) {

	        // 0) Egutegiaren muga: kanpoan bada, EZ
	        if ((egHas != null && d.isBefore(egHas)) || (egBuk != null && d.isAfter(egBuk))) {
	            map.put(d.toString(), false);
	            continue;
	        }

	        // 1) Asteburuak EZ
	        if (d.getDayOfWeek().getValue() >= 6) {
	            map.put(d.toString(), false);
	            continue;
	        }

	        // 2) Egun bereziak: EZ_LEKTIBOA/JAIEGUNA bada, EZ
	        EgunBerezi eb = bereziak.get(d);
	        if (eb != null && (eb.getMota() == EgunMota.EZ_LEKTIBOA || eb.getMota() == EgunMota.JAIEGUNA)) {
	            map.put(d.toString(), false);
	            continue;
	        }

	        // 3) Ordezkatua bada, ordezkatutako asteguna hartu
	        Astegunak asteguna = mapAsteguna(d.getDayOfWeek());
	        if (eb != null && eb.getMota() == EgunMota.ORDEZKATUA && eb.getOrdezkatua() != null) {
	            asteguna = eb.getOrdezkatua();
	        }

	        // 4) Koadernoaren ordutegiak astegun horretan badu blokerik?
	        boolean badago = blokAstegunak.contains(asteguna);
	        map.put(d.toString(), badago);
	    }
	    return map;
	}

}

