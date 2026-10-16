package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioEgoera;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioMomentua;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioNota;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioMomentuaRepository;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioNotaRepository;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Ebaluaketa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Jarduera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.service.JardueraKalifikazioService;
import com.koadernoa.app.objektuak.koadernoak.service.JardueraNotaIraulketaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;

class JardueraNotaIraulketaServiceTest {

    private JardueraKalifikazioService kalifikazioService;
    private EbaluazioMomentuaRepository momentuaRepository;
    private EbaluazioNotaRepository notaRepository;
    private JardueraNotaIraulketaService service;

    @BeforeEach
    void setUp() {
        kalifikazioService = mock(JardueraKalifikazioService.class);
        momentuaRepository = mock(EbaluazioMomentuaRepository.class);
        notaRepository = mock(EbaluazioNotaRepository.class);
        service = new JardueraNotaIraulketaService(kalifikazioService, momentuaRepository, notaRepository);
    }

    @Test
    void pisudunJardueraGuztiekNotaDuteneanIrauliDaiteke() {
        when(kalifikazioService.prestatu(1L, 11L, null)).thenReturn(data(true));

        assertThat(service.irauliDaiteke(1L, 11L)).isTrue();
    }

    @Test
    void pisudunJarduerarenNotaFaltaBadaEzinDaIrauli() {
        when(kalifikazioService.prestatu(1L, 11L, null)).thenReturn(data(false));

        assertThat(service.irauliDaiteke(1L, 11L)).isFalse();
    }

    @Test
    void ikasleBatekNotaGuztiakBadituBotoiaAgertuEtaIkasleHoriBakarrikIraultzenDu() {
        Ebaluaketa ebaluaketa = new Ebaluaketa();
        ebaluaketa.setId(11L);
        ebaluaketa.setOrdena(1);
        Jarduera jarduera = new Jarduera();
        jarduera.setId(21L);
        jarduera.setPisua(new BigDecimal("100"));
        Matrikula osatua = new Matrikula();
        osatua.setId(31L);
        Matrikula osatuGabea = new Matrikula();
        osatuGabea.setId(32L);
        var data = new JardueraKalifikazioService.PantailaData(
                List.of(ebaluaketa), Map.of(11L, List.of(jarduera)), 11L,
                List.of(jarduera), List.of(osatua, osatuGabea),
                Map.of("31_21", new BigDecimal("7")),
                Map.of(31L, new BigDecimal("7"), 32L, BigDecimal.ZERO), null);
        when(kalifikazioService.prestatu(1L, 11L, null)).thenReturn(data);

        assertThat(service.irauliDaiteke(1L, 11L)).isTrue();

        Maila maila = new Maila();
        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setId(1L);
        Moduloa moduloa = new Moduloa();
        moduloa.setMaila(maila);
        koadernoa.setModuloa(moduloa);
        EbaluazioMomentua momentua = new EbaluazioMomentua();
        momentua.setAktibo(true);
        momentua.setOnartuNotaZenbakizkoa(true);
        when(momentuaRepository.findByMailaAndKodea(maila, "1_EBAL")).thenReturn(Optional.of(momentua));
        when(notaRepository.findByMatrikulaAndEbaluazioMomentua(osatua, momentua)).thenReturn(Optional.empty());

        assertThat(service.irauli(koadernoa, 11L)).isEqualTo(1);
        verify(notaRepository).findByMatrikulaAndEbaluazioMomentua(osatua, momentua);
        verify(notaRepository, org.mockito.Mockito.never())
                .findByMatrikulaAndEbaluazioMomentua(osatuGabea, momentua);
    }

