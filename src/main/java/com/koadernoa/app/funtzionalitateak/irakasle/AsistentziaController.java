package com.koadernoa.app.funtzionalitateak.irakasle;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Asistentzia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa;
import com.koadernoa.app.objektuak.koadernoak.repository.AsistentziaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoOrdutegiBlokeaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.SaioaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.AsistentziaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle/asistentzia")
@RequiredArgsConstructor
@SessionAttributes("koadernoAktiboa") //<- KOADERNOA SESIOAN GORDE
public class AsistentziaController {
	
	  private final AsistentziaService asistentziaService;
	  private final AsistentziaRepository asistentziaRepository;
	  private final SaioaRepository saioaRepository;
	  private final KoadernoOrdutegiBlokeaRepository koadernoOrdutegiBlokeaRepository;
	  private final MatrikulaRepository matrikulaRepository;

	  /** GET: Eguneko taula (Saioa-ren lazy-create. Behar denean sortu) */
	  @GetMapping({"","/"})
	  @Transactional
	  public String egunekoAsistentzia(
	      @SessionAttribute("koadernoAktiboa") Koadernoa koadernoa,
	      @RequestParam("data") String dataIso,
	      Model model) {

	    LocalDate data = LocalDate.parse(dataIso);

	    // Egun horretarako saioak bermatu (slot bakoitzeko bana)
	    asistentziaService.ensureSaioakForDate(koadernoa, data);

	    // ondoren, kargatu saioak + matrikulak eta renderizatu
	    List<Saioa> saioak = saioaRepository.findByKoadernoaIdAndData(koadernoa.getId(), data)
	        .stream().sorted(Comparator.comparingInt(Saioa::getHasieraSlot)).toList();
	    List<Matrikula> matrikulak =
	        matrikulaRepository.findByKoadernoaIdAndEgoeraMatrikulatuta(koadernoa.getId());

	    model.addAttribute("data", data);
	    model.addAttribute("saioak", saioak);
	    model.addAttribute("matrikulak", matrikulak);
	    model.addAttribute("egoeraMap", asistentziaService.mapEgoerak(saioak, matrikulak));
	    return "irakasleak/asistentzia/eguna";
	  }
	  
	  /** POST: taula gorde */
	  @PostMapping({"","/"})
	  @Transactional
	  public String gordeEgunekoAsistentzia(
	      @SessionAttribute("koadernoAktiboa") Koadernoa koadernoa,
	      @RequestParam("data") String dataIso,
	      @RequestParam Map<String,String> form) {

	    LocalDate data = LocalDate.parse(dataIso);

	    form.entrySet().stream()
	      .filter(e -> e.getKey().startsWith("chk_"))
	      .forEach(e -> {
	        String[] parts = e.getKey().split("_");
	        Long saioaId = Long.valueOf(parts[1]);
	        Long matrId  = Long.valueOf(parts[2]);
	        String val   = e.getValue(); // "" edo HUTS/JUSTIFIKATUA/BERANDU

	        if (val == null || val.isBlank()) {
	          // ETORRI => erregistroa ezabatu (aukera gomendatua) edo ez sortu
	          asistentziaRepository.deleteBySaioaIdAndMatrikulaId(saioaId, matrId);
	        } else {
	          var egoera = Asistentzia.AsistentziaEgoera.valueOf(val); // hemen ez du lehertuko
	          asistentziaService.markatu(saioaId, matrId, egoera, null, null);
	        }
	      });

	    return "redirect:/irakasle/asistentzia?data=" + dataIso;
	  }
	  
// API endpoint
	  @PostMapping("/markatu")
	  @ResponseBody
	  @Transactional
	  public Map<String, Object> markatuAjax(
	      @SessionAttribute("koadernoAktiboa") Koadernoa koadernoa,
	      @RequestParam Long saioaId,
	      @RequestParam Long matrikulaId,
	      @RequestParam(required=false) String egoera // null/"" => ETORRI
	  ) {
	    // koherentzia minimoa: saioa eta matrikula koaderno berekoak direla (gomendatua)
	    Saioa s = saioaRepository.findById(saioaId).orElseThrow();
	    if (!s.getKoadernoa().getId().equals(koadernoa.getId()))
	      throw new IllegalArgumentException("Saioa ez dator bat koadernoarekin");

	    if (egoera == null || egoera.isBlank() || "ETORRI".equals(egoera)) {
	      asistentziaRepository.deleteBySaioaIdAndMatrikulaId(saioaId, matrikulaId);
	    } else {
	      Asistentzia.AsistentziaEgoera e = Asistentzia.AsistentziaEgoera.valueOf(egoera);
	      asistentziaService.markatu(saioaId, matrikulaId, e, null, null);
	    }
	    return Map.of("ok", true);
	  }
}
