package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunMota;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoOrdutegiBlokea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Asistentzia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.denboralizazioa.FaltakBistaDTO;
import com.koadernoa.app.objektuak.koadernoak.repository.AsistentziaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoOrdutegiBlokeaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.SaioaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.DenboralizazioFaltaService;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoKlaseEgunService;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;

class DenboralizazioFaltaServiceRegressionTest {

    private final SaioaRepository saioaRepository = mock(SaioaRepository.class);
    private final MatrikulaRepository matrikulaRepository = mock(MatrikulaRepository.class);
    private final AsistentziaRepository asistentziaRepository = mock(AsistentziaRepository.class);
    private final KoadernoOrdutegiBlokeaRepository ordutegiRepository =
            mock(KoadernoOrdutegiBlokeaRepository.class);
    private final DenboralizazioFaltaService service = new DenboralizazioFaltaService(
            saioaRepository, matrikulaRepository, asistentziaRepository, ordutegiRepository,
            new KoadernoKlaseEgunService());

    @Test
    void dualOrdutegiaBaztertuEtaAsteburuakEzDituZenbatzenMainBezala() {
        Koadernoa koadernoa = koadernoa(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), List.of());
        List<KoadernoOrdutegiBlokea> blokeak = List.of(
                blokea(LocalDate.of(2026, 10, 1), Astegunak.ASTELEHENA, 2),
                blokea(LocalDate.of(2026, 10, 1), Astegunak.LARUNBATA, 3),
                duala(LocalDate.of(2026, 10, 12)));
        prestatu(koadernoa, blokeak);

        FaltakBistaDTO dto = service.kalkulatuFaltenBista(koadernoa, 10, 2026);

