package com.koadernoa.app.funtzionalitateak.irakasle;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.audit.entitateak.AuditAtala;
import com.koadernoa.app.objektuak.audit.entitateak.AuditEkintza;
import com.koadernoa.app.objektuak.audit.service.AuditService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Rola;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoaSortuDto;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoaService;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoSorreraEmaitza;
import com.koadernoa.app.objektuak.koadernoak.service.ProgramazioaService;
import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;
import com.koadernoa.app.objektuak.modulua.service.IkasleaService;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.IrakasleOrdutegiLerroa;
import com.koadernoa.app.objektuak.ordutegiak.repository.IrakasleOrdutegiaRepository;
import com.koadernoa.app.objektuak.zikloak.service.FamiliaService;

import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle/koadernoa")
@RequiredArgsConstructor
@SessionAttributes("koadernoAktiboa") //<-KOADERNOA SESIOAN GORDE
public class KoadernoaController {
	
	private final IrakasleaService irakasleaService;
	private final KoadernoaService koadernoaService;
    private final ProgramazioaService programazioaService;
	private final IkasleaService ikasleaService;
    private final AuditService auditService;
	private final KoadernoaRepository koadernoaRepository;
	private final FamiliaService familiaService;
	private final AplikazioAukeraService aplikazioAukeraService;
    private final IrakasleOrdutegiaRepository irakasleOrdutegiaRepository;
	
	private static final List<Astegunak> ASTE_ORDENA = List.of(
	        Astegunak.ASTELEHENA,
	        Astegunak.ASTEARTEA,
	        Astegunak.ASTEAZKENA,
	        Astegunak.OSTEGUNA,
	        Astegunak.OSTIRALA
	    );

	
	@GetMapping("/{id}")
	public String hautatuKoadernoa(
	        @PathVariable Long id,
	        @RequestParam(value = "next", required = false) String nextEncoded,
	        Authentication auth,
	        Model model,
	        RedirectAttributes ra) {

	    Koadernoa koadernoa = koadernoaService.findById(id);
	    if (koadernoa == null) {
	        ra.addFlashAttribute("error", "Koadernoa ez da existitzen.");
	        return "redirect:/irakasle";
	    }

	    // Nor dago logeatuta?
	    Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);

	    // ADMIN / KUDEATZAILEA bada, beti onartu
	    boolean adminEdoKude = auth != null && auth.getAuthorities().stream()
	            .anyMatch(a ->
	                    "ROLE_ADMIN".equals(a.getAuthority()) ||
	                    "ROLE_KUDEATZAILEA".equals(a.getAuthority())
	            );

	    if (!adminEdoKude && !koadernoaService.irakasleakBadaukaSarbidea(irakaslea, koadernoa)) {
	        ra.addFlashAttribute("error", "Ez duzu koaderno honetarako sarbiderik.");
	        return "redirect:/irakasle";
	    }

	    // Sesioan gorde (@SessionAttributes("koadernoAktiboa") dela eta)
	    model.addAttribute("koadernoAktiboa", koadernoa);

	    // 'next' DEKODETU eta modu seguruan balidatu
	    String next = null;
	    if (nextEncoded != null && !nextEncoded.isBlank()) {
	        next = java.net.URLDecoder.decode(nextEncoded, java.nio.charset.StandardCharsets.UTF_8);
	    }

