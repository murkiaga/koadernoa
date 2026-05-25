package com.koadernoa.app.funtzionalitateak.irakasle;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import com.koadernoa.app.objektuak.audit.entitateak.AuditAtala;
import com.koadernoa.app.objektuak.audit.entitateak.AuditEkintza;
import com.koadernoa.app.objektuak.audit.service.AuditService;
import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunMota;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunaBista;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.service.EgutegiaService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.logak.entitateak.LogMota;
import com.koadernoa.app.objektuak.logak.service.LogService;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Jarduera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.JardueraEditDto;
import com.koadernoa.app.objektuak.koadernoak.entitateak.JardueraSortuDto;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.denboralizazioa.FaltakBistaDTO;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoOrdutegiBlokeaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.SaioaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.AsistentziaService;
import com.koadernoa.app.objektuak.koadernoak.service.DenboralizazioFaltaService;
import com.koadernoa.app.objektuak.koadernoak.service.DenboralizazioGeneratorService;
import com.koadernoa.app.objektuak.koadernoak.service.FaltenJakinarazpenPdfService;
import com.koadernoa.app.objektuak.koadernoak.service.FaltenExcelInportService;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoaService;
import com.koadernoa.app.objektuak.koadernoak.service.ProgramazioTxantiloiService;
import com.koadernoa.app.objektuak.koadernoak.service.ProgramazioaService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irakasle/denboralizazioa")
@RequiredArgsConstructor
@SessionAttributes("koadernoAktiboa") //<-KOADERNOA SESIOAN GORDE
public class DenboralizazioaController {
	
	private final EgutegiaService egutegiaService;
	private final AsistentziaService asistentziaService;
	private final KoadernoOrdutegiBlokeaRepository koadernoOrdutegiBlokeaRepository;
	private final SaioaRepository saioaRepository;
	private final KoadernoaService koadernoaService;
	private final DenboralizazioFaltaService denboralizazioFaltaService;
	private final DenboralizazioGeneratorService denboralizazioGeneratorService;
	private final FaltenJakinarazpenPdfService faltenJakinarazpenPdfService;
	private final FaltenExcelInportService faltenExcelInportService;
	private final ProgramazioTxantiloiService programazioTxantiloiService;
	private final ProgramazioaService programazioaService;
	private final IrakasleaService irakasleaService;
	private final LogService logService;
	private final AuditService auditService;

