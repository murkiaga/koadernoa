package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/kudeatzaile/api/taldeak")
@RequiredArgsConstructor
public class TaldeaApiController {

    private final TaldeaRepository taldeaRepository;

    @GetMapping("/bilatu")
    public List<TaldeaBilaketaItem> bilatu(@RequestParam(name = "q", defaultValue = "") String q,
                                           @RequestParam(name = "limit", defaultValue = "10") int limit) {
        if (!StringUtils.hasText(q) || q.trim().length() < 2) {
            return List.of();
        }

        int emaitzaMuga = Math.min(Math.max(limit, 1), 15);

        return taldeaRepository.bilatuAutocomplete(q.trim(), PageRequest.of(0, emaitzaMuga))
                .stream()
                .map(t -> new TaldeaBilaketaItem(
                        t.getId(),
                        t.getIzena(),
                        t.getZikloa() != null ? t.getZikloa().getIzena() : "-",
                        t.getZikloa() != null && t.getZikloa().getMaila() != null ? t.getZikloa().getMaila().name() : "-",
                        txandaEbatzi(t.getIzena())))
                .toList();
    }

    private String txandaEbatzi(String taldeIzena) {
        if (!StringUtils.hasText(taldeIzena)) {
            return "-";
        }
        String izena = taldeIzena.trim().toUpperCase();
        if (izena.endsWith("A")) {
            return "goiza";
        }
        if (izena.endsWith("B")) {
            return "arratsaldea";
        }
        return "-";
    }

    public record TaldeaBilaketaItem(Long id, String kodea, String zikloa, String maila, String txanda) {
    }
}
