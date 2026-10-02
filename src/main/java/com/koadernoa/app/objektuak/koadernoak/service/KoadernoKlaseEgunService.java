package com.koadernoa.app.objektuak.koadernoak.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunMota;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoOrdutegiBlokea;

@Service
public class KoadernoKlaseEgunService {

    public List<LocalDate> hurrengoKlaseEgunak(
            Egutegia egutegia, List<KoadernoOrdutegiBlokea> blokeak, LocalDate hasiera, int gehienez) {
        if (egutegia == null || egutegia.getBukaeraData() == null || hasiera == null || gehienez <= 0) {
            return List.of();
        }
        LocalDate noiztik = egutegia.getHasieraData() != null && hasiera.isBefore(egutegia.getHasieraData())
                ? egutegia.getHasieraData() : hasiera;
        if (noiztik.isAfter(egutegia.getBukaeraData())) return List.of();

        List<LocalDate> emaitza = new ArrayList<>();
        Map<LocalDate, Integer> slotak = egunekoSlotak(
                egutegia, blokeak, noiztik, egutegia.getBukaeraData());
        for (LocalDate data : slotak.keySet()) {
            emaitza.add(data);
            if (emaitza.size() == gehienez) break;
        }
        return List.copyOf(emaitza);
    }

    public Map<LocalDate, Integer> egunekoSlotak(
            Egutegia egutegia, List<KoadernoOrdutegiBlokea> blokeak, LocalDate hasiera, LocalDate bukaera) {
        if (egutegia == null || hasiera == null || bukaera == null || hasiera.isAfter(bukaera)) return Map.of();
        LocalDate noiztik = egutegia.getHasieraData() != null && hasiera.isBefore(egutegia.getHasieraData())
                ? egutegia.getHasieraData() : hasiera;
        LocalDate noizArte = egutegia.getBukaeraData() != null && bukaera.isAfter(egutegia.getBukaeraData())
                ? egutegia.getBukaeraData() : bukaera;
        if (noiztik.isAfter(noizArte)) return Map.of();

        NavigableMap<LocalDate, Map<Astegunak, Integer>> ordutegiak = prestatuOrdutegiak(egutegia, blokeak);
        Map<LocalDate, EgunBerezi> egunBereziak = prestatuEgunBereziak(egutegia);
        Map<LocalDate, Integer> emaitza = new LinkedHashMap<>();
        for (LocalDate data = noiztik; !data.isAfter(noizArte); data = data.plusDays(1)) {
            Astegunak asteguna = astegunEraginkorra(data, egunBereziak);
            if (asteguna == null) continue;
            Map.Entry<LocalDate, Map<Astegunak, Integer>> ordutegia = ordutegiak.floorEntry(data);
            int slotak = ordutegia == null ? 0 : ordutegia.getValue().getOrDefault(asteguna, 0);
            if (slotak > 0) emaitza.put(data, slotak);
        }
        return emaitza;
    }

    public int ikasturtekoKlaseSlotak(Egutegia egutegia, List<KoadernoOrdutegiBlokea> blokeak) {
        if (egutegia == null || egutegia.getHasieraData() == null || egutegia.getBukaeraData() == null) return 0;
        return egunekoSlotak(egutegia, blokeak, egutegia.getHasieraData(), egutegia.getBukaeraData())
                .values().stream().mapToInt(Integer::intValue).sum();
    }

    private NavigableMap<LocalDate, Map<Astegunak, Integer>> prestatuOrdutegiak(
            Egutegia egutegia, List<KoadernoOrdutegiBlokea> blokeak) {
        NavigableMap<LocalDate, Map<Astegunak, Integer>> ordutegiak = new TreeMap<>();
        LocalDate ikasturteHasiera = egutegia.getHasieraData();
        for (KoadernoOrdutegiBlokea blokea : Optional.ofNullable(blokeak).orElse(List.of())) {
            if (blokea == null) continue;
            LocalDate hasiera = blokea.getHasieraData() != null ? blokea.getHasieraData() : ikasturteHasiera;
            if (hasiera == null) continue;
            if (blokea.isTarteHutsa()) {
                ordutegiak.putIfAbsent(hasiera, new EnumMap<>(Astegunak.class));
                continue;
            }
            if (blokea.isDualOrdutegia()) continue;
            if (blokea.getAsteguna() == null || blokea.getIraupenaSlot() <= 0) continue;
            ordutegiak.computeIfAbsent(hasiera, __ -> new EnumMap<>(Astegunak.class))
                    .merge(blokea.getAsteguna(), blokea.getIraupenaSlot(), Integer::sum);
        }
        return ordutegiak;
    }

    private Map<LocalDate, EgunBerezi> prestatuEgunBereziak(Egutegia egutegia) {
        return Optional.ofNullable(egutegia.getEgunBereziak()).orElse(List.of()).stream()
                .filter(eguna -> eguna != null && eguna.getData() != null)
                .collect(Collectors.toMap(EgunBerezi::getData, eguna -> eguna, (lehena, bigarrena) -> lehena));
    }

    private Astegunak astegunEraginkorra(LocalDate data, Map<LocalDate, EgunBerezi> egunBereziak) {
        EgunBerezi egunBerezia = egunBereziak.get(data);
        if (egunBerezia != null && egunBerezia.getMota() != null) {
            EgunMota mota = egunBerezia.getMota();
            if (mota == EgunMota.EZ_LEKTIBOA || mota == EgunMota.JAIEGUNA) return null;
            if (mota == EgunMota.ORDEZKATUA) return egunBerezia.getOrdezkatua();
        }
        return asteguna(data.getDayOfWeek());
    }

    private Astegunak asteguna(DayOfWeek asteguna) {
        return switch (asteguna) {
            case MONDAY -> Astegunak.ASTELEHENA;
            case TUESDAY -> Astegunak.ASTEARTEA;
            case WEDNESDAY -> Astegunak.ASTEAZKENA;
            case THURSDAY -> Astegunak.OSTEGUNA;
            case FRIDAY -> Astegunak.OSTIRALA;
            case SATURDAY, SUNDAY -> null;
        };
    }
}
