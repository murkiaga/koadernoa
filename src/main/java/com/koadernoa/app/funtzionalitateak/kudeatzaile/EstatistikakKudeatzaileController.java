package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import com.koadernoa.app.objektuak.koadernoak.service.EstatistikakKudeatzaileService;
import com.koadernoa.app.funtzionalitateak.kudeatzaile.EstatistikaDashboard.*;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EstatistikaEbaluazioan;
import com.koadernoa.app.objektuak.koadernoak.repository.EstatistikaEbaluazioanRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.EzadostasunFitxaRepository;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EzadostasunFitxa;
import com.koadernoa.app.objektuak.koadernoak.repository.projection.EbaluazioKodeKopuruaProjection;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.http.MediaType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Controller
@RequestMapping("/kudeatzaile/estatistikak")
@RequiredArgsConstructor
public class EstatistikakKudeatzaileController {

    private final EstatistikakKudeatzaileService estatistikakService;
    private final EstatistikaEbaluazioanRepository estatistikaEbaluazioanRepository;
    private final EzadostasunFitxaRepository ezadostasunFitxaRepository;

    @GetMapping
    public String index(
            @ModelAttribute("filtro") EstatistikakFiltroa filtro,
            @PageableDefault(size = 20, sort = {"ebaluazioMomentua.ordena","id"}, direction = Sort.Direction.ASC) Pageable pageable,
            Model model,
            HttpServletRequest request
    ) {
    	
        model.addAttribute("currentPath", "/kudeatzaile/estatistikak");

        // Sort egonkorra: erabiltzaileak zutabe batez ordenatzen badu, id gehitu atzetik
        Sort s = pageable.getSort();
        if (s.isSorted() && s.getOrderFor("id") == null) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), s.and(Sort.by("id")));
            s = pageable.getSort();
        }

        String sortBy  = s.isSorted() ? s.iterator().next().getProperty() : "ebaluazioMomentua.ordena";
        String sortDir = s.isSorted() ? s.iterator().next().getDirection().name().toLowerCase() : "asc";
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        
        // Dashboard: ebaluazio-kode bakoitzeko "bete gabe" kopurua (filtroekin ere bai)
        List<EbaluazioKodeKopuruaProjection> beteGabeakKodez =
                estatistikakService.beteGabeakEbaluazioKodez(filtro);
        model.addAttribute("beteGabeakKodez", beteGabeakKodez);

        // Orrikatutako zerrenda (defektuz: filtroaren arabera; nahi baduzu defektuz "kalkulatu gabe" jarri filtro.kalkulatua=false hasieratik)
        Page<EstatistikaEbaluazioan> orria = estatistikakService.bilatuOrrikatuta(filtro, pageable);
        model.addAttribute("orria", orria);

        // Dropdown-entzako datuak (zure service propioetara egokitu)
        model.addAttribute("familiaList", estatistikakService.lortuFamiliak(filtro));
        model.addAttribute("zikloaList", estatistikakService.lortuZikloak(filtro));
        model.addAttribute("taldeaList", estatistikakService.lortuTaldeak(filtro));
        model.addAttribute("mailaList", estatistikakService.lortuMailak(filtro));
        model.addAttribute("ebaluazioKodeList", estatistikakService.lortuEbaluazioKodeak(filtro));

        // Querystring mantentzeko laguntza (aukerakoa)
        model.addAttribute("queryString", request.getQueryString() == null ? "" : request.getQueryString());

        return "kudeatzaile/estatistikak/index";
    }

    @GetMapping("/{estatId}/ezadostasuna")
    public String ezadostasunFitxa(
            @PathVariable Long estatId,
            Model model,
            RedirectAttributes ra) {

        EstatistikaEbaluazioan estatistika =
                estatistikaEbaluazioanRepository.findById(estatId).orElse(null);
        if (estatistika == null) {
            ra.addFlashAttribute("error", "Estatistika ez da aurkitu.");
            return "redirect:/kudeatzaile/estatistikak";
        }

        EzadostasunFitxa fitxa = ezadostasunFitxaRepository.findByEstatistikaId(estatId).orElse(null);
        List<String> ezadostasunak = kalkulatuEzadostasunak(estatistika);

        model.addAttribute("estatistika", estatistika);
        model.addAttribute("fitxa", fitxa);
        model.addAttribute("ezadostasunak", ezadostasunak);
        model.addAttribute("ezadostasunFitxaMezua",
                (fitxa == null && !ezadostasunak.isEmpty())
                        ? "Irakasleak ez du ezadostasun fitxa bete."
                        : null);

        return "kudeatzaile/estatistikak/ezadostasun-fitxa";
    }
    
    
    /************************** CSV Deskargatzeko *******************************/
    @GetMapping(value = "/csv", produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> exportCsv(
            @ModelAttribute("filtro") EstatistikakFiltroa filtro,
            @RequestParam(name = "sort", required = false) String sortParam
    ) {
        Sort sort = parseSortOrDefault(sortParam);

        StreamingResponseBody body = outputStream -> {
            // UTF-8 + Excel-friendly BOM (Excel-ek ondo ireki dezan euskarazko karaktereekin)
            outputStream.write(new byte[] {(byte)0xEF, (byte)0xBB, (byte)0xBF});

            estatistikakService.exportCsv(filtro, sort, outputStream);
        };

        String filename = "estatistikak.csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    /**
     * UI-tik datozen sort field-ak whitelisteatu (seguruagoa + akats gutxiago).
     * sort param formatua: "field,asc" edo "field,desc"
     */
    private Sort parseSortOrDefault(String sortParam) {
        String field = "id";
        Sort.Direction dir = Sort.Direction.ASC;

        if (sortParam != null && !sortParam.isBlank()) {
            String[] parts = sortParam.split(",");
            if (parts.length >= 1 && !parts[0].isBlank()) field = parts[0].trim();
            if (parts.length >= 2) {
                String d = parts[1].trim().toLowerCase();
                if ("desc".equals(d)) dir = Sort.Direction.DESC;
            }
        }

        // ONARTUTAKO eremuak bakarrik (zure goiburuetan erabiltzen dituzunak)
        Set<String> allowed = Set.of(
                "id",
                "ebaluazioMomentua.ordena",
                "koadernoa.moduloa.taldea.izena",
                "koadernoa.moduloa.izena",
                "koadernoa.egutegia.maila.ordena",
                "kalkulatua",
                "azkenKalkulua"
        );

        if (!allowed.contains(field)) field = "id";

        return Sort.by(dir, field);
    }

    private List<String> kalkulatuEzadostasunak(EstatistikaEbaluazioan estatistika) {
        List<String> emaitza = new ArrayList<>();
        if (estatistika == null || estatistika.getEbaluazioMomentua() == null ||
                estatistika.getEbaluazioMomentua().getEzadostasunKonfig() == null) {
            return emaitza;
        }

        com.koadernoa.app.objektuak.ebaluazioa.entitateak.EzadostasunKonfig konfig =
                estatistika.getEbaluazioMomentua().getEzadostasunKonfig();

        if (estatistika.getUdPortzentaia() != null &&
                estatistika.getUdPortzentaia() < konfig.getMinBlokePortzentaia()) {
            emaitza.add("UD-ak emanda < %" + konfig.getMinBlokePortzentaia());
        }
        if (estatistika.getOrduPortzentaia() != null &&
                estatistika.getOrduPortzentaia() < konfig.getMinOrduPortzentaia()) {
            emaitza.add("Orduak emanda < %" + konfig.getMinOrduPortzentaia());
        }
        if (estatistika.getGaindituPortzentaia() != null &&
                estatistika.getGaindituPortzentaia() < konfig.getMinGaindituPortzentaia()) {
            emaitza.add("Gainditu duten ikasleak < %" + konfig.getMinGaindituPortzentaia());
        }
        if (estatistika.getBertaratzePortzentaia() != null &&
                estatistika.getBertaratzePortzentaia() < konfig.getMinBertaratzePortzentaia()) {
            emaitza.add("Ikasleen bertaratzea < %" + konfig.getMinBertaratzePortzentaia());
        }
        return emaitza;
    }
}
