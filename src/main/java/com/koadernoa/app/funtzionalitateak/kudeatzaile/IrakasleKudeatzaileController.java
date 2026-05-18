package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.service.IkasturteaService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.IrakasleOrdutegiLerroa;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.IrakasleOrdutegia;
import com.koadernoa.app.objektuak.ordutegiak.repository.IrakasleOrdutegiLerroaRepository;
import com.koadernoa.app.objektuak.ordutegiak.repository.IrakasleOrdutegiaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;
import com.koadernoa.app.objektuak.zikloak.repository.FamiliaRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/kudeatzaile/irakasleak")
public class IrakasleKudeatzaileController {
	
	private final IrakasleaRepository irakasleaRepository;
	private final FamiliaRepository familiaRepository;
    private final IkasturteaService ikasturteaService;
    private final IrakasleOrdutegiaRepository irakasleOrdutegiaRepository;
    private final IrakasleOrdutegiLerroaRepository irakasleOrdutegiLerroaRepository;
    private final KoadernoaRepository koadernoaRepository;

    private static final List<Astegunak> ASTE_ORDENA = List.of(
            Astegunak.ASTELEHENA, Astegunak.ASTEARTEA, Astegunak.ASTEAZKENA,
            Astegunak.OSTEGUNA, Astegunak.OSTIRALA
    );
	
	@GetMapping({"","/"})
	public String zerrenda(Model model) {
	    List<Irakaslea> irakasleak = irakasleaRepository.findAll();
	    model.addAttribute("irakasleak", irakasleak);
	    model.addAttribute("familiaGuztiak", familiaRepository.findAll());
	    return "kudeatzaile/irakasleak/index";
	}

    @GetMapping("/{id}")
    public String fitxa(@PathVariable("id") Long id,
                        @RequestParam(name = "ikasturteaId", required = false) Long ikasturteaId,
                        Model model) {
        Irakaslea irakaslea = irakasleaRepository.findById(id).orElseThrow();
        List<Ikasturtea> ikasturteak = ikasturteaService.getAllOrderedDesc();
        Long selectedIkasturteaId = ikasturteaId != null
                ? ikasturteaId
                : ikasturteaService.getAktiboa().map(Ikasturtea::getId).orElse(null);

        IrakasleOrdutegia ordutegia = selectedIkasturteaId == null ? null
                : irakasleOrdutegiaRepository.findByIrakasleaIdAndIkasturteaId(id, selectedIkasturteaId).orElse(null);
        Map<String, List<OrdutegiGridEntry>> ordutegiGelaxkak = new LinkedHashMap<>();
        int azkenOrdua = 12;
        if (ordutegia != null && ordutegia.getLerroak() != null) {
            ordutegia.getLerroak().sort(Comparator
                    .comparingInt((IrakasleOrdutegiLerroa l) -> l.getAsteguna() != null ? l.getAsteguna().ordinal() : Integer.MAX_VALUE)
                    .thenComparingInt(l -> l.getOrduZenbakia() != null ? l.getOrduZenbakia() : Integer.MAX_VALUE));
            for (IrakasleOrdutegiLerroa lerroa : ordutegia.getLerroak()) {
                if (lerroa.getAsteguna() == null || lerroa.getOrduZenbakia() == null) {
                    continue;
                }
                if (!ASTE_ORDENA.contains(lerroa.getAsteguna())) {
                    continue;
                }
                int saioKopurua = lerroa.getSaioKopurua() != null && lerroa.getSaioKopurua() > 0 ? lerroa.getSaioKopurua() : 1;
                int bukaeraOrdua = lerroa.getOrduZenbakia() + saioKopurua - 1;
                azkenOrdua = Math.max(azkenOrdua, bukaeraOrdua);
                OrdutegiGridEntry edukia = ordutegiGridEntry(lerroa);
                for (int ordua = lerroa.getOrduZenbakia(); ordua <= bukaeraOrdua; ordua++) {
                    ordutegiGelaxkak.computeIfAbsent(ordutegiGelaxkaKey(lerroa.getAsteguna(), ordua), k -> new java.util.ArrayList<>()).add(edukia);
                }
            }
        }

        List<Koadernoa> koadernoak = koadernoaRepository.findByIrakasleaAndIkasturteaWithRelations(id, selectedIkasturteaId);

        model.addAttribute("irakaslea", irakaslea);
        model.addAttribute("ikasturteak", ikasturteak);
        model.addAttribute("ikasturteaId", selectedIkasturteaId);
        model.addAttribute("ordutegia", ordutegia);
        model.addAttribute("ordutegiGridRows", sortuOrdutegiGridRows(ordutegiGelaxkak, azkenOrdua));
        model.addAttribute("cols", ASTE_ORDENA);
        model.addAttribute("koadernoak", koadernoak);
        return "kudeatzaile/irakasleak/fitxa";
    }
	