    @Test
    void kalkulatutakoNotaDagokionMomenturaIraultzenDu() {
        var data = data(true);
        when(kalifikazioService.prestatu(1L, 11L, null)).thenReturn(data);

        Maila maila = new Maila();
        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setId(1L);
        Moduloa moduloa = new Moduloa();
        moduloa.setMaila(maila);
        koadernoa.setModuloa(moduloa);

        EbaluazioMomentua momentua = new EbaluazioMomentua();
        momentua.setKodea("1_EBAL");
        momentua.setAktibo(true);
        momentua.setOnartuNotaZenbakizkoa(true);
        momentua.setOnartuHamartarrak(true);
        when(momentuaRepository.findByMailaAndKodea(maila, "1_EBAL")).thenReturn(Optional.of(momentua));

        Matrikula matrikula = data.matrikulak().get(0);
        EbaluazioNota lehendik = new EbaluazioNota();
        lehendik.setEgoera(new EbaluazioEgoera());
        when(notaRepository.findByMatrikulaAndEbaluazioMomentua(matrikula, momentua))
                .thenReturn(Optional.of(lehendik));

        assertThat(service.irauli(koadernoa, 11L)).isEqualTo(1);

        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<List<EbaluazioNota>> captor = (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
        verify(notaRepository).saveAll(captor.capture());
        EbaluazioNota gordeta = captor.getValue().get(0);
        assertThat(gordeta.getNota()).isEqualTo(6.8);
        assertThat(gordeta.getEgoera()).isNull();
        assertThat(gordeta.getEbaluazioMomentua()).isSameAs(momentua);
    }

    @Test
    void hamartarrikGabekoMomentuanAdostutakoBiribiltzeaAplikatzenDu() {
        assertThat(iraulitakoNota("4.99")).isEqualTo(4.0);
        assertThat(iraulitakoNota("5.50")).isEqualTo(5.0);
        assertThat(iraulitakoNota("5.51")).isEqualTo(6.0);
        assertThat(iraulitakoNota("8.50")).isEqualTo(8.0);
        assertThat(iraulitakoNota("8.51")).isEqualTo(9.0);
    }

    private double iraulitakoNota(String guztira) {
        clearInvocations(kalifikazioService, momentuaRepository, notaRepository);
        var data = data(true, guztira);
        when(kalifikazioService.prestatu(1L, 11L, null)).thenReturn(data);

        Maila maila = new Maila();
        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setId(1L);
        Moduloa moduloa = new Moduloa();
        moduloa.setMaila(maila);
        koadernoa.setModuloa(moduloa);

        EbaluazioMomentua momentua = new EbaluazioMomentua();
        momentua.setAktibo(true);
        momentua.setOnartuNotaZenbakizkoa(true);
        momentua.setOnartuHamartarrak(false);
        when(momentuaRepository.findByMailaAndKodea(maila, "1_EBAL")).thenReturn(Optional.of(momentua));
        when(notaRepository.findByMatrikulaAndEbaluazioMomentua(data.matrikulak().get(0), momentua))
                .thenReturn(Optional.empty());

        service.irauli(koadernoa, 11L);
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<List<EbaluazioNota>> captor = (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
        verify(notaRepository).saveAll(captor.capture());
        return captor.getValue().get(0).getNota();
    }

    private JardueraKalifikazioService.PantailaData data(boolean notaOsatuta) {
        return data(notaOsatuta, "6.80");
    }

    private JardueraKalifikazioService.PantailaData data(boolean notaOsatuta, String guztira) {
        Ebaluaketa ebaluaketa = new Ebaluaketa();
        ebaluaketa.setId(11L);
        ebaluaketa.setOrdena(1);
        Jarduera jarduera = new Jarduera();
        jarduera.setId(21L);
        jarduera.setPisua(new BigDecimal("100"));
        Matrikula matrikula = new Matrikula();
        matrikula.setId(31L);
        Map<String, BigDecimal> notak = notaOsatuta
                ? Map.of("31_21", new BigDecimal("6.8")) : Map.of();
        return new JardueraKalifikazioService.PantailaData(
                List.of(ebaluaketa), Map.of(11L, List.of(jarduera)), 11L,
                List.of(jarduera), List.of(matrikula), notak,
                Map.of(31L, new BigDecimal(guztira)), null);
    }
}