        assertThat(dto.getEgunekoOrduak()).containsExactly(
                entry(LocalDate.of(2026, 10, 5), 2),
                entry(LocalDate.of(2026, 10, 12), 2),
                entry(LocalDate.of(2026, 10, 19), 2),
                entry(LocalDate.of(2026, 10, 26), 2));
        assertThat(dto.getProgramaOrduak()).isEqualTo(8);
    }

    @Test
    void tarteHutsakOrdutegiaEtetenDuMainBezala() {
        Koadernoa koadernoa = koadernoa(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), List.of());
        prestatu(koadernoa, List.of(
                blokea(LocalDate.of(2026, 10, 1), Astegunak.ASTELEHENA, 2),
                tarteHutsa(LocalDate.of(2026, 10, 19))));

        FaltakBistaDTO dto = service.kalkulatuFaltenBista(koadernoa, 10, 2026);

        assertThat(dto.getEgunekoOrduak()).containsExactly(
                entry(LocalDate.of(2026, 10, 5), 2),
                entry(LocalDate.of(2026, 10, 12), 2));
        assertThat(dto.getProgramaOrduak()).isEqualTo(4);
    }

    @Test
    void egunBereziakEtaOrdezkatuaMainBezalaInterpretatzenDitu() {
        List<EgunBerezi> bereziak = List.of(
                egunBerezia(LocalDate.of(2026, 10, 5), EgunMota.JAIEGUNA, null),
                egunBerezia(LocalDate.of(2026, 10, 6), EgunMota.EZ_LEKTIBOA, null),
                egunBerezia(LocalDate.of(2026, 10, 8), EgunMota.ORDEZKATUA, Astegunak.ASTELEHENA),
                egunBerezia(LocalDate.of(2026, 10, 10), EgunMota.LEKTIBOA, null));
        Koadernoa koadernoa = koadernoa(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 11), bereziak);
        prestatu(koadernoa, List.of(
                blokea(LocalDate.of(2026, 10, 1), Astegunak.ASTELEHENA, 2),
                blokea(LocalDate.of(2026, 10, 1), Astegunak.LARUNBATA, 3)));

        FaltakBistaDTO dto = service.kalkulatuFaltenBista(koadernoa, 10, 2026);

        assertThat(dto.getEgunekoOrduak()).containsExactly(
                entry(LocalDate.of(2026, 10, 8), 2));
        assertThat(dto.getProgramaOrduak()).isEqualTo(2);
    }

    @Test
    void hutsegitePortzentajeakMainEkoProgramaOrduBerakErabiltzenDitu() {
        Koadernoa koadernoa = koadernoa(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), List.of());
        prestatu(koadernoa, List.of(
                blokea(LocalDate.of(2026, 9, 1), Astegunak.ASTELEHENA, 2),
                blokea(LocalDate.of(2026, 9, 1), Astegunak.LARUNBATA, 3),
                duala(LocalDate.of(2026, 9, 14))));
        Matrikula matrikula = new Matrikula();
        matrikula.setId(7L);
        matrikula.setKoadernoa(koadernoa);
        Saioa saioa = new Saioa();
        saioa.setKoadernoa(koadernoa);
        saioa.setData(LocalDate.of(2026, 9, 7));
        saioa.setIraupenaSlot(2);
        Asistentzia asistentzia = new Asistentzia();
        asistentzia.setSaioa(saioa);
        asistentzia.setMatrikula(matrikula);
        asistentzia.setEgoera(Asistentzia.AsistentziaEgoera.HUTS);
        when(saioaRepository.findByKoadernoaIdAndDataBetweenOrderByDataAscHasieraSlotAsc(
                org.mockito.ArgumentMatchers.eq(koadernoa.getId()),
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                org.mockito.ArgumentMatchers.any(LocalDate.class)))
                .thenReturn(List.of(saioa));
        when(asistentziaRepository.findBySaioaInAndMatrikulaIn(
                List.of(saioa), List.of(matrikula))).thenReturn(List.of(asistentzia));

        Map<Long, Double> emaitza = service.kalkulatuHutsegitePortzentaiak(List.of(matrikula));

        assertThat(emaitza).containsEntry(7L, 25.0);
    }

    private void prestatu(Koadernoa koadernoa, List<KoadernoOrdutegiBlokea> blokeak) {
        when(ordutegiRepository.findByKoadernoa_Id(koadernoa.getId())).thenReturn(blokeak);
        when(matrikulaRepository.findByKoadernoaIdAndEgoeraMatrikulatuta(koadernoa.getId()))
                .thenReturn(List.of());
    }

    private Koadernoa koadernoa(LocalDate hasiera, LocalDate bukaera, List<EgunBerezi> bereziak) {
        Egutegia egutegia = new Egutegia();
        egutegia.setHasieraData(hasiera);
        egutegia.setBukaeraData(bukaera);
        egutegia.setEgunBereziak(new ArrayList<>(bereziak));
        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setId(1L);
        koadernoa.setEgutegia(egutegia);
        return koadernoa;
    }

    private KoadernoOrdutegiBlokea blokea(LocalDate hasiera, Astegunak asteguna, int iraupena) {
        KoadernoOrdutegiBlokea blokea = new KoadernoOrdutegiBlokea();
        blokea.setHasieraData(hasiera);
        blokea.setAsteguna(asteguna);
        blokea.setIraupenaSlot(iraupena);
        return blokea;
    }

    private KoadernoOrdutegiBlokea duala(LocalDate hasiera) {
        KoadernoOrdutegiBlokea blokea = new KoadernoOrdutegiBlokea();
        blokea.setHasieraData(hasiera);
        blokea.setDualOrdutegia(true);
        return blokea;
    }

    private KoadernoOrdutegiBlokea tarteHutsa(LocalDate hasiera) {
        KoadernoOrdutegiBlokea blokea = new KoadernoOrdutegiBlokea();
        blokea.setHasieraData(hasiera);
        blokea.setTarteHutsa(true);
        return blokea;
    }

    private EgunBerezi egunBerezia(LocalDate data, EgunMota mota, Astegunak ordezkatua) {
        EgunBerezi eguna = new EgunBerezi();
        eguna.setData(data);
        eguna.setMota(mota);
        eguna.setOrdezkatua(ordezkatua);
        return eguna;
    }
}
