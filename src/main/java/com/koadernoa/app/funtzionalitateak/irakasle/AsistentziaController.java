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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Asistentzia;
import com.koadernoa.app.objektuak.audit.entitateak.AuditAtala;
import com.koadernoa.app.objektuak.audit.entitateak.AuditEkintza;
import com.koadernoa.app.objektuak.audit.service.AuditService;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa;
import com.koadernoa.app.objektuak.koadernoak.repository.AsistentziaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoOrdutegiBlokeaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.SaioaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.AsistentziaService;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoaService;
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
	  private final MatrikulaRepository matrikulaRepository;
	  private final KoadernoaService koadernoaService;
	  private final AuditService auditService;

	  /** GET: Eguneko taula (Saioa-ren lazy-create. Behar denean sortu) */
	  @GetMapping({"","/"})
	  @Transactional
	  public String egunekoAsistentzia(
	      @SessionAttribute("koadernoAktiboa") Koadernoa koadernoa,
	      @RequestParam("data") String dataIso,
	      Authentication auth,
	      Model model) {

	    if (koadernoa == null || koadernoa.getId() == null) {
	      model.addAttribute("errorea", "Ez dago koaderno aktiborik aukeratuta.");
	      return "error/404";
	    }

	    Koadernoa kargatutakoKoadernoa = koadernoaService
	        .findByIdWithEgutegiaAndEgunBereziak(koadernoa.getId())
	        .orElseThrow(() -> new IllegalStateException("Koaderno aktiboa ez da aurkitu."));

	    LocalDate data = LocalDate.parse(dataIso);

	    // Egun horretarako saioak bermatu (slot bakoitzeko bana)
	    asistentziaService.ensureSaioakForDate(kargatutakoKoadernoa, data);

	    // ondoren, kargatu saioak + matrikulak eta renderizatu
	    List<Saioa> saioak = saioaRepository.findByKoadernoaIdAndData(kargatutakoKoadernoa.getId(), data)
	        .stream().sorted(Comparator.comparingInt(Saioa::getHasieraSlot)).toList();
	    List<Matrikula> matrikulak =
	        matrikulaRepository.findByKoadernoaIdAndEgoeraMatrikulatuta(kargatutakoKoadernoa.getId());

	    model.addAttribute("data", data);
	    model.addAttribute("saioak", saioak);
	    model.addAttribute("matrikulak", matrikulak);
	    model.addAttribute("egoeraMap", asistentziaService.mapEgoerak(saioak, matrikulak));
	    PrincipalInfo p = resolvePrincipalInfo(auth);
	    var event = auditService.buildBaseEvent(
	        p.erabiltzaileId,
	        p.emaila,
	        p.izena,
	        p.rola,
	        "/irakasle/asistentzia",
	        "GET",
	        null,
	        null,
	        "Ekintza=ASISTENTZIA_PASATU data=" + dataIso,
	        AuditAtala.DENBORALIZAZIOA,
	        AuditEkintza.ASISTENTZIA_PASATU
	    );
	    event.setKoadernoId(kargatutakoKoadernoa.getId());
	    event.setEntitateMota("Koadernoa");
	    event.setEntitateId(String.valueOf(kargatutakoKoadernoa.getId()));
	    event.setArrakastatsua(true);
	    auditService.recordAction(event);
	    return "irakasleak/asistentzia/eguna";
	  }

	  private PrincipalInfo resolvePrincipalInfo(Authentication auth) {
	    if (auth == null) return new PrincipalInfo(null, null, null, null);
	    Object principal = auth.getPrincipal();

	    if (principal instanceof com.koadernoa.app.objektuak.irakasleak.entitateak.IrakasleUserDetails iu) {
	      var ir = iu.getIrakaslea();
	      return new PrincipalInfo(ir.getId(), ir.getEmaila(), ir.getIzena(), ir.getRola() != null ? ir.getRola().name() : null);
	    }
	    if (principal instanceof OidcUser oidcUser) {
	      return new PrincipalInfo(null,
	          firstNonBlank(oidcUser.getEmail(), oidcUser.getPreferredUsername()),
	          firstNonBlank(oidcUser.getFullName(), oidcUser.getName()),
	          null);
	    }
	    if (principal instanceof OAuth2User oauth2User) {
	      Object email = oauth2User.getAttributes().get("email");
	      Object name = oauth2User.getAttributes().get("name");
	      return new PrincipalInfo(null,
	          firstNonBlank((String) email, auth.getName()),
	          firstNonBlank((String) name, auth.getName()),
	          null);
	    }
	    if (principal instanceof UserDetails ud) {
	      return new PrincipalInfo(null, ud.getUsername(), ud.getUsername(), null);
	    }
	    return new PrincipalInfo(null, auth.getName(), auth.getName(), null);
	  }

	  private String firstNonBlank(String first, String second) {
	    if (first != null && !first.isBlank()) return first;
	    return (second != null && !second.isBlank()) ? second : null;
	  }

	  private record PrincipalInfo(Long erabiltzaileId, String emaila, String izena, String rola) {}
	  
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