	@PostMapping("/{id}/mintegia")
    public String aldatuMintegia(@PathVariable("id") Long id,
                                 @RequestParam("mintegia") Long familiaId) {
        eguneratuIrakaslea(id, familiaId);
        return "redirect:/kudeatzaile/irakasleak";
    }

    @PostMapping("/{id}/ordutegia/lerroa")
    public String sortuOrdutegiLerroa(@PathVariable("id") Long id,
                                      @RequestParam("ikasturteaId") Long ikasturteaId,
                                      @RequestParam("asteguna") Astegunak asteguna,
                                      @RequestParam("orduZenbakia") Integer orduZenbakia,
                                      @RequestParam(name = "moduluKodea", required = false) String moduluKodea,
                                      @RequestParam(name = "taldeKodea", required = false) String taldeKodea,
                                      @RequestParam(name = "gelaKodea", required = false) String gelaKodea,
                                      RedirectAttributes ra) {
        Irakaslea irakaslea = irakasleaRepository.findById(id).orElseThrow();
        IrakasleOrdutegia ordutegia = irakasleOrdutegiaRepository.findByIrakasleaIdAndIkasturteaId(id, ikasturteaId)
                .orElseGet(() -> sortuEskuzkoOrdutegia(irakaslea, ikasturteaId));

        IrakasleOrdutegiLerroa lerroa = new IrakasleOrdutegiLerroa();
        lerroa.setIrakasleOrdutegia(ordutegia);
        lerroa.setAsteguna(asteguna);
        lerroa.setOrduZenbakia(orduZenbakia);
        lerroa.setSaioKopurua(1);
        lerroa.setModuluKodea(normalizatuTestua(moduluKodea));
        lerroa.setTaldeKodea(normalizatuTestua(taldeKodea));
        lerroa.setGelaKodea(normalizatuTestua(gelaKodea));
        irakasleOrdutegiLerroaRepository.save(lerroa);
        ra.addFlashAttribute("success", "Ordutegi gelaxka sortu da.");
        return redirectIrakasleFitxara(id, ikasturteaId);
    }

    @PostMapping("/{id}/ordutegia/lerroa/{lerroaId}")
    public String eguneratuOrdutegiLerroa(@PathVariable("id") Long id,
                                          @PathVariable("lerroaId") Long lerroaId,
                                          @RequestParam(name = "ikasturteaId", required = false) Long ikasturteaId,
                                          @RequestParam(name = "moduluKodea", required = false) String moduluKodea,
                                          @RequestParam(name = "taldeKodea", required = false) String taldeKodea,
                                          @RequestParam(name = "gelaKodea", required = false) String gelaKodea,
                                          RedirectAttributes ra) {
        IrakasleOrdutegiLerroa lerroa = bilatuEtaEgiaztatuOrdutegiLerroa(id, lerroaId);
        lerroa.setModuluKodea(normalizatuTestua(moduluKodea));
        lerroa.setTaldeKodea(normalizatuTestua(taldeKodea));
        lerroa.setGelaKodea(normalizatuTestua(gelaKodea));
        irakasleOrdutegiLerroaRepository.save(lerroa);
        ra.addFlashAttribute("success", "Ordutegi gelaxka eguneratu da.");
        return redirectIrakasleFitxara(id, ikasturteaId);
    }

    @PostMapping("/{id}/ordutegia/lerroa/{lerroaId}/ezabatu")
    public String ezabatuOrdutegiLerroa(@PathVariable("id") Long id,
                                        @PathVariable("lerroaId") Long lerroaId,
                                        @RequestParam(name = "ikasturteaId", required = false) Long ikasturteaId,
                                        RedirectAttributes ra) {
        IrakasleOrdutegiLerroa lerroa = bilatuEtaEgiaztatuOrdutegiLerroa(id, lerroaId);
        irakasleOrdutegiLerroaRepository.delete(lerroa);
        ra.addFlashAttribute("success", "Ordutegi gelaxka ezabatu da.");
        return redirectIrakasleFitxara(id, ikasturteaId);
    }

    @PostMapping("/{id}/fitxa")
    public String eguneratuFitxa(@PathVariable("id") Long id,
                                 @RequestParam("mintegia") Long familiaId,
                                 @RequestParam(name = "ikasturteaId", required = false) Long ikasturteaId,
                                 RedirectAttributes ra) {
        eguneratuIrakaslea(id, familiaId);
        ra.addFlashAttribute("success", "Irakaslearen datuak eguneratu dira.");
        String redirect = "redirect:/kudeatzaile/irakasleak/" + id;
        return ikasturteaId != null ? redirect + "?ikasturteaId=" + ikasturteaId : redirect;
    }

