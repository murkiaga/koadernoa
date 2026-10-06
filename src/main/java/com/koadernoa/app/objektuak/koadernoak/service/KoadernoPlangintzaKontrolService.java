package com.koadernoa.app.objektuak.koadernoak.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.service.IkasturteaService;
import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoOrdutegiBlokea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.repository.JardueraRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoOrdutegiBlokeaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoPlangintzaKontrola.Egoera;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoPlangintzaKontrola.PlangintzaEgunKontrola;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KoadernoPlangintzaKontrolService {

    private final IkasturteaService ikasturteaService;
    private final KoadernoaRepository koadernoaRepository;
    private final JardueraRepository jardueraRepository;
    private final KoadernoOrdutegiBlokeaRepository ordutegiBlokeaRepository;
    private final KoadernoKlaseEgunService klaseEgunService;
    private final AplikazioAukeraService aplikazioAukeraService;

    @Transactional(readOnly = true)
    public List<KoadernoPlangintzaKontrola> lortuKontrola() {
        return lortuKontrola(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<KoadernoPlangintzaKontrola> lortuKontrola(LocalDate gaur) {
        Ikasturtea ikasturtea = ikasturteaService.getAktiboa().orElse(null);
        if (ikasturtea == null || ikasturtea.getId() == null) {
            return List.of();
        }

        List<Koadernoa> koadernoak = koadernoaRepository
                .findByIkasturteaIdWithPlangintzaKontrolRelations(ikasturtea.getId());
        Map<Long, List<KoadernoOrdutegiBlokea>> blokeak = ordutegiBlokeaRepository
                .findByIkasturteaId(ikasturtea.getId()).stream()
                .collect(Collectors.groupingBy(b -> b.getKoadernoa().getId()));

        Map<Long, List<LocalDate>> kontrolEgunak = new HashMap<>();
        LocalDate azkenKontrolData = null;
        int lanegunak = aplikazioAukeraService.getPlangintzaKontrolLanegunak();
        for (Koadernoa koadernoa : koadernoak) {
            List<LocalDate> lanEgunenLeihoa = klaseEgunService.hurrengoLanEgunak(
                    koadernoa.getEgutegia(), gaur, lanegunak);
            List<LocalDate> egunak = lanEgunenLeihoa.isEmpty()
                    ? List.of()
                    : List.copyOf(klaseEgunService.egunekoSlotak(
                            koadernoa.getEgutegia(), blokeak.getOrDefault(koadernoa.getId(), List.of()),
                            lanEgunenLeihoa.get(0), lanEgunenLeihoa.get(lanEgunenLeihoa.size() - 1)).keySet());
            kontrolEgunak.put(koadernoa.getId(), egunak);
            if (!egunak.isEmpty()) {
                LocalDate azkena = egunak.get(egunak.size() - 1);
                if (azkenKontrolData == null || azkena.isAfter(azkenKontrolData)) azkenKontrolData = azkena;
            }
        }

        Map<Long, Set<LocalDate>> jardueraDatak = azkenKontrolData == null
                ? Map.of()
                : jardueraDatenMapa(jardueraRepository.findJardueraDatak(
                        koadernoak.stream().map(Koadernoa::getId).toList(), gaur, azkenKontrolData));

        return koadernoak.stream().map(koadernoa -> sortuKontrola(
                koadernoa,
                kontrolEgunak.getOrDefault(koadernoa.getId(), List.of()),
                jardueraDatak.getOrDefault(koadernoa.getId(), Set.of())))
                .toList();
    }

    public int getKontrolLanegunak() {
        return aplikazioAukeraService.getPlangintzaKontrolLanegunak();
    }

    public long abisuHartzaileKopurua(List<KoadernoPlangintzaKontrola> kontrolak) {
        Set<Long> hartzaileak = new LinkedHashSet<>();
        kontrolak.stream()
                .filter(KoadernoPlangintzaKontrola::abisuaBeharDu)
                .map(KoadernoPlangintzaKontrola::koadernoa)
                .map(Koadernoa::getIrakasleak)
                .filter(java.util.Objects::nonNull)
                .flatMap(List::stream)
                .filter(java.util.Objects::nonNull)
                .map(Irakaslea::getId)
                .filter(java.util.Objects::nonNull)
                .forEach(hartzaileak::add);
        return hartzaileak.size();
    }

    public List<String> abisuHartzaileEmailak(List<KoadernoPlangintzaKontrola> kontrolak) {
        Map<String, String> emailak = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        kontrolak.stream()
                .filter(KoadernoPlangintzaKontrola::abisuaBeharDu)
                .map(KoadernoPlangintzaKontrola::koadernoa)
                .map(Koadernoa::getIrakasleak)
                .filter(java.util.Objects::nonNull)
                .flatMap(List::stream)
                .filter(java.util.Objects::nonNull)
                .map(Irakaslea::getEmaila)
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(email -> !email.isBlank())
                .forEach(email -> emailak.putIfAbsent(email, email));
        return List.copyOf(emailak.values());
    }

    private KoadernoPlangintzaKontrola sortuKontrola(
            Koadernoa koadernoa, List<LocalDate> kontrolEgunak, Set<LocalDate> jardueraDatak) {
        List<PlangintzaEgunKontrola> egunak = kontrolEgunak.stream()
                .map(data -> new PlangintzaEgunKontrola(data, jardueraDatak.contains(data)))
                .toList();
        int planifikatutakoak = (int) egunak.stream().filter(PlangintzaEgunKontrola::jardueraDu).count();
        Egoera egoera = egunak.isEmpty()
                ? Egoera.KLASE_EGUNIK_EZ
                : planifikatutakoak == egunak.size() ? Egoera.ONDO : Egoera.OSATU_GABE;
        return new KoadernoPlangintzaKontrola(
                koadernoa, egunak, planifikatutakoak, egunak.size(), egoera);
    }

    private Map<Long, Set<LocalDate>> jardueraDatenMapa(List<Object[]> emaitzak) {
        Map<Long, Set<LocalDate>> datak = new HashMap<>();
        for (Object[] emaitza : emaitzak) {
            if (emaitza[0] instanceof Number koadernoId && emaitza[1] instanceof LocalDate data) {
                datak.computeIfAbsent(koadernoId.longValue(), __ -> new HashSet<>()).add(data);
            }
        }
        return datak;
    }
}
