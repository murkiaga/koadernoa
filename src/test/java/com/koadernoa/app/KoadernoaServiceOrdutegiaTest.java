package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.koadernoa.app.objektuak.ebaluazioa.repository.*;
import com.koadernoa.app.objektuak.egutegia.entitateak.*;
import com.koadernoa.app.objektuak.egutegia.repository.EgutegiaRepository;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.koadernoak.entitateak.*;
import com.koadernoa.app.objektuak.koadernoak.repository.*;
import com.koadernoa.app.objektuak.koadernoak.service.*;
import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;
import com.koadernoa.app.objektuak.modulua.repository.*;

class KoadernoaServiceOrdutegiaTest {
    private final KoadernoaRepository koadernoak = mock(KoadernoaRepository.class);
    private final SaioaRepository saioak = mock(SaioaRepository.class);
    private final AsistentziaRepository asistentziak = mock(AsistentziaRepository.class);
    private final KoadernoOrdutegiBlokeaRepository blokeak = mock(KoadernoOrdutegiBlokeaRepository.class);
    private final ProgramazioaService programazioa = mock(ProgramazioaService.class);
    private KoadernoaService service;
    private Koadernoa koadernoa;
    private KoadernoOrdutegiBlokea blokea;
    private final LocalDate hasiera = LocalDate.of(2026, 9, 1);

    @BeforeEach
    void prestatu() {
        service = new KoadernoaService(mock(ModuloaRepository.class), mock(EgutegiaRepository.class),
                mock(IrakasleaRepository.class), koadernoak, mock(JardueraRepository.class),
                mock(EbaluazioMomentuaRepository.class), mock(ProgramazioaRepository.class),
                mock(UnitateDidaktikoaRepository.class), mock(EstatistikaEbaluazioanRepository.class),
                saioak, mock(MatrikulaRepository.class), mock(NotaFitxategiaRepository.class),
                asistentziak, mock(EbaluazioNotaRepository.class), blokeak,
                mock(AplikazioAukeraService.class), mock(MintegiModuluBaimenaRepository.class), programazioa);
        Egutegia egutegia = new Egutegia();
        egutegia.setHasieraData(hasiera); egutegia.setBukaeraData(LocalDate.of(2027, 6, 30));
        koadernoa = new Koadernoa(); koadernoa.setId(341L); koadernoa.setEgutegia(egutegia);
        blokea = blokea(10L, 1, 1, hasiera);
        koadernoa.setOrdutegiak(new ArrayList<>(List.of(blokea)));
        when(koadernoak.findWithOrdutegiaById(341L)).thenReturn(Optional.of(koadernoa));
        when(asistentziak.findBySaioa_IdIn(any())).thenReturn(List.of());
    }

    @Test void saioaAsistentziarikGabeSlotKentzeaOnartzenDu() {
        Saioa s = saioa(1L, blokea, hasiera.plusDays(6), 1);
        when(saioak.findByKoadernoa_IdAndDataBetween(any(), any(), any())).thenReturn(List.of(s));
        service.setSlotSelected(341L, 1, 1, false, hasiera);
        verify(saioak).deleteAll(List.of(s));
    }

    @Test void etorriDuenSaioaEtaAsistentziaGarbitzenDitu() {
        Saioa s = saioa(1L, blokea, hasiera.plusDays(6), 1);
        Asistentzia a = asistentzia(s, Asistentzia.AsistentziaEgoera.ETORRI);
        when(saioak.findByKoadernoa_IdAndDataBetween(any(), any(), any())).thenReturn(List.of(s));
        when(asistentziak.findBySaioa_IdIn(any())).thenReturn(List.of(a));
        service.setSlotSelected(341L, 1, 1, false, hasiera);
        verify(asistentziak).deleteAll(List.of(a)); verify(saioak).deleteAll(List.of(s));
    }

    @Test void hutsDuenSaioakAldaketaBlokeatzenDu() { egiaztatuBlokeoa(Asistentzia.AsistentziaEgoera.HUTS); }
    @Test void justifikatuaDuenSaioakAldaketaBlokeatzenDu() { egiaztatuBlokeoa(Asistentzia.AsistentziaEgoera.JUSTIFIKATUA); }
    @Test void beranduDuenSaioakAldaketaBlokeatzenDu() { egiaztatuBlokeoa(Asistentzia.AsistentziaEgoera.BERANDU); }

    @Test void muturrekoSlotBatKentzeanBlokeaLaburtzenDu() {
        blokea.setIraupenaSlot(3);
        service.setSlotSelected(341L, 1, 1, false, hasiera);
        assertThat(blokea.getHasieraSlot()).isEqualTo(2); assertThat(blokea.getIraupenaSlot()).isEqualTo(2);
    }