    private IrakasleOrdutegia sortuEskuzkoOrdutegia(Irakaslea irakaslea, Long ikasturteaId) {
        Ikasturtea ikasturtea = ikasturteaService.getById(ikasturteaId);
        if (ikasturtea == null) {
            throw new IllegalArgumentException("Ikasturtea ez da aurkitu: " + ikasturteaId);
        }
        IrakasleOrdutegia ordutegia = new IrakasleOrdutegia();
        ordutegia.setIkasturtea(ikasturtea);
        ordutegia.setIrakaslea(irakaslea);
        ordutegia.setXmlIrakasleKodea(irakaslea.getEmaila());
        ordutegia.setXmlIrakasleIzena(irakaslea.getIzena());
        ordutegia.setJatorria("ESKUZ");
        ordutegia.setInportazioData(LocalDateTime.now());
        return irakasleOrdutegiaRepository.save(ordutegia);
    }

    private IrakasleOrdutegiLerroa bilatuEtaEgiaztatuOrdutegiLerroa(Long irakasleId, Long lerroaId) {
        IrakasleOrdutegiLerroa lerroa = irakasleOrdutegiLerroaRepository.findWithIrakasleOrdutegiaById(lerroaId).orElseThrow();
        if (lerroa.getIrakasleOrdutegia() == null
                || lerroa.getIrakasleOrdutegia().getIrakaslea() == null
                || !irakasleId.equals(lerroa.getIrakasleOrdutegia().getIrakaslea().getId())) {
            throw new IllegalArgumentException("Ordutegi lerroa ez dagokio irakasle honi");
        }
        return lerroa;
    }

    private String redirectIrakasleFitxara(Long id, Long ikasturteaId) {
        String redirect = "redirect:/kudeatzaile/irakasleak/" + id;
        return ikasturteaId != null ? redirect + "?ikasturteaId=" + ikasturteaId : redirect;
    }

    private void eguneratuIrakaslea(Long id, Long familiaId) {
        Irakaslea irakaslea = irakasleaRepository.findById(id).orElseThrow();
        Familia familia = familiaRepository.findById(familiaId).orElseThrow();
        irakaslea.setMintegia(familia);
        irakasleaRepository.save(irakaslea);
    }

    private String normalizatuTestua(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<OrdutegiGridRow> sortuOrdutegiGridRows(Map<String, List<OrdutegiGridEntry>> ordutegiGelaxkak, int azkenOrdua) {
        return IntStream.rangeClosed(1, azkenOrdua)
                .mapToObj(ordua -> new OrdutegiGridRow(ordua, ASTE_ORDENA.stream()
                        .map(asteguna -> new OrdutegiGridCell(asteguna,
                                ordutegiGelaxkak.getOrDefault(ordutegiGelaxkaKey(asteguna, ordua), List.of())))
                        .toList()))
                .toList();
    }

    private String ordutegiGelaxkaKey(Astegunak asteguna, int orduZenbakia) {
        return asteguna.name() + "-" + orduZenbakia;
    }

    private OrdutegiGridEntry ordutegiGridEntry(IrakasleOrdutegiLerroa lerroa) {
        String irakasgaia = irakasgaiTestua(lerroa);
        String taldea = lerroa.getTaldeKodea();
        String gela = gelaTestua(lerroa);
        String laburpena = java.util.stream.Stream.of(irakasgaia, taldea, gela)
                .filter(v -> v != null && !v.isBlank())
                .collect(java.util.stream.Collectors.joining(" · "));
        return new OrdutegiGridEntry(lerroa.getId(), irakasgaia, taldea, gela, laburpena);
    }

    private String irakasgaiTestua(IrakasleOrdutegiLerroa lerroa) {
        if (lerroa.getModuluKodea() != null && !lerroa.getModuluKodea().isBlank()) {
            return lerroa.getModuluKodea();
        }
        return lerroa.getModuluIzena();
    }

    private String gelaTestua(IrakasleOrdutegiLerroa lerroa) {
        if (lerroa.getGelaKodea() != null && !lerroa.getGelaKodea().isBlank()) {
            return lerroa.getGelaKodea();
        }
        return lerroa.getGelaIzena();
    }

    public record OrdutegiGridRow(Integer orduZenbakia, List<OrdutegiGridCell> gelaxkak) {
    }

    public record OrdutegiGridCell(Astegunak asteguna, List<OrdutegiGridEntry> edukiak) {
        public boolean beteta() {
            return edukiak != null && !edukiak.isEmpty();
        }
    }

    public record OrdutegiGridEntry(Long id, String irakasgaia, String taldea, String gela, String laburpena) {
    }
}