	@GetMapping({"","/"})
	public String erakutsiHilabetekoDenboralizazioa(
	    @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoa,
	    @RequestParam(name="urtea", required=false) Integer urtea,
	    @RequestParam(name="hilabetea", required=false) Integer hilabetea,
	    @RequestParam(name="bista", required=false, defaultValue = "egutegia") String bista,
	    @RequestParam(name="egutegiMota", required=false) String egutegiMota,
	    @RequestParam(name="asteHasiera", required=false) LocalDate asteHasiera,
	    Model model,
	    HttpSession session) {

	    if (koadernoa == null || koadernoa.getId() == null) {
	        model.addAttribute("errorea", "Ez dago koaderno aktiborik aukeratuta.");
	        return "error/404";
	    }

	    var koadernoaOpt = koadernoaService.findByIdWithEgutegiaAndEgunBereziak(koadernoa.getId());
	    if (koadernoaOpt.isEmpty()) {
	        model.addAttribute("errorea", "Koaderno aktiboa ez da aurkitu.");
	        return "error/404";
	    }

	    Koadernoa kargatutakoKoadernoa = koadernoaOpt.get();

	    LocalDate orain = LocalDate.now();
	    if (egutegiMota == null || egutegiMota.isBlank()) {
	        Object saioMota = session.getAttribute("denboralizazioaEgutegiMota");
	        egutegiMota = (saioMota instanceof String s && !s.isBlank()) ? s : "hilabetea";
	    } else {
	        session.setAttribute("denboralizazioaEgutegiMota", egutegiMota);
	    }
	    int unekoUrtea = (urtea != null) ? urtea : orain.getYear();
	    int unekoHilabetea = (hilabetea != null) ? hilabetea : orain.getMonthValue();

	    Egutegia egutegia = kargatutakoKoadernoa.getEgutegia();
	    if (egutegia == null) {
	        model.addAttribute("errorea", "Koaderno aktiboak ez du egutegirik esleituta.");
	        return "error/404";
	    }

	    List<LocalDate> egunGuztiak =
	        egutegiaService.getEgunakHilabetekoBista(unekoUrtea, unekoHilabetea, egutegia);

	    // Asteen zerrenda prestatu (astelehenetik ostiralera)
	    List<List<EgunaBista>> asteak = new ArrayList<>();
	    List<EgunaBista> astea = new ArrayList<>();

	    for (LocalDate eguna : egunGuztiak) {
	        if (eguna.getDayOfWeek().getValue() >= 1 && eguna.getDayOfWeek().getValue() <= 5) {
	            boolean aktiboa = eguna.getMonthValue() == unekoHilabetea;
	            astea.add(new EgunaBista(eguna, aktiboa));
	            if (astea.size() == 5) {
	                asteak.add(new ArrayList<>(astea));
	                astea.clear();
	            }
	        } else if (eguna.getDayOfWeek().getValue() == 7) {
	            if (!astea.isEmpty()) {
	                while (astea.size() < 5) {
	                    astea.add(new EgunaBista(null, false));
	                }
	                asteak.add(new ArrayList<>(astea));
	                astea.clear();
	            }
	        }
	    }
	    if (!astea.isEmpty()) {
	        while (astea.size() < 5) {
	            astea.add(new EgunaBista(null, false));
	        }
	        asteak.add(new ArrayList<>(astea));
	    }
	    
	    List<EgunaBista> astekoEgunak = List.of();
	    LocalDate astekoHasieraEraginkorra = null;
	    if ("astea".equalsIgnoreCase(egutegiMota)) {
	        if (asteHasiera != null) {
	            astekoHasieraEraginkorra = asteHasiera;
	            astekoEgunak = new ArrayList<>();
	            for (int i = 0; i < 5; i++) {
	                LocalDate d = asteHasiera.plusDays(i);
	                astekoEgunak.add(new EgunaBista(d, d.getMonthValue() == unekoHilabetea));
	            }
	        } else {
	            LocalDate gaur = LocalDate.now();
	            boolean hilabeteHonetanDa = gaur.getYear() == unekoUrtea && gaur.getMonthValue() == unekoHilabetea;
	            astekoEgunak = asteak.stream()
	                    .filter(a -> {
	                        if (hilabeteHonetanDa) {
	                            return a.stream().anyMatch(e -> e.getData() != null && e.getData().equals(gaur));
	                        }
	                        return a.stream().anyMatch(EgunaBista::isHilabeteAktibokoa);
	                    })
	                    .findFirst()
	                    .orElse(asteak.isEmpty() ? List.of() : asteak.get(0));
	            if (!astekoEgunak.isEmpty() && astekoEgunak.get(0).getData() != null) {
	                astekoHasieraEraginkorra = astekoEgunak.get(0).getData();
	            }
	        }
	    }

	    Map<String, String> egunMap = egutegiaService.kalkulatuKlaseak(egutegia);

	    // Egun bereziak segurtasunez hartu (null -> lista hutsa)
	    List<EgunBerezi> egunBereziak = (egutegia.getEgunBereziak() != null)
	            ? egutegia.getEgunBereziak()
	            : java.util.Collections.emptyList();

	    // toMap-ek ez du null key/value onartzen
	    Map<String, String> deskribapenaMap = egunBereziak.stream()
	            .filter(eb -> eb.getData() != null)                 // key-a ez dadin null izan
	            .filter(eb -> eb.getDeskribapena() != null)         // value-a ez dadin null izan
	            .collect(Collectors.toMap(
	                    eb -> eb.getData().toString(),
	                    eb -> eb.getDeskribapena(),                 // nahi baduzu: .trim()
	                    (a, b) -> a
	            ));

	    // Hilabete + urtea euskaraz formatuta
	    String hilabeteUrtea = LocalDate.of(unekoUrtea, unekoHilabetea, 1)
	            .format(java.time.format.DateTimeFormatter
	                    .ofPattern("LLLL yyyy")
	                    .withLocale(new java.util.Locale("eu", "ES")));

	    // Hilabetearen muga-datak
	    LocalDate hasiera = LocalDate.of(unekoUrtea, unekoHilabetea, 1);
	    LocalDate amaiera = hasiera.withDayOfMonth(hasiera.lengthOfMonth());
	    if ("astea".equalsIgnoreCase(egutegiMota) && astekoHasieraEraginkorra != null) {
	        hasiera = astekoHasieraEraginkorra;
	        amaiera = astekoHasieraEraginkorra.plusDays(4);
	    }

	    // Lortu jarduerak
	    List<Jarduera> jarduerak =
	            koadernoaService.lortuJarduerakDataTartean(kargatutakoKoadernoa, hasiera, amaiera);

	    // Lortu asistentzia egunak
	    LocalDate ikastHasiera = egutegia.getHasieraData();
	    java.util.NavigableMap<LocalDate, Set<Astegunak>> blokAstegunakByDate = new java.util.TreeMap<>();
	    for (var b : koadernoOrdutegiBlokeaRepository.findByKoadernoa_Id(kargatutakoKoadernoa.getId())) {
	        LocalDate has = b.getHasieraData() != null ? b.getHasieraData() : ikastHasiera;
	        if (b.isTarteHutsa()) {
	            blokAstegunakByDate.putIfAbsent(has, new HashSet<>());
	            continue;
	        }
	        if (b.isDualOrdutegia() || b.getAsteguna() == null || b.getIraupenaSlot() <= 0) {
	            continue;
	        }
	        blokAstegunakByDate.computeIfAbsent(has, __ -> new HashSet<>()).add(b.getAsteguna());
	    }

	    Map<String, Boolean> asistentziaEgunMap =
	            asistentziaService.kalkulatuAsistentziaEgunak(blokAstegunakByDate, egutegia, unekoUrtea, unekoHilabetea);
	    Set<LocalDate> asistentziaIrekitaEgunak = saioaRepository
	            .findByKoadernoaIdAndDataBetweenOrderByDataAscHasieraSlotAsc(kargatutakoKoadernoa.getId(), hasiera, amaiera)
	            .stream()
	            .map(Saioa::getData)
	            .collect(Collectors.toSet());
	    Map<String, Boolean> asistentziaIrekitaEgunMap = asistentziaIrekitaEgunak.stream()
	            .collect(Collectors.toMap(LocalDate::toString, __ -> true));

	    Map<LocalDate, List<Jarduera>> jardueraMap = jarduerak.stream()
	            .filter(j -> j.getData() != null) // segurtasun gehigarria
	            .collect(Collectors.groupingBy(Jarduera::getData));

	    var denboralizazioUnitateak = programazioaService
	            .loadWithEbaluaketakUdetajpByKoadernoId(kargatutakoKoadernoa.getId())
	            .map(p -> p.getEbaluaketak() == null ? java.util.List.<com.koadernoa.app.objektuak.koadernoak.entitateak.UnitateDidaktikoa>of()
	                    : p.getEbaluaketak().stream()
	                        .sorted(java.util.Comparator.comparing(e -> java.util.Optional.ofNullable(e.getOrdena()).orElse(0)))
	                        .flatMap(e -> e.getUnitateak() == null ? java.util.stream.Stream.<com.koadernoa.app.objektuak.koadernoak.entitateak.UnitateDidaktikoa>empty()
	                                : e.getUnitateak().stream()
	                                    .sorted(java.util.Comparator
	                                        .comparingInt(com.koadernoa.app.objektuak.koadernoak.entitateak.UnitateDidaktikoa::getPosizioa)
	                                        .thenComparing(com.koadernoa.app.objektuak.koadernoak.entitateak.UnitateDidaktikoa::getId)))
	                        .toList())
	            .orElse(java.util.List.of());

	    // Model atributuak (bi bistentzat komunak)
	    model.addAttribute("jardueraMap", jardueraMap);
	    model.addAttribute("denboralizazioUnitateak", denboralizazioUnitateak);
	    model.addAttribute("defaultUnitateaIdMap", kalkulatuEgunekoLehenUdMap(kargatutakoKoadernoa.getId()));
	    model.addAttribute("egutegia", egutegia);
	    model.addAttribute("ikasturtea", egutegia.getIkasturtea());
	    model.addAttribute("hilabeteUrtea", hilabeteUrtea);
	    model.addAttribute("urtea", unekoUrtea);
	    model.addAttribute("hilabetea", unekoHilabetea);
	    model.addAttribute("asteak", asteak);
	    model.addAttribute("astekoEgunak", astekoEgunak);
	    model.addAttribute("asteHasiera", astekoHasieraEraginkorra);
	    model.addAttribute("egunMap", egunMap);
	    model.addAttribute("asistentziaEgunMap", asistentziaEgunMap);
	    model.addAttribute("asistentziaIrekitaEgunMap", asistentziaIrekitaEgunMap);
	    model.addAttribute("deskribapenaMap", deskribapenaMap);
	    model.addAttribute("bista", bista);
	    model.addAttribute("egutegiMota", egutegiMota);
	    model.addAttribute("txantiloiAldia", dagoTxantiloiAldia(egutegia));

	    // FALTEN BISTA-rako datuak soilik bista == "faltak" denean
	    if ("faltak".equalsIgnoreCase(bista)) {
	        FaltakBistaDTO faltak = denboralizazioFaltaService
	                .kalkulatuFaltenBista(kargatutakoKoadernoa, unekoHilabetea, unekoUrtea);

	        model.addAttribute("programaOrduak", faltak.getProgramaOrduak());
	        model.addAttribute("faltaEgunak", faltak.getEgunak());
	        model.addAttribute("faltaEgunOrduak", faltak.getEgunekoOrduak());
	        model.addAttribute("faltakIkasleak", faltak.getIkasleRows());
	    }

	    // Egutegiko oharrak hartzeko:
	    Map<String, String> oharraMap = new HashMap<>();
	    for (EgunBerezi eb : egunBereziak) {
	        if (eb.getData() == null) continue;
	        if (eb.getDeskribapena() == null || eb.getDeskribapena().isBlank()) continue;
	        oharraMap.put(eb.getData().toString(), eb.getDeskribapena().trim());
	    }
	    model.addAttribute("oharraMap", oharraMap);

	    return "irakasleak/denboralizazioa/denboralizazioa";
	}

