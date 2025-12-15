package com.koadernoa.app.objektuak.koadernoak.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunMota;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Asistentzia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoOrdutegiBlokea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa.SaioEgoera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.denboralizazioa.FaltaIkasleRow;
import com.koadernoa.app.objektuak.koadernoak.entitateak.denboralizazioa.FaltakBistaDTO;
import com.koadernoa.app.objektuak.koadernoak.repository.AsistentziaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoOrdutegiBlokeaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.SaioaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DenboralizazioFaltaService {

    private final SaioaRepository saioaRepository;
    private final MatrikulaRepository matrikulaRepository;
    private final AsistentziaRepository asistentziaRepository;
    private final KoadernoOrdutegiBlokeaRepository koadernoOrdutegiBlokeaRepository;

    public FaltakBistaDTO kalkulatuFaltenBista(Koadernoa koadernoa, int hilabetea, int urtea) {

        Long koadernoId = koadernoa.getId();
        Egutegia egutegia = koadernoa.getEgutegia();

        YearMonth ym = YearMonth.of(urtea, hilabetea);
        LocalDate from = ym.atDay(1);
        LocalDate to   = ym.atEndOfMonth();

        // Ordutegi + Egutegia: hilabete honetako klase-egunak eta ordu kopurua
        List<KoadernoOrdutegiBlokea> blokak =
                koadernoOrdutegiBlokeaRepository.findByKoadernoa_Id(koadernoId);

        Map<Astegunak,Integer> orduakAstegunaka = blokak.stream()
                .collect(Collectors.groupingBy(
                        KoadernoOrdutegiBlokea::getAsteguna,
                        Collectors.summingInt(KoadernoOrdutegiBlokea::getIraupenaSlot)
                ));

        Map<LocalDate,EgunBerezi> bereziMap =
                egutegia.getEgunBereziak().stream()
                        .collect(Collectors.toMap(
                                EgunBerezi::getData,
                                eb -> eb,
                                (a,b) -> a
                        ));

        Map<LocalDate,Integer> egunekoOrduak =
                kalkulatuEgunekoOrduakHilabetean(egutegia, orduakAstegunaka, bereziMap, from, to);

        List<LocalDate> egunak = new ArrayList<>(egunekoOrduak.keySet());

        // Programatutako ordu GUZTIAK (ikasturte osoan) → 2. puntuan komentatuko dugu
        int programaOrduak = kalkulatuProgramaOrduakUrteOsoan(egutegia, orduakAstegunaka, bereziMap);

        // Koaderno honetako MATRIKULATUAK
        List<Matrikula> matrikulak =
                matrikulaRepository.findByKoadernoaIdAndEgoeraMatrikulatuta(koadernoId);

        Map<Long, FaltaIkasleRow> rowMap = new LinkedHashMap<>();
        for (Matrikula m : matrikulak) {
            FaltaIkasleRow row = new FaltaIkasleRow();
            row.setMatrikula(m);
            rowMap.put(m.getId(), row);
        }

        // asistentziak IKASTURTE HASIERATIK HILABETE HONEN BUKAERARA ARTE
        LocalDate ikastHasiera = egutegia.getHasieraData();
        LocalDate ikastBukaera = egutegia.getBukaeraData();

        // Totala kalkulatzeko tartea: IKASTURTE HASIERATIK GAUR ARTE
        LocalDate totalsFrom = (ikastHasiera != null) ? ikastHasiera : from;

        LocalDate orain = LocalDate.now();
        LocalDate totalsTo;

        // Ez joan gaurtik harago (ez dugu etorkizuneko faltarik kontatu nahi)
        if (ikastBukaera != null && ikastBukaera.isBefore(orain)) {
            totalsTo = ikastBukaera;
        } else {
            totalsTo = orain;
        }

        // Tarte horretan saio GUZTIAK
        List<Saioa> saioakDenboraOsoan =
                saioaRepository.findByKoadernoaIdAndDataBetweenOrderByDataAscHasieraSlotAsc(
                        koadernoId, totalsFrom, totalsTo);

        if (!saioakDenboraOsoan.isEmpty() && !matrikulak.isEmpty()) {
            List<Asistentzia> asistentziak =
                    asistentziaRepository.findBySaioaInAndMatrikulaIn(
                            saioakDenboraOsoan, matrikulak);

            for (Asistentzia a : asistentziak) {
                Saioa s = a.getSaioa();

                if (s.getEgoera() == SaioEgoera.EZEZTATUA) continue;

                LocalDate data = s.getData();
                FaltaIkasleRow row = rowMap.get(a.getMatrikula().getId());
                if (row == null) continue;

                int orduakSaio = s.getIraupenaSlot();

                // Hilabete honetako taulan agertu behar den eguna?
                boolean hilabetekoEguna = egunekoOrduak.containsKey(data);

                switch (a.getEgoera()) {
                    case HUTS, JUSTIFIKATUA -> {
                        String etiketa = hilabetekoEguna
                                ? String.valueOf(orduakSaio)
                                : null; // aurreko hilabeteak: totala bai, zelula ez
                        row.gehituFalta(data, orduakSaio, etiketa);
                    }
                    case BERANDU -> {
                        String etiketa = hilabetekoEguna ? "b" : null;
                        row.gehituFalta(data, 0, etiketa);
                    }
                    case ETORRI -> {
                        // ezer ez
                    }
                }
            }
        }

        // % kalkulua
        for (FaltaIkasleRow row : rowMap.values()) {
            double pct = programaOrduak == 0 ? 0
                    : row.getFaltaOrduak() * 100.0 / programaOrduak;
            row.setFaltaPortzentaia(pct);
        }

        FaltakBistaDTO dto = new FaltakBistaDTO();
        dto.setProgramaOrduak(programaOrduak);
        dto.setEgunak(egunak);
        dto.setEgunekoOrduak(egunekoOrduak);
        dto.setIkasleRows(new ArrayList<>(rowMap.values()));
        return dto;
    }

    /** Hilabete jakin bateko egunak + ordu kopurua, Egutegia + ordutegia kontuan hartuta */
    private Map<LocalDate,Integer> kalkulatuEgunekoOrduakHilabetean(
            Egutegia egutegia,
            Map<Astegunak,Integer> orduakAstegunaka,
            Map<LocalDate,EgunBerezi> bereziMap,
            LocalDate from,
            LocalDate to) {

        LocalDate ikastHasiera = egutegia.getHasieraData();
        LocalDate ikastBukaera = egutegia.getBukaeraData();

        Map<LocalDate,Integer> ema = new LinkedHashMap<>();

        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            // Ikasturtearen barruan ez badago, salto
            if (ikastHasiera != null && d.isBefore(ikastHasiera)) continue;
            if (ikastBukaera != null && d.isAfter(ikastBukaera)) continue;

            Astegunak ag = astegunEraginkorra(d, bereziMap);
            if (ag == null) continue;

            int ordu = orduakAstegunaka.getOrDefault(ag, 0);
            if (ordu > 0) {
                ema.put(d, ordu);
            }
        }

        return ema;
    }

    /** Ikasturte osoan programan dauden orduak (3 ebaluazio guztiak) */
    private int kalkulatuProgramaOrduakUrteOsoan(
            Egutegia egutegia,
            Map<Astegunak,Integer> orduakAstegunaka,
            Map<LocalDate,EgunBerezi> bereziMap) {

        LocalDate ikastHasiera = egutegia.getHasieraData();
        LocalDate ikastBukaera = egutegia.getBukaeraData();

        if (ikastHasiera == null || ikastBukaera == null) return 0;

        int total = 0;
        for (LocalDate d = ikastHasiera; !d.isAfter(ikastBukaera); d = d.plusDays(1)) {
            Astegunak ag = astegunEraginkorra(d, bereziMap);
            if (ag == null) continue;

            total += orduakAstegunaka.getOrDefault(ag, 0);
        }
        return total;
    }

    /**
     * Egun jakin bateko "asteguna eraginkorra":
     *  - Egun bereziak kontuan: EZ_LEKTIBOA/JAIEGUNA -> null (klaserik ez)
     *  - ORDEZKATUA -> ordezkatua eremuak esaten duen asteguna
     *  - Bestela: asteguna = d.getDayOfWeek (larunbata/igandea -> null)
     */
    private Astegunak astegunEraginkorra(LocalDate d, Map<LocalDate,EgunBerezi> bereziMap) {
        EgunBerezi eb = bereziMap.get(d);
        if (eb != null) {
            EgunMota mota = eb.getMota();
            switch (mota) {
                case EZ_LEKTIBOA, JAIEGUNA:
                    return null;
                case ORDEZKATUA:
                    return eb.getOrdezkatua(); // astelehena...ostirala (izan beharko luke)
                case LEKTIBOA:
                    // jarraitu beheko logikarekin (normal bezala)
                    break;
            }
        }

        DayOfWeek dow = d.getDayOfWeek();
        return switch (dow) {
            case MONDAY    -> Astegunak.ASTELEHENA;
            case TUESDAY   -> Astegunak.ASTEARTEA;
            case WEDNESDAY -> Astegunak.ASTEAZKENA;
            case THURSDAY  -> Astegunak.OSTEGUNA;
            case FRIDAY    -> Astegunak.OSTIRALA;
            default        -> null; // larunbata/igandea
        };
    }
}
