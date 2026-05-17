package com.koadernoa.app.funtzionalitateak.kudeatzaile;

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
        Map<String, List<String>> ordutegiGelaxkak = new LinkedHashMap<>();
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
                String edukia = ordutegiGelaxkaTestua(lerroa);
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
                                 @RequestParam("mintegia") Long familiaId,
                                 @RequestParam(name = "ordutegiKodea", required = false) String ordutegiKodea) {
        eguneratuIrakaslea(id, familiaId, ordutegiKodea);
        return "redirect:/kudeatzaile/irakasleak";
    }

    @PostMapping("/{id}/fitxa")
    public String eguneratuFitxa(@PathVariable("id") Long id,
                                 @RequestParam("mintegia") Long familiaId,
                                 @RequestParam(name = "ordutegiKodea", required = false) String ordutegiKodea,
                                 @RequestParam(name = "ikasturteaId", required = false) Long ikasturteaId,
                                 RedirectAttributes ra) {
        eguneratuIrakaslea(id, familiaId, ordutegiKodea);
        ra.addFlashAttribute("success", "Irakaslearen datuak eguneratu dira.");
        String redirect = "redirect:/kudeatzaile/irakasleak/" + id;
        return ikasturteaId != null ? redirect + "?ikasturteaId=" + ikasturteaId : redirect;
    }

    private void eguneratuIrakaslea(Long id, Long familiaId, String ordutegiKodea) {
        Irakaslea irakaslea = irakasleaRepository.findById(id).orElseThrow();
        Familia familia = familiaRepository.findById(familiaId).orElseThrow();
        irakaslea.setMintegia(familia);
        irakaslea.setOrdutegiKodea(ordutegiKodea == null || ordutegiKodea.isBlank() ? null : ordutegiKodea.trim());
        irakasleaRepository.save(irakaslea);
    }

    private List<OrdutegiGridRow> sortuOrdutegiGridRows(Map<String, List<String>> ordutegiGelaxkak, int azkenOrdua) {
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

    private String ordutegiGelaxkaTestua(IrakasleOrdutegiLerroa lerroa) {
        return java.util.stream.Stream.of(irakasgaiTestua(lerroa), lerroa.getTaldeKodea(), gelaTestua(lerroa))
                .filter(v -> v != null && !v.isBlank())
                .collect(java.util.stream.Collectors.joining(" · "));
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

    public record OrdutegiGridCell(Astegunak asteguna, List<String> edukiak) {
        public boolean beteta() {
            return edukiak != null && !edukiak.isEmpty();
        }
    }
}