	private Map<String, Long> kalkulatuEgunekoLehenUdMap(Long koadernoId) {
	    if (koadernoId == null) return java.util.Collections.emptyMap();

	    var koadernoaOrdutegiarekin = koadernoaService.findByIdWithOrdutegiaAndEgutegia(koadernoId);
	    var programazioa = programazioaService.loadWithEbaluaketakUdetajpByKoadernoId(koadernoId);
	    if (koadernoaOrdutegiarekin.isEmpty() || programazioa.isEmpty()) {
	        return java.util.Collections.emptyMap();
	    }

	    try {
	        Map<String, Long> map = new java.util.LinkedHashMap<>();
	        denboralizazioGeneratorService
	                .generateFromProgramazioa(koadernoaOrdutegiarekin.get(), programazioa.get(), true, false)
	                .stream()
	                .filter(item -> item.data() != null && item.unitatea() != null && item.unitatea().getId() != null)
	                .forEach(item -> map.putIfAbsent(item.data().toString(), item.unitatea().getId()));
	        return map;
	    } catch (RuntimeException ex) {
	        return java.util.Collections.emptyMap();
	    }
	}

	@GetMapping("/falten-jakinarazpena/pdf")
	public ResponseEntity<?> deskargatuFaltenJakinarazpenaPdf(
	    @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoa,
	    @RequestParam("matrikulaId") Long matrikulaId,
	    @RequestParam("hilabetea") int hilabetea,
	    @RequestParam("urtea") int urtea,
	    Authentication auth) {

	    if (koadernoa == null || koadernoa.getId() == null) {
	        return ResponseEntity.badRequest().body(Map.of("error", "Ez dago koaderno aktiborik aukeratuta."));
	    }

	    var koadernoaOpt = koadernoaService.findByIdWithEgutegiaAndEgunBereziak(koadernoa.getId());
	    if (koadernoaOpt.isEmpty()) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Koaderno aktiboa ez da aurkitu."));
	    }

