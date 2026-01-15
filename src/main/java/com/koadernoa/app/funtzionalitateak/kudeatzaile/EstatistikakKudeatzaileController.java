package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import com.koadernoa.app.objektuak.koadernoak.service.EstatistikakKudeatzaileService;
import com.koadernoa.app.funtzionalitateak.kudeatzaile.EstatistikaDashboard.*;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EstatistikaEbaluazioan;
import com.koadernoa.app.objektuak.koadernoak.repository.EstatistikaEbaluazioanRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.projection.EbaluazioKodeKopuruaProjection;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/kudeatzaile/estatistikak")
@RequiredArgsConstructor
public class EstatistikakKudeatzaileController {

    private final EstatistikakKudeatzaileService estatistikakService;
    private final EstatistikaEbaluazioanRepository estatistikaEbaluazioanRepository;

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
}
