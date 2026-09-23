package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.mezuak.entitateak.Mezua;
import com.koadernoa.app.objektuak.mezuak.repository.MezuaRepository;
import com.koadernoa.app.objektuak.mezuak.service.MezuaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.repository.IkasleaRepository;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;
import com.koadernoa.app.objektuak.modulua.service.IkasleaService;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;

class IkasleaTaldeAldaketaMezuTest {
    private final KoadernoaRepository koadernoak = mock(KoadernoaRepository.class);
    private final IkasleaRepository ikasleak = mock(IkasleaRepository.class);
    private final MatrikulaRepository matrikulak = mock(MatrikulaRepository.class);
    private final TaldeaRepository taldeak = mock(TaldeaRepository.class);
    private final IkasturteaRepository ikasturteak = mock(IkasturteaRepository.class);
    private final MezuaRepository mezuak = mock(MezuaRepository.class);
    private final IrakasleaRepository irakasleak = mock(IrakasleaRepository.class);
    private final IkasleaService service = new IkasleaService(koadernoak, ikasleak, matrikulak,
            taldeak, ikasturteak, new MezuaService(mezuak, irakasleak));

    @Test
    void taldeBakoitzekoIrakasleakIdzBereiztenDituMatrikulaLehendikEgondaEre() {
        Ikaslea ikaslea = prestatu(true);
        Koadernoa zaharra = koadernoa(10L, 100L, 101L);
        Koadernoa zaharra2 = koadernoa(11L, 100L);
        Koadernoa berria = koadernoa(20L, 100L, 102L);
        when(koadernoak.findActiveYearKoadernoIdsByTaldea(1L)).thenReturn(List.of(10L, 11L));
        when(koadernoak.findAllById(List.of(10L, 11L))).thenReturn(List.of(zaharra, zaharra2));
        when(koadernoak.findActiveYearKoadernoIdsByTaldea(2L)).thenReturn(List.of(20L));
        when(koadernoak.findAllById(List.of(20L))).thenReturn(List.of(berria));
        when(matrikulak.existsByIkasleaIdAndKoadernoaId(7L, 20L)).thenReturn(true);
        Irakaslea sistema = new Irakaslea(); sistema.setId(500L);
        when(irakasleak.findByIzenaIgnoreCase("sistema")).thenReturn(Optional.of(sistema));

        service.aldatuIkaslearenTaldea(7L, 2L, "admin");

        ArgumentCaptor<Mezua> captor = ArgumentCaptor.forClass(Mezua.class);
        verify(mezuak, times(4)).save(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(m -> {
            assertThat(m.getBidaltzailea()).isSameAs(sistema);
            assertThat(m.getEdukia()).contains(ikaslea.getIzenOsoa());
            assertThat(m.getBidalketaData()).isNotNull();
        });
        assertThat(captor.getAllValues().subList(0, 2)).allSatisfy(m ->
                assertThat(m.getEdukia()).contains("jada ez da 1SMA taldekoa"));
        assertThat(captor.getAllValues().subList(2, 4)).allSatisfy(m ->
                assertThat(m.getEdukia()).contains("Ikasle berri bat dago 2SMA taldean"));
        assertThat(captor.getAllValues()).extracting(m -> m.getHartzailea().getId())
                .containsExactly(100L, 101L, 100L, 102L);
    }

    @Test
    void taldeaHutsikUzteanZaharrekoeiBakarrikJakinaraztenDie() {
        Ikaslea ikaslea = prestatu(true);
        when(koadernoak.findActiveYearKoadernoIdsByTaldea(1L)).thenReturn(List.of(10L));
        when(koadernoak.findAllById(List.of(10L))).thenReturn(List.of(koadernoa(10L, 100L)));
        Matrikula matrikula = new Matrikula();
        when(matrikulak.findActiveYearMatrikulatuakByIkasleaAndNotTaldea(7L, null)).thenReturn(List.of(matrikula));

        var emaitza = service.aldatuIkaslearenTaldea(7L, null, null);

        assertThat(ikaslea.getTaldea()).isNull();
        assertThat(emaitza.kendutakoMatrikulak()).isEqualTo(1);
        assertThat(emaitza.sortutakoMatrikulak()).isZero();
        verify(matrikulak).deleteAll(List.of(matrikula));
        verify(matrikulak, never()).save(any());
        ArgumentCaptor<Mezua> captor = ArgumentCaptor.forClass(Mezua.class);
        verify(mezuak).save(captor.capture());
        assertThat(captor.getValue().getEdukia()).contains("jada ez da 1SMA taldekoa");
    }

    @Test
    void talderikGabekoIkasleariTaldeaEmateanBerrikoeiJakinaraztenDie() {
        prestatu(false);
        when(koadernoak.findActiveYearKoadernoIdsByTaldea(2L)).thenReturn(List.of(20L));
        when(koadernoak.findAllById(List.of(20L))).thenReturn(List.of(koadernoa(20L, 102L)));
        var emaitza = service.aldatuIkaslearenTaldea(7L, 2L, null);
        assertThat(emaitza.sortutakoMatrikulak()).isEqualTo(1);
        verify(matrikulak).save(any());
        ArgumentCaptor<Mezua> captor = ArgumentCaptor.forClass(Mezua.class);
        verify(mezuak).save(captor.capture());
        assertThat(captor.getValue().getEdukia()).contains("Ikasle berri bat dago 2SMA taldean");
    }

    @Test
    void aldaketarikGabeEzDuMezurikBidaltzen() {
        prestatu(true);
        assertThat(service.aldatuIkaslearenTaldea(7L, 1L, null).aldaketaEginDa()).isFalse();
        prestatu(false);
        assertThat(service.aldatuIkaslearenTaldea(7L, null, null).aldaketaEginDa()).isFalse();
        verifyNoInteractions(mezuak, matrikulak, koadernoak);
    }

    private Ikaslea prestatu(boolean taldearekin) {
        Taldea zaharra = new Taldea(); zaharra.setId(1L); zaharra.setIzena("1SMA");
        Taldea berria = new Taldea(); berria.setId(2L); berria.setIzena("2SMA");
        Ikaslea ikaslea = new Ikaslea(); ikaslea.setId(7L); ikaslea.setIzena("Ane");
        ikaslea.setAbizena1("Etxeberria"); ikaslea.setTaldea(taldearekin ? zaharra : null);
        when(ikasleak.findById(7L)).thenReturn(Optional.of(ikaslea));
        when(taldeak.findById(1L)).thenReturn(Optional.of(zaharra));
        when(taldeak.findById(2L)).thenReturn(Optional.of(berria));
        when(ikasturteak.findFirstByAktiboaTrueOrderByIdDesc()).thenReturn(Optional.of(new Ikasturtea()));
        return ikaslea;
    }

    private Koadernoa koadernoa(Long id, Long... irakasleIds) {
        Koadernoa koadernoa = new Koadernoa(); koadernoa.setId(id);
        koadernoa.setIrakasleak(java.util.Arrays.stream(irakasleIds).map(irakasleId -> {
            Irakaslea irakaslea = new Irakaslea(); irakaslea.setId(irakasleId); return irakaslea;
        }).toList());
        return koadernoa;
    }
}
