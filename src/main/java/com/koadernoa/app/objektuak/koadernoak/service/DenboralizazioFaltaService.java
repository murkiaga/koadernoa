package com.koadernoa.app.objektuak.koadernoak.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final KoadernoKlaseEgunService koadernoKlaseEgunService;

    /**
     * Matrikula bakoitzak bere koadernoan dituen hutsegiteen ehunekoa kalkulatzen du.
     * Falten bistaren irizpide bera erabiltzen da: HUTS eta JUSTIFIKATUA egoeren
     * orduak, koadernoan programatutako ikasturte osoko orduen gainean.
     */
    @Transactional(readOnly = true)
    public Map<Long, Double> kalkulatuHutsegitePortzentaiak(List<Matrikula> matrikulak) {
        if (matrikulak == null || matrikulak.isEmpty()) {
            return Map.of();
        }

        Map<Long, Double> emaitza = new LinkedHashMap<>();
        for (Matrikula matrikula : matrikulak) {
            if (matrikula == null || matrikula.getId() == null || matrikula.getKoadernoa() == null) {
                continue;
            }

            Koadernoa koadernoa = matrikula.getKoadernoa();
            int programaOrduak = kalkulatuProgramaOrduakUrteOsoan(koadernoa);
            if (programaOrduak <= 0) {
                emaitza.put(matrikula.getId(), 0.0);
                continue;
            }

            LocalDate gaur = LocalDate.now();
            LocalDate hasiera = koadernoa.getEgutegia() != null
                    ? koadernoa.getEgutegia().getHasieraData()
                    : null;
            LocalDate bukaera = koadernoa.getEgutegia() != null
                    ? koadernoa.getEgutegia().getBukaeraData()
                    : null;
            LocalDate noiztik = hasiera != null ? hasiera : LocalDate.MIN;
            LocalDate noizArte = bukaera != null && bukaera.isBefore(gaur) ? bukaera : gaur;

            List<Saioa> saioak = saioaRepository
                    .findByKoadernoaIdAndDataBetweenOrderByDataAscHasieraSlotAsc(
                            koadernoa.getId(), noiztik, noizArte);
            int hutsegiteOrduak = saioak.isEmpty() ? 0 : asistentziaRepository
                            .findBySaioaInAndMatrikulaIn(saioak, List.of(matrikula)).stream()
                            .filter(a -> a.getSaioa().getEgoera() != SaioEgoera.EZEZTATUA)
                            .filter(a -> a.getEgoera() == Asistentzia.AsistentziaEgoera.HUTS
                                    || a.getEgoera() == Asistentzia.AsistentziaEgoera.JUSTIFIKATUA)
                            .map(Asistentzia::getSaioa)
                            .filter(Objects::nonNull)
                            .mapToInt(Saioa::getIraupenaSlot)
                            .sum();

            emaitza.put(matrikula.getId(), hutsegiteOrduak * 100.0 / programaOrduak);
        }
        return emaitza;
    }

    @Transactional(readOnly = true)
    public FaltakBistaDTO kalkulatuFaltenBista(Koadernoa koadernoa, int hilabetea, int urtea) {
        return kalkulatuFaltenBista(koadernoa, hilabetea, urtea, null);
    }

    @Transactional(readOnly = true)
    public FaltakBistaDTO kalkulatuMatrikularenFaltenBista(
            Matrikula matrikula, int hilabetea, int urtea) {
        if (matrikula == null || matrikula.getKoadernoa() == null) {
            throw new IllegalArgumentException("Matrikulak koaderno bat izan behar du.");
        }
        return kalkulatuFaltenBista(
                matrikula.getKoadernoa(), hilabetea, urtea, List.of(matrikula));
    }

    private FaltakBistaDTO kalkulatuFaltenBista(
            Koadernoa koadernoa, int hilabetea, int urtea, List<Matrikula> mugatutakoMatrikulak) {

        Long koadernoId = koadernoa.getId();
        Egutegia egutegia = koadernoa.getEgutegia();

        YearMonth ym = YearMonth.of(urtea, hilabetea);
        LocalDate from = ym.atDay(1);
        LocalDate to   = ym.atEndOfMonth();

        // Ordutegi + Egutegia: hilabete honetako klase-egunak eta ordu kopurua
        List<KoadernoOrdutegiBlokea> blokak =
                koadernoOrdutegiBlokeaRepository.findByKoadernoa_Id(koadernoId);

        Map<LocalDate,Integer> egunekoOrduak =
                koadernoKlaseEgunService.egunekoSlotak(egutegia, blokak, from, to);

        List<LocalDate> egunak = new ArrayList<>(egunekoOrduak.keySet());

        // Programatutako ordu GUZTIAK (ikasturte osoan) → 2. puntuan komentatuko dugu
        int programaOrduak = koadernoKlaseEgunService.ikasturtekoKlaseSlotak(egutegia, blokak);

        // Koaderno honetako MATRIKULATUAK
        List<Matrikula> matrikulak = mugatutakoMatrikulak != null
                ? mugatutakoMatrikulak
                : matrikulaRepository.findByKoadernoaIdAndEgoeraMatrikulatuta(koadernoId);

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

    private int kalkulatuProgramaOrduakUrteOsoan(Koadernoa koadernoa) {
        if (koadernoa == null || koadernoa.getId() == null || koadernoa.getEgutegia() == null) {
            return 0;
        }

        List<KoadernoOrdutegiBlokea> blokeak =
                koadernoOrdutegiBlokeaRepository.findByKoadernoa_Id(koadernoa.getId());
        return koadernoKlaseEgunService.ikasturtekoKlaseSlotak(koadernoa.getEgutegia(), blokeak);
    }
}