	    if (isSafeInternal(next)) {
	        return "redirect:" + next;
	    }
	    return "redirect:/irakasle";
	}

	
	private boolean isSafeInternal(String next) {
	    if (next == null) return false;
	    // injekzio saiakerak/kanpoko URL-ak baztertu
	    if (next.contains("\r") || next.contains("\n")) return false;
	    if (next.startsWith("http://") || next.startsWith("https://")) return false;
	    if (!next.startsWith("/")) return false;

	    // loopak saihestu eta bide zentzudunak onartu
	    if (next.startsWith("/irakasle/koaderno/")) return false;
	    // Nahi baduzu, hemen murriztu: /irakasle... soilik
	    if (!next.startsWith("/irakasle")) return false;

	    return true;
	}
	
	@GetMapping("/berria")
	public String erakutsiFormularioa(Authentication auth, 
									@RequestParam(value = "familiaId", required = false) Long familiaId,
									@RequestParam(value = "zikloaId", required = false) Long zikloaId,
									@RequestParam(value = "mailaId", required = false) Long mailaId,
									Model model) {
		Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);

	    Long nireFamiliaId = (irakaslea.getMintegia() != null) ? irakaslea.getMintegia().getId() : null;
	    boolean besteMintegiaBaimendu = aplikazioAukeraService.getBool(
	            AplikazioAukeraService.KOADERNO_BESTE_MINTEGIA_BAIMENDU, false);
	    boolean kanpokoFamiliaEskatuDa = familiaId != null && !java.util.Objects.equals(familiaId, nireFamiliaId);
	    Long aukeratua = (besteMintegiaBaimendu && familiaId != null) ? familiaId : nireFamiliaId;
	    if (!besteMintegiaBaimendu && kanpokoFamiliaEskatuDa) {
	        zikloaId = null;
	        mailaId = null;
	    }

	    KoadernoaSortuDto dto = new KoadernoaSortuDto();
	    dto.setFamiliaId(aukeratua);
	    dto.setZikloaId(zikloaId);
	    dto.setMailaId(mailaId);

	    model.addAttribute("koadernoaDto", dto);

	    // 1) Familia aukerak: beste mintegietako koadernoak desgaituta badaude, irakaslearen familia soilik erakutsi
	    model.addAttribute("familiaGuztiak", besteMintegiaBaimendu
	            ? familiaService.lortuAktiboakOrdenatuta()
	            : (irakaslea.getMintegia() != null ? List.of(irakaslea.getMintegia()) : List.of()));
	    model.addAttribute("zikloak", koadernoaService.lortuErabilgarriDaudenZikloak(irakaslea, aukeratua));
	    model.addAttribute("mailak", koadernoaService.lortuErabilgarriDaudenMailak(irakaslea, aukeratua, zikloaId));

	    // 2) Moduluak familia, ziklo eta maila iragazkien arabera
	    model.addAttribute("moduluak",
	            koadernoaService.lortuErabilgarriDaudenModuluak(irakaslea, aukeratua, zikloaId, mailaId));

	    model.addAttribute("irakasleLogeatua", irakaslea);

	    // Ordutegi-grid-a
	    model.addAttribute("rows", IntStream.rangeClosed(1, 12).boxed().toList());
	    model.addAttribute("cols", ASTE_ORDENA);
	    model.addAttribute("selected", Set.of());
        model.addAttribute("irakasleOrdutegiRows", sortuIrakasleOrdutegiRows(irakaslea));

	    return "irakasleak/koadernoa-sortu";
    }

    private List<List<String>> sortuIrakasleOrdutegiRows(Irakaslea irakaslea) {
        List<List<String>> grid = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            grid.add(new ArrayList<>(java.util.Collections.nCopies(ASTE_ORDENA.size(), null)));
        }
        if (irakaslea == null) {
            return grid;
        }
        irakasleOrdutegiaRepository
                .findAktiboenaByIrakasleaId(irakaslea.getId())
                .ifPresent(ordutegia -> {
                    if (ordutegia.getLerroak() == null) {
                        return;
                    }
                    for (IrakasleOrdutegiLerroa lerroa : ordutegia.getLerroak()) {
                        if (lerroa.getAsteguna() == null || lerroa.getOrduZenbakia() == null) continue;
                        int col = ASTE_ORDENA.indexOf(lerroa.getAsteguna());
                        if (col < 0) continue;
                        int saioKopurua = lerroa.getSaioKopurua() != null && lerroa.getSaioKopurua() > 0
                                ? lerroa.getSaioKopurua() : 1;
                        String testua = irakasleOrdutegiGelaxkaTestua(lerroa);
                        for (int ordua = lerroa.getOrduZenbakia(); ordua < lerroa.getOrduZenbakia() + saioKopurua; ordua++) {
                            int row = ordua - 1;
                            if (row < 0 || row >= grid.size()) continue;
                            String aurrekoa = grid.get(row).get(col);
                            grid.get(row).set(col, aurrekoa == null || aurrekoa.isBlank() ? testua : aurrekoa + " / " + testua);
                        }
                    }
                });
        return grid;
    }

    private String irakasleOrdutegiGelaxkaTestua(IrakasleOrdutegiLerroa lerroa) {
        return java.util.stream.Stream.of(lerroa.getModuluKodea(), lerroa.getTaldeKodea(), irakasleOrdutegiGelaTestua(lerroa))
                .filter(v -> v != null && !v.isBlank())
                .collect(java.util.stream.Collectors.joining(" · "));
    }

    private String irakasleOrdutegiGelaTestua(IrakasleOrdutegiLerroa lerroa) {
        if (lerroa.getGelaKodea() != null && !lerroa.getGelaKodea().isBlank()) return lerroa.getGelaKodea();
        return lerroa.getGelaIzena();
    }

	@PostMapping("/berria")
	public String submit(@ModelAttribute("koadernoaDto") KoadernoaSortuDto dto,
	                     Authentication auth,
	                     Model model,
	                     RedirectAttributes ra,
	                     @RequestParam(name = "cells", required = false) List<String> cells) {

	    Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);

        List<String> selectedCells = cells == null ? List.of() : cells.stream()
                .filter(v -> v != null && !v.isBlank())
                .toList();
        if (selectedCells.isEmpty()) {
            ra.addFlashAttribute("error", "Ordutegi taulan gutxienez gelaxka bat hautatu behar duzu koadernoa sortzeko.");
            return "redirect:/irakasle/koadernoa/berria";
        }

        try {
            KoadernoSorreraEmaitza emaitza = koadernoaService.sortuEdoEsleituKoadernoa(
                    dto,
                    irakaslea,
                    selectedCells
            );

            if (emaitza.egoera() == KoadernoSorreraEmaitza.Egoera.EXISTITZEN_DA) {
                ra.addFlashAttribute("error", emaitza.mezua());
                return "redirect:/irakasle/koadernoa/berria";
            }

	            Koadernoa koadernoa = emaitza.koadernoa();
	            model.addAttribute("koadernoAktiboa", koadernoa);
	            try {
	                ikasleaService.syncKoadernoBakarra(koadernoa.getId());
	            } catch (Exception e) {
            }
            ra.addFlashAttribute("success", emaitza.mezua());
            var event = auditService.buildBaseEvent(
                    null, null, null, null,
                    "/irakasle/koadernoa/berria", "POST", null, null,
                    "Ekintza=KOADERNOA_SORTU",
                    AuditAtala.IRAKASLE, AuditEkintza.KOADERNOA_SORTU);
            event.setKoadernoId(koadernoa.getId());
            event.setEntitateMota("Koadernoa");
            event.setEntitateId(String.valueOf(koadernoa.getId()));
            event.setArrakastatsua(true);
            auditService.recordAction(event);
            return "redirect:/irakasle";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/irakasle/koadernoa/berria";
        }
	}
	
	@PostMapping("/{id}/ordutegia/cell")
    @ResponseBody
    public ResponseEntity<?> toggleCell(@PathVariable Long id,
                                        @RequestParam int col,
                                        @RequestParam int row,
                                        @RequestParam boolean selected,
                                        @RequestParam(required = false) LocalDate scheduleStartDate) {
        try {
            koadernoaService.setSlotSelected(id, col, row, selected, scheduleStartDate);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", ex.getMessage()));
        }
    }


	

	@PostMapping("/{id}/ordutegia/berria")
    @ResponseBody
    public ResponseEntity<?> ordutegiBerria(@PathVariable Long id,
                                            @RequestParam LocalDate hasieraData,
                                            @RequestParam(name = "dualOrdutegia", defaultValue = "false") boolean dualOrdutegia) {
        LocalDate created = koadernoaService.sortuOrdutegiBerria(id, hasieraData, dualOrdutegia);
        programazioaService.syncDualUdForKoaderno(id);
        var event = auditService.buildBaseEvent(
                null, null, null, null,
                "/irakasle/koadernoa/" + id + "/ordutegia/berria", "POST", null, null,
                "hasieraData=" + hasieraData,
                AuditAtala.IRAKASLE, AuditEkintza.ORDUTEGI_BERRIA_SORTU);
        event.setKoadernoId(id);
        event.setEntitateMota("Koadernoa");
        event.setEntitateId(String.valueOf(id));
        event.setArrakastatsua(true);
        auditService.recordAction(event);
        return ResponseEntity.ok(Map.of("ok", true, "hasieraData", created.toString()));
    }

    @PostMapping("/{id}/ordutegia/ezabatu")
    @ResponseBody
    public ResponseEntity<?> ezabatuOrdutegia(@PathVariable Long id,
                                              @RequestParam LocalDate hasieraData) {
        boolean deleted = koadernoaService.ezabatuOrdutegia(id, hasieraData);
        programazioaService.syncDualUdForKoaderno(id);
        return ResponseEntity.ok(Map.of("ok", true, "deleted", deleted));
    }


	@PostMapping("/{id}/moodle-esteka")
	public String gordeMoodleEsteka(@PathVariable Long id,
	                               @RequestParam("moodleEsteka") String moodleEsteka,
	                               @RequestParam(value = "next", required = false) String next,
	                               Authentication auth,
	                               Model model,
	                               RedirectAttributes ra) {
	    Koadernoa koadernoa = koadernoaRepository.findById(id)
	            .orElseThrow(() -> new IllegalArgumentException("Koadernoa ez da aurkitu"));

	    Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
	    if (!koadernoaService.irakasleakBadaukaSarbidea(irakaslea, koadernoa)) {
	        ra.addFlashAttribute("error", "Ez duzu koaderno honetarako sarbiderik.");
	        return "redirect:/irakasle";
	    }

	    Koadernoa eguneratua = koadernoaService.gordeMoodleEsteka(id, moodleEsteka);
	    model.addAttribute("koadernoAktiboa", eguneratua);
	    ra.addFlashAttribute("success", "Moodle esteka gorde da.");

	    if (isSafeInternal(next)) {
	        return "redirect:" + next;
	    }
	    return "redirect:/irakasle";
	}

	@PostMapping("/{id}/inportatu-taldetik")
	public String inportatuTaldekoIkasleakKoadernoan(@PathVariable("id") Long koadernoaId,
	                                                 RedirectAttributes ra) {
	    var res = ikasleaService.syncKoadernoBakarra(koadernoaId); 

	    if (res.ohartarazpena() != null) {
	        ra.addFlashAttribute("errorea", res.ohartarazpena());
	    } else if (res.sortuak() > 0) {
	        ra.addFlashAttribute("msg", res.sortuak() + " ikasle matrikulatu dira koaderno honetan (sinkronizatuta).");
	    } else {
	        ra.addFlashAttribute("msg", "Koadernoa sinkronizatuta: ez zegoen aldaketarik.");
	    }
	    return "redirect:/irakasle/ikasleak";
	}
	
	@PostMapping("/{id}/partekatu")
    public String partekatuKoadernoa(
            @PathVariable Long id,
            @RequestParam("ident") String ident,
            Authentication auth,
            RedirectAttributes ra) {

        // 1) Uneko irakaslea (zure service-ak dagoeneko OAuth2 / username bideratzen du)
        Irakaslea uneKoIrakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);

        // 2) Koadernoa ekarri
        Koadernoa koadernoa = koadernoaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Koadernoa ez da aurkitu"));

        if (koadernoa.getIrakasleak() == null) {
            koadernoa.setIrakasleak(new ArrayList<>());
        }
        
        // 3) Egiaztatu une-ko irakaslea koadernoaren irakasle-zerrendan dagoela, edo KUDEATZAILEA dela
        boolean irakasleakBaimena = koadernoaService.irakasleakBadaukaSarbidea(uneKoIrakaslea, koadernoa);

        if (!irakasleakBaimena) {
            ra.addFlashAttribute("error", "Ez duzu koaderno hau partekatzeko baimenik.");
            return "redirect:/irakasle";
        }

        // 4) Bilatu gehitu nahi duzun irakaslea (email edo izenaren arabera)
        var targetOpt = irakasleaService.bilatuIdent(ident);
        if (targetOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Ez da irakaslerik aurkitu: \"" + ident + "\".");
            return "redirect:/irakasle";
        }
        Irakaslea target = targetOpt.get();

        // 5) Dagoeneko badago?
        boolean already = koadernoa.getIrakasleak().stream()
                .anyMatch(i -> i.getId().equals(target.getId()));

        if (already) {
            ra.addFlashAttribute("success", "Irakaslea jada badago koaderno honetan.");
            return "redirect:/irakasle";
        }

        // 6) Gehitu eta gorde
        koadernoa.getIrakasleak().add(target);
        koadernoaRepository.save(koadernoa);
        var event = auditService.buildBaseEvent(
                null, null, null, null,
                "/irakasle/koadernoa/" + id + "/partekatu", "POST", null, null,
                "irakasleId=" + target.getId(),
                AuditAtala.IRAKASLE, AuditEkintza.KOADERNOA_PARTEKATU);
        event.setKoadernoId(id);
        event.setEntitateMota("Koadernoa");
        event.setEntitateId(String.valueOf(id));
        event.setArrakastatsua(true);
        auditService.recordAction(event);

        String izenOsoa = target.getIzena(); // edo target.getIzenaOsoa(), zuk daukazunaren arabera
        ra.addFlashAttribute("success",
                "Koadernoa partekatu da irakasle honekin: " + izenOsoa + ".");

        return "redirect:/irakasle";
    }
	
	@PostMapping("/{id}/irten")
	public String utziKoadernoa(@PathVariable Long id,
	                            Authentication auth,
	                            Model model,
	                            RedirectAttributes ra) {

	    // 1) Nor dago logeatuta?
	    Irakaslea ni = irakasleaService.getLogeatutaDagoenIrakaslea(auth);

	    // 2) Koadernoa hartu
	    Koadernoa k = koadernoaService.findById(id);
	    if (k == null) {
	        ra.addFlashAttribute("error", "Koadernoa ez da existitzen.");
	        return "redirect:/irakasle";
	    }

	    // 3) Egiaztatu ni koaderno honetako irakaslea naizela
	    boolean nireKoadernoaDa = k.getIrakasleak().stream()
	            .anyMatch(ir -> ir.getId().equals(ni.getId()));

	    if (!nireKoadernoaDa) {
	        ra.addFlashAttribute("error", "Koaderno honetako irakasle ez zara.");
	        return "redirect:/irakasle";
	    }

	    // 4) Ez utzi azken irakaslea izaten
	    if (k.getIrakasleak().size() <= 1) {
	        ra.addFlashAttribute("error",
	                "Ezin duzu koaderno hau utzi; bestela irakaslerik gabe geratuko litzateke.");
	        return "redirect:/irakasle";
	    }

	    // 5) Nire burua zerrendatik kendu eta gorde
	    k.getIrakasleak().removeIf(ir -> ir.getId().equals(ni.getId()));
        if (k.getJabea() != null && k.getJabea().getId().equals(ni.getId())) {
            k.setJabea(null);
        }
	    koadernoaRepository.save(k); 

	    // 6) Nire beste koadernoak bilatu
	    List<Koadernoa> nireKoadernoak = koadernoaRepository.findAllByIrakasleak_Id(ni.getId());

	    if (nireKoadernoak.isEmpty()) {
	        // koaderno aktiborik EZ → sesioan null jartzen dugu
	        model.addAttribute("koadernoAktiboa", null);
	        return "redirect:/login?logout";
	    } else {
	        // beste bat badago → lehenengoa aktibo jarri (edo zuk nahi duzuna)
	        model.addAttribute("koadernoAktiboa", nireKoadernoak.get(0));
	    }

	    ra.addFlashAttribute("success", "Koaderno hau utzi duzu.");
	    return "redirect:/irakasle";
	}


	
	@PostMapping("/{id}/irakasle/{irakasleId}/kendu")
	public String kenduIrakasleaKoadernotik(@PathVariable Long id,
	                                    @PathVariable Long irakasleId,
	                                    Authentication auth,
	                                    RedirectAttributes ra) {

	    Irakaslea ni = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
	    Koadernoa k = koadernoaService.findById(id);
	    if (k == null) {
	        ra.addFlashAttribute("error", "Koadernoa ez da existitzen.");
	        return "redirect:/irakasle";
	    }

	    boolean sarbidea = koadernoaService.irakasleakBadaukaSarbidea(ni, k);
	    boolean adminEdoKudeatzaile = ni.getRola() == Rola.ADMIN || ni.getRola() == Rola.KUDEATZAILEA;
        boolean jabeaDa = k.getJabea() != null && ni.getId() != null && ni.getId().equals(k.getJabea().getId());
	    if (!adminEdoKudeatzaile && !(sarbidea && jabeaDa)) {
	        ra.addFlashAttribute("error", "Ez duzu baimenik irakasleak kentzeko.");
	        return "redirect:/irakasle/koadernoa/" + id;
	    }

	    if (k.getIrakasleak() == null || k.getIrakasleak().stream().noneMatch(ir -> ir.getId().equals(irakasleId))) {
	        ra.addFlashAttribute("error", "Irakaslea ez dago koaderno honetan.");
	        return "redirect:/irakasle/koadernoa/" + id;
	    }

	    if (k.getIrakasleak().size() <= 1) {
	        ra.addFlashAttribute("error", "Ezin da azken irakaslea kendu.");
	        return "redirect:/irakasle/koadernoa/" + id;
	    }

	    k.getIrakasleak().removeIf(ir -> ir.getId().equals(irakasleId));
        if (k.getJabea() != null && k.getJabea().getId().equals(irakasleId)) {
            k.setJabea(null);
        }
	    koadernoaRepository.save(k);
	    ra.addFlashAttribute("success", "Irakaslea koadernotik kendu da.");
	    return "redirect:/irakasle/koadernoa/" + id;
	}


	@PostMapping("/{id}/ezabatu")
	public String ezabatuKoadernoa(@PathVariable Long id,
	                               @RequestParam("confirmIzena") String confirmIzena,
	                               @RequestParam("confirmIkasturtea") String confirmIkasturtea,
	                               Authentication auth,
	                               RedirectAttributes ra) {

	    // 1) Koadernoa existitzen den egiaztatu
	    Koadernoa k = koadernoaService.findById(id);
	    if (k == null) {
	        ra.addFlashAttribute("error", "Koadernoa ez da existitzen.");
	        return "redirect:/irakasle";
	    }

	    // 2) Irakasleak baimena duen egiaztatu
	    Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
	    if (!koadernoaService.irakasleakBadaukaSarbidea(irakaslea, k)) {
	        ra.addFlashAttribute("error", "Ez duzu koaderno hau ezabatzeko baimenik.");
	        return "redirect:/irakasle";
	    }

	    // 3) Confirm testuak konparatu (trim + case-sensitive)
	    String izenaEsperotakoa = k.getIzena() != null ? k.getIzena().trim() : "";
	    String ikasEsperotakoa =
	            (k.getEgutegia() != null &&
	             k.getEgutegia().getIkasturtea() != null &&
	             k.getEgutegia().getIkasturtea().getIzena() != null)
	                    ? k.getEgutegia().getIkasturtea().getIzena().trim()
	                    : "";

	    String izenaUser = confirmIzena != null ? confirmIzena.trim() : "";
	    String ikasUser  = confirmIkasturtea != null ? confirmIkasturtea.trim() : "";

	    if (!izenaEsperotakoa.equals(izenaUser) || !ikasEsperotakoa.equals(ikasUser)) {
	        ra.addFlashAttribute("error",
	            "Baieztapen testuak ez datoz bat. Idatzi zehazki:\n" +
	            "Koadernoaren izena: \"" + izenaEsperotakoa + "\"\n" +
	            "Ikasturtea: \"" + ikasEsperotakoa + "\"");
	        return "redirect:/irakasle";
	    }

	    // 4) Benetako ezabaketa + debug
	    try {
	        System.out.println(">>> KOADERNOA EZABATZEN: id=" + k.getId()
	                + ", izena=" + izenaEsperotakoa);

	        koadernoaService.ezabatuKoadernoa(k, irakaslea);

	        ra.addFlashAttribute("success", "Koadernoa ondo ezabatu da.");
	    } catch (org.springframework.dao.DataIntegrityViolationException ex) {
	        ex.printStackTrace(); // log-ean ikusteko
	        ra.addFlashAttribute("error",
	            "Ezin izan da koadernoa ezabatu: badirudi badituela erlazionatutako datuak " +
	            "(jarduerak, asistentziak, notak...).");
	    } catch (Exception ex) {
	        ex.printStackTrace();
	        ra.addFlashAttribute("error", "Ezin izan da koadernoa ezabatu: " + ex.getMessage());
	    }

	    return "redirect:/irakasle";
	}

}
	
