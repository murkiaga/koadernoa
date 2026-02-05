package com.koadernoa.app.objektuak.koadernoak.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunMota;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Asistentzia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoOrdutegiBlokea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa.SaioEgoera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.denboralizazioa.FaltaIkasleRow;
import com.koadernoa.app.objektuak.koadernoak.entitateak.denboralizazioa.FaltakBistaDTO;
import com.koadernoa.app.objektuak.koadernoak.repository.AsistentziaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoOrdutegiBlokeaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.SaioaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.entitateak.MatrikulaEgoera;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AsistentziaService {
  private final SaioaRepository saioaRepo;
  private final AsistentziaRepository asisRepo;
  private final KoadernoOrdutegiBlokeaRepository koadernoOrdutegiBlokeaRepository;
  private final MatrikulaRepository matrikulaRepo;

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

	  //asteguna eraginkorra (ORDEZKATUA kontuan)
	  var asteGunaEraginkorra = effectiveAsteguna(koadernoa.getEgutegia(), data);

	  var blokeak = koadernoOrdutegiBlokeaRepository
	      .findByKoadernoaIdAndAsteguna(koadernoa.getId(), asteGunaEraginkorra);

	  for (var b : blokeak) {
	    // slot bakoitzeko saio bana (orduak banaka)
	    for (int slot = b.getHasieraSlot(); slot <= b.bukaeraSlot(); slot++) {
	      ensureSaioa(
	          koadernoa.getId(), data, slot, 1, b
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
    if (egutegia.getEgunBereziak() == null) {
      return true;
    }
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
  
  	/** Egun ordezkatuen jatorrizko ordutegia jasotzeko */
  	public Optional<EgunBerezi> getEgunBerezi(Egutegia egutegia, LocalDate data){
	  if (egutegia == null || egutegia.getEgunBereziak() == null) return Optional.empty();
	  return egutegia.getEgunBereziak().stream()
	      .filter(eb -> data.equals(eb.getData()))
	      .findFirst();
	}
  	/** Egun honetarako astegun "eraginkorra": ORDEZKATUA bada -> ordezkatua; bestela data.getDayOfWeek() */
  	public Astegunak effectiveAsteguna(Egutegia egutegia, LocalDate data){
  	  Astegunak base = mapAsteguna(data.getDayOfWeek());
  	  return getEgunBerezi(egutegia, data)
  	      .filter(eb -> eb.getMota() == EgunMota.ORDEZKATUA && eb.getOrdezkatua() != null)
  	      .map(EgunBerezi::getOrdezkatua)
  	      .orElse(base);
  	}
  	
  	public FaltakBistaDTO kalkulatuFaltenBista(Koadernoa koadernoa, int hilabetea, int urtea) {

        YearMonth ym = YearMonth.of(urtea, hilabetea);
        LocalDate from = ym.atDay(1);
        LocalDate to   = ym.atEndOfMonth();

        Long koadernoId = koadernoa.getId();

        // 1) Hilabete horretako saio guztiak, data+slot ordenatuta
        List<Saioa> saioakHilabetean =
                saioaRepo.findByKoadernoaIdAndDataBetweenOrderByDataAscHasieraSlotAsc(
                        koadernoId, from, to);

        // 2) Koaderno honetako saio GUZTIAK (programa orduak kalkulatzeko)
        List<Saioa> saioakGuztiak =
                saioaRepo.findByKoadernoa_Id(koadernoId);

        int programaOrduak = saioakGuztiak.stream()
                .filter(s -> s.getEgoera() != Saioa.SaioEgoera.EZEZTATUA)
                .mapToInt(Saioa::getIraupenaSlot)
                .sum();

        // 3) Hilabeteko egunak eta egun bakoitzeko klase orduak
        Map<LocalDate, List<Saioa>> byDate = saioakHilabetean.stream()
                .filter(s -> s.getEgoera() != Saioa.SaioEgoera.EZEZTATUA)
                .collect(Collectors.groupingBy(
                        Saioa::getData,
                        TreeMap::new,          // ordenatuak egon daitezen
                        Collectors.toList()
                ));

        List<LocalDate> egunak = new ArrayList<>(byDate.keySet());

        Map<LocalDate,Integer> egunekoOrduak = new LinkedHashMap<>();
        for (var entry : byDate.entrySet()) {
            int orduak = entry.getValue().stream()
                    .mapToInt(Saioa::getIraupenaSlot)
                    .sum();
            egunekoOrduak.put(entry.getKey(), orduak);
        }

        // 4) Koaderno honetako MATRIKULATUAK
        List<Matrikula> matrikulak =
                matrikulaRepo.findByKoadernoaIdAndEgoeraMatrikulatuta(koadernoId);

        Map<Long, FaltaIkasleRow> rowMap = new LinkedHashMap<>();
        for (Matrikula m : matrikulak) {
            FaltaIkasleRow row = new FaltaIkasleRow();
            row.setMatrikula(m);
            rowMap.put(m.getId(), row);
        }

        // 5) Hilabete horretako asistentziak
        if (!saioakHilabetean.isEmpty() && !matrikulak.isEmpty()) {

            List<Asistentzia> asistentziak =
                    asisRepo.findBySaioaInAndMatrikulaIn(
                            saioakHilabetean, matrikulak);

            for (Asistentzia a : asistentziak) {
                Saioa s = a.getSaioa();

                // saioa EZEZTATUA bada, ez dugu kontuan hartu nahi
                if (s.getEgoera() == Saioa.SaioEgoera.EZEZTATUA) continue;

                LocalDate data = s.getData();
                if (!byDate.containsKey(data)) continue;

                FaltaIkasleRow row = rowMap.get(a.getMatrikula().getId());
                if (row == null) continue;

                int orduak = s.getIraupenaSlot();

                switch (a.getEgoera()) {
                    case HUTS, JUSTIFIKATUA ->
                            row.gehituFalta(data, orduak, String.valueOf(orduak));
                    case BERANDU ->
                            // hemen erabaki: portzentajean kontatu nahi duzu ala ez;
                            // adib: markatu "b" eta ez gehitu orduak
                            row.gehituFalta(data, 0, "b");
                    case ETORRI -> {
                        // ezer ez
                    }
                }
            }
        }

        // 6) % kalkulua
        for (FaltaIkasleRow row : rowMap.values()) {
            double pct = programaOrduak == 0 ? 0
                    : row.getFaltaOrduak() * 100.0 / programaOrduak;
            row.setFaltaPortzentaia(pct);
        }

        // 7) DTO-a osatu
        FaltakBistaDTO dto = new FaltakBistaDTO();
        dto.setProgramaOrduak(programaOrduak);
        dto.setEgunak(egunak);
        dto.setEgunekoOrduak(egunekoOrduak);
        dto.setIkasleRows(new ArrayList<>(rowMap.values()));
        return dto;
    }

}