	    Koadernoa kargatutakoKoadernoa = koadernoaOpt.get();
	    FaltakBistaDTO faltak = denboralizazioFaltaService
	            .kalkulatuFaltenBista(kargatutakoKoadernoa, hilabetea, urtea);

	    var rowOpt = faltak.getIkasleRows().stream()
	            .filter(row -> row.getMatrikula() != null)
	            .filter(row -> matrikulaId.equals(row.getMatrikula().getId()))
	            .findFirst();

	    if (rowOpt.isEmpty()) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Ez da aurkitu matrikula."));
	    }

	    var row = rowOpt.get();
	    if (row.getFaltaPortzentaia() <= 20d) {
	        return ResponseEntity.badRequest().body(Map.of("error", "PDFa sortzeko faltak %20 baino handiagoa izan behar du."));
	    }

	    Irakaslea deskargatzailea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
	    LocalDate gaur = LocalDate.now();

	    String taldea = kargatutakoKoadernoa.getModuloa() != null && kargatutakoKoadernoa.getModuloa().getTaldea() != null
	        ? kargatutakoKoadernoa.getModuloa().getTaldea().getIzena() : "";
	    String modulua = kargatutakoKoadernoa.getModuloa() != null ? kargatutakoKoadernoa.getModuloa().getIzena() : "";
	    String deskargatzaileIzena = deskargatzailea != null ? deskargatzailea.getIzena() : "";

	    var data = FaltenJakinarazpenPdfService.FaltenJakinarazpenaData.of(
	        taldea,
	        row.getIkasleIzenOsoa(),
	        row.getFaltaOrduak(),
	        modulua,
	        row.getFaltaPortzentaia(),
	        urtea,
	        gaur,
	        deskargatzaileIzena
	    );

	    byte[] pdf;
	    try {
	        pdf = faltenJakinarazpenPdfService.sortuPdf(data);
	    } catch (IllegalStateException ex) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
	    }

	    auditDenbAction(auth, AuditEkintza.FALTEN_JAKINARAZPENA_DESKARGATU, koadernoa.getId(), "Matrikula", String.valueOf(matrikulaId));
    String fitxIzena = "falten-jakinarazpena-" + matrikulaId + "-" + urtea + "-" + hilabetea + ".pdf";
	    return ResponseEntity.ok()
	        .contentType(MediaType.APPLICATION_PDF)
	        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fitxIzena + "\"")
	        .body(pdf);
	}

	
	@PostMapping("/jarduera")
	public String gordeEdoEguneratu(
	    @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoa,
	    @ModelAttribute JardueraSortuDto dto,
	    @RequestParam("urtea") int urtea,
	    @RequestParam("hilabetea") int hilabetea,
	    @RequestParam(name="egutegiMota", required=false, defaultValue="hilabetea") String egutegiMota,
	    @RequestParam(name="asteHasiera", required=false) String asteHasiera,
	    @RequestParam(value = "id", required = false) Long id) {

	    if (koadernoa == null || koadernoa.getId() == null) {
	        throw new IllegalStateException("Koaderno aktiborik ez");
	    }

	    if (id == null) {
	        koadernoaService.gordeJarduera(koadernoa, dto);
	        auditDenbAction(null, AuditEkintza.JARDUERA_SORTU, koadernoa.getId(), "Jarduera", null);
	    } else {
	        koadernoaService.eguneratuJarduera(koadernoa, id, dto);
	        auditDenbAction(null, AuditEkintza.JARDUERA_EDITATU, koadernoa.getId(), "Jarduera", String.valueOf(id));
	    }

	    String redirect = "redirect:/irakasle/denboralizazioa?urtea=" + urtea + "&hilabetea=" + hilabetea + "&egutegiMota=" + egutegiMota;
	    if ("astea".equalsIgnoreCase(egutegiMota) && asteHasiera != null && !asteHasiera.isBlank()) {
	        redirect += "&asteHasiera=" + asteHasiera;
	    }
	    return redirect;
	}

	@PostMapping("/faltak/inportatu")
	public String inportatuFaltakExcel(
	        @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoa,
	        @RequestParam("fitxategia") MultipartFile fitxategia,
	        @RequestParam("urtea") int urtea,
	        @RequestParam("hilabetea") int hilabetea,
	        RedirectAttributes ra,
	        Authentication auth) {

	    if (koadernoa == null || koadernoa.getId() == null) {
	        ra.addFlashAttribute("error", "Ez dago koaderno aktiborik aukeratuta.");
	        return "redirect:/irakasle";
	    }

	    var koadernoaOpt = koadernoaService.findByIdWithEgutegiaAndEgunBereziak(koadernoa.getId());
	    if (koadernoaOpt.isEmpty()) {
	        ra.addFlashAttribute("error", "Koaderno aktiboa ez da aurkitu.");
	        return "redirect:/irakasle/denboralizazioa?urtea=" + urtea + "&hilabetea=" + hilabetea + "&bista=faltak";
	    }

	    try {
	        var emaitza = faltenExcelInportService.inportatu(koadernoaOpt.get(), fitxategia);
	        String mezua = String.format("Inportazioa eginda. Irakurrita: %d · Sortuta: %d · Baztertuta: %d",
	                emaitza.getIrakurritakoak(), emaitza.getSortuak(), emaitza.getBaztertuak());
	        ra.addFlashAttribute("success", mezua);
	        if (!emaitza.getOharrak().isEmpty()) {
	            ra.addFlashAttribute("error", String.join(" | ", emaitza.getOharrak().stream().limit(5).toList()));
	        }
	        gordeFaltenInportazioLoga(auth, koadernoaOpt.get(), emaitza, null);
	    } catch (Exception ex) {
	        ra.addFlashAttribute("error", "Ezin izan dira faltak inportatu: " + ex.getMessage());
	        gordeFaltenInportazioLoga(auth, koadernoaOpt.get(), null, ex.getMessage());
	    }

	    return "redirect:/irakasle/denboralizazioa?urtea=" + urtea + "&hilabetea=" + hilabetea + "&bista=faltak";
	}

	private void gordeFaltenInportazioLoga(Authentication auth,
	                                       Koadernoa koadernoa,
	                                       FaltenExcelInportService.InportEmaitza emaitza,
	                                       String errorea) {
	    Irakaslea eragilea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
	    String deskribapena;
	    if (emaitza != null) {
	        String oharrak = emaitza.getOharrak() == null || emaitza.getOharrak().isEmpty()
	                ? "oharrik ez"
	                : String.join(" | ", emaitza.getOharrak().stream().limit(3).toList());
	        deskribapena = String.format(
	                "Falten inportazioa: irakurrita=%d, sortuta=%d, baztertuta=%d, oharrak=%s",
	                emaitza.getIrakurritakoak(),
	                emaitza.getSortuak(),
	                emaitza.getBaztertuak(),
	                oharrak
	        );
	    } else {
	        deskribapena = "Falten inportazio errorea: " + (errorea == null ? "ezezaguna" : errorea);
	    }
	    if (deskribapena.length() > 2500) {
	        deskribapena = deskribapena.substring(0, 2500);
	    }

	    logService.gorde(
	            LogMota.HUTSEGITE_INPORTAZIOA,
	            eragilea,
	            "Koadernoa",
	            koadernoa != null ? koadernoa.getId() : null,
	            deskribapena
	    );
	}

	private boolean dagoTxantiloiAldia(Egutegia egutegia) {
	    if (egutegia == null || egutegia.getEgunBereziak() == null) return false;
	    List<LocalDate> lektiboak = egutegia.getEgunBereziak().stream()
	        .filter(eb -> eb.getData() != null)
	        .filter(eb -> eb.getMota() == EgunMota.LEKTIBOA || eb.getMota() == EgunMota.ORDEZKATUA)
	        .map(EgunBerezi::getData)
	        .distinct()
	        .sorted()
	        .toList();
	    if (lektiboak.isEmpty()) return false;
	    int index = Math.max(0, lektiboak.size() - 10);
	    LocalDate muga = lektiboak.get(index);
	    return !LocalDate.now().isBefore(muga);
	}

	@PostMapping("/txantiloiak/sortu")
	public String sortuTxantiloia(
	    @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoa,
	    @RequestParam("urtea") int urtea,
	    @RequestParam("hilabetea") int hilabetea,
	    @RequestParam(value = "izena", required = false) String izena,
	    Authentication auth,
	    RedirectAttributes ra) {

	    if (koadernoa == null || koadernoa.getId() == null) {
	        ra.addFlashAttribute("error", "Ez dago koaderno aktiborik aukeratuta.");
	        return "redirect:/irakasle";
	    }

	    Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
	    if (!koadernoaService.irakasleakBadaukaSarbidea(irakaslea, koadernoa)) {
	        ra.addFlashAttribute("error", "Ez duzu baimenik txantiloia sortzeko.");
	        return "redirect:/irakasle/denboralizazioa?urtea=" + urtea + "&hilabetea=" + hilabetea;
	    }

	    try {
	        var txantiloi = programazioTxantiloiService.sortuTxantiloiDenboralizaziotik(koadernoa, irakaslea, izena);
	        ra.addFlashAttribute("success", "Txantiloia gorde da: " + txantiloi.getIzena());
	    } catch (IllegalArgumentException ex) {
	        ra.addFlashAttribute("error", "Ezin izan da txantiloia sortu: " + ex.getMessage());
	    }

	    return "redirect:/irakasle/denboralizazioa?urtea=" + urtea + "&hilabetea=" + hilabetea;
	}
	
	
	@GetMapping(path="/jarduera/{id}",
	        produces=org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public org.springframework.http.ResponseEntity<?> lortuJarduera(
	    @PathVariable("id") Long id,
	    @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoa) {

	    if (koadernoa == null || koadernoa.getId() == null) {
	        return org.springframework.http.ResponseEntity.status(409)
	            .body(java.util.Map.of("error", "Koaderno aktiboa falta da"));
	    }

	    Jarduera j = koadernoaService.lortuJardueraKoadernoan(koadernoa, id);
	    if (j == null) {
	        return org.springframework.http.ResponseEntity.status(404)
	            .body(java.util.Map.of("error","Not found"));
	    }
	    return org.springframework.http.ResponseEntity.ok(JardueraEditDto.from(j));
	}
	
	@PostMapping("/jarduera/{id}/mugitu")
	@ResponseBody
	public ResponseEntity<?> mugituJarduera(
	        @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoa,
	        @PathVariable("id") Long id,
	        @RequestParam("data")
	        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataBerria) {

	    if (koadernoa == null || koadernoa.getId() == null) {
	        return ResponseEntity.status(HttpStatus.CONFLICT)
	                .body(Map.of("error", "Koaderno aktiboa falta da"));
	    }

	    try {
	        koadernoaService.aldatuJardueraData(koadernoa, id, dataBerria);
	        auditDenbAction(null, AuditEkintza.JARDUERA_MUGITU, koadernoa.getId(), "Jarduera", String.valueOf(id));
	        return ResponseEntity.ok(Map.of("ok", true));
	    } catch (IllegalArgumentException ex) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                .body(Map.of("error", ex.getMessage()));
	    }
	}
	
	
	@PostMapping("/jarduera/{id}/ezabatu")
	public String ezabatuJarduera(
	        @SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoa,
	        @PathVariable("id") Long id,
	        @RequestParam("urtea") int urtea,
	        @RequestParam("hilabetea") int hilabetea,
	        @RequestParam(name="egutegiMota", required=false, defaultValue="hilabetea") String egutegiMota,
	        @RequestParam(name="asteHasiera", required=false) String asteHasiera) {

	    if (koadernoa == null || koadernoa.getId() == null) {
	        throw new IllegalStateException("Koaderno aktiborik ez");
	    }

	    koadernoaService.ezabatuJarduera(koadernoa, id); // id koaderno horren barrukoa dela balidatu!
	    String redirect = "redirect:/irakasle/denboralizazioa?urtea=" + urtea + "&hilabetea=" + hilabetea + "&egutegiMota=" + egutegiMota;
	    if ("astea".equalsIgnoreCase(egutegiMota) && asteHasiera != null && !asteHasiera.isBlank()) {
	        redirect += "&asteHasiera=" + asteHasiera;
	    }
	    return redirect;
	}
	@PostMapping("/audit/asistentzia-pasatu")
	@ResponseBody
	public ResponseEntity<?> auditAsistentziaPasatu(@SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoa,
	                                               Authentication auth) {
	    if (koadernoa == null || koadernoa.getId() == null) return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("ok", false));
	    auditDenbAction(auth, AuditEkintza.ASISTENTZIA_PASATU, koadernoa.getId(), "Koadernoa", String.valueOf(koadernoa.getId()));
	    return ResponseEntity.ok(Map.of("ok", true));
	}

	@PostMapping("/audit/faltak-inport-modal")
	@ResponseBody
	public ResponseEntity<?> auditFaltakModal(@SessionAttribute(value = "koadernoAktiboa", required = false) Koadernoa koadernoa,
	                                          Authentication auth) {
	    if (koadernoa == null || koadernoa.getId() == null) return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("ok", false));
	    auditDenbAction(auth, AuditEkintza.FALTAK_HEZKUNTZATIK_INPORTATU, koadernoa.getId(), "Koadernoa", String.valueOf(koadernoa.getId()));
	    return ResponseEntity.ok(Map.of("ok", true));
	}

	private void auditDenbAction(Authentication auth, AuditEkintza ekintza, Long koadernoId, String entitateMota, String entitateId) {
	    Authentication a = auth != null ? auth : SecurityContextHolder.getContext().getAuthentication();
	    Irakaslea ir = null;
	    try { ir = irakasleaService.getLogeatutaDagoenIrakaslea(a); } catch (Exception ignored) {}
	    HttpServletRequest req = (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes sra) ? sra.getRequest() : null;
	    var e = auditService.buildBaseEvent(
	        ir != null ? ir.getId() : null,
	        ir != null ? ir.getEmaila() : (a != null ? a.getName() : null),
	        ir != null ? ir.getIzena() : (a != null ? a.getName() : null),
	        ir != null && ir.getRola() != null ? ir.getRola().name() : null,
	        req != null ? req.getRequestURI() : "/irakasle/denboralizazioa",
	        req != null ? req.getMethod() : "POST",
	        req != null ? req.getRemoteAddr() : null,
	        req != null ? req.getHeader("User-Agent") : null,
	        "Ekintza=" + ekintza,
	        AuditAtala.DENBORALIZAZIOA,
	        ekintza
	    );
	    e.setKoadernoId(koadernoId);
	    e.setEntitateMota(entitateMota);
	    e.setEntitateId(entitateId);
	    e.setArrakastatsua(true);
	    auditService.recordAction(e);
	}

}