    @Test void erdikoSlotBatKentzeanBlokeaZatitzenDu() {
        blokea.setIraupenaSlot(3);
        when(blokeak.save(any())).thenAnswer(i -> { KoadernoOrdutegiBlokea b=i.getArgument(0); b.setId(11L); return b; });
        service.setSlotSelected(341L, 1, 2, false, hasiera);
        assertThat(koadernoa.getOrdutegiak()).extracting(KoadernoOrdutegiBlokea::getHasieraSlot).containsExactly(1, 3);
    }

    @Test void slotBerriaGehitzeanBiBlokeBatzenEtaSaioakBerrizEsleitzenDitu() {
        KoadernoOrdutegiBlokea eskuina = blokea(11L, 3, 1, hasiera);
        koadernoa.getOrdutegiak().add(eskuina);
        Saioa s = saioa(2L, eskuina, hasiera.plusDays(6), 3);
        when(saioak.findByKoadernoa_IdAndDataBetween(any(), any(), any())).thenReturn(List.of(s));
        service.setSlotSelected(341L, 1, 2, true, hasiera);
        assertThat(koadernoa.getOrdutegiak()).containsExactly(blokea);
        assertThat(s.getIturburuBlokea()).isSameAs(blokea);
    }

    @Test void ordutegiHistorikoaEditatzeanBereIndarraldikoSaioakBakarrikKontsultatzenDitu() {
        LocalDate hurrengoa = LocalDate.of(2027, 1, 10);
        koadernoa.getOrdutegiak().add(blokea(12L, 2, 1, hurrengoa));
        service.setSlotSelected(341L, 1, 1, false, hasiera);
        verify(saioak).findByKoadernoa_IdAndDataBetween(341L, hasiera, hurrengoa.minusDays(1));
    }

    @Test void ordutegiOsoaSaioHutsekinEzabatzenDu() {
        Saioa s = saioa(1L, blokea, hasiera.plusDays(6), 1);
        when(saioak.findByKoadernoa_IdAndDataBetween(any(), any(), any())).thenReturn(List.of(s));
        assertThat(service.ezabatuOrdutegia(341L, hasiera)).isTrue();
        verify(saioak).deleteAll(List.of(s));
    }

    @Test void ordutegiOsoaBenetakoFaltarekinEzDuEzabatzen() {
        Saioa s = saioa(1L, blokea, hasiera.plusDays(6), 1);
        when(saioak.findByKoadernoa_IdAndDataBetween(any(), any(), any())).thenReturn(List.of(s));
        when(asistentziak.findBySaioa_IdIn(any())).thenReturn(List.of(asistentzia(s, Asistentzia.AsistentziaEgoera.HUTS)));
        assertThatThrownBy(() -> service.ezabatuOrdutegia(341L, hasiera))
                .hasMessage(KoadernoaService.ORDUTEGIA_ASISTENTZIA_MEZUA);
        assertThat(koadernoa.getOrdutegiak()).containsExactly(blokea);
    }

    private void egiaztatuBlokeoa(Asistentzia.AsistentziaEgoera egoera) {
        Saioa s = saioa(1L, blokea, hasiera.plusDays(6), 1);
        when(saioak.findByKoadernoa_IdAndDataBetween(any(), any(), any())).thenReturn(List.of(s));
        when(asistentziak.findBySaioa_IdIn(any())).thenReturn(List.of(asistentzia(s, egoera)));
        assertThatThrownBy(() -> service.setSlotSelected(341L, 1, 1, false, hasiera))
                .hasMessage(KoadernoaService.ORDUTEGIA_ASISTENTZIA_MEZUA);
        assertThat(koadernoa.getOrdutegiak()).containsExactly(blokea);
        verify(saioak, never()).deleteAll(any()); verify(asistentziak, never()).deleteAll(any());
    }

    private KoadernoOrdutegiBlokea blokea(Long id, int slot, int iraupena, LocalDate data) {
        KoadernoOrdutegiBlokea b = new KoadernoOrdutegiBlokea(); b.setId(id); b.setKoadernoa(koadernoa);
        b.setAsteguna(Astegunak.ASTELEHENA); b.setHasieraSlot(slot); b.setIraupenaSlot(iraupena); b.setHasieraData(data); return b;
    }
    private Saioa saioa(Long id, KoadernoOrdutegiBlokea b, LocalDate data, int slot) {
        Saioa s = new Saioa(); s.setId(id); s.setKoadernoa(koadernoa); s.setIturburuBlokea(b); s.setData(data); s.setHasieraSlot(slot); return s;
    }
    private Asistentzia asistentzia(Saioa s, Asistentzia.AsistentziaEgoera egoera) {
        Asistentzia a = new Asistentzia(); a.setId(20L); a.setSaioa(s); a.setEgoera(egoera); return a;
    }
}
