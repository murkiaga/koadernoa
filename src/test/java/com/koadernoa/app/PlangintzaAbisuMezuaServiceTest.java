package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoPlangintzaKontrola;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoPlangintzaKontrola.Egoera;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoPlangintzaKontrola.PlangintzaEgunKontrola;
import com.koadernoa.app.objektuak.mezuak.entitateak.Mezua;
import com.koadernoa.app.objektuak.mezuak.repository.MezuaRepository;
import com.koadernoa.app.objektuak.mezuak.service.MezuaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

class PlangintzaAbisuMezuaServiceTest {

    private final MezuaRepository mezuaRepository = mock(MezuaRepository.class);
    private final IrakasleaRepository irakasleaRepository = mock(IrakasleaRepository.class);
    private final MezuaService service = new MezuaService(
            mezuaRepository, irakasleaRepository);

    @Test
    void mezuBakarreanKoadernoakSartzenDituDatakGehituGabeEtaKlaseEgunikEzBaztertzenDu() {
        Irakaslea bidaltzailea = irakaslea(99L, "Kudeatzailea");
        Irakaslea ane = irakaslea(1L, "Ane");
        Irakaslea mikel = irakaslea(2L, "Mikel");
        Koadernoa programazioa = koadernoa(10L, "Programazioa", "1AW3", ane, mikel);
        Koadernoa datuBaseak = koadernoa(11L, "Datu-baseak", "1AW3", ane);
        Koadernoa fct = koadernoa(12L, "FCT", "2SMA", mikel);
        List<KoadernoPlangintzaKontrola> kontrolak = List.of(
                osatuGabe(programazioa, LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 19)),
                osatuGabe(datuBaseak, LocalDate.of(2026, 10, 16)),
                new KoadernoPlangintzaKontrola(fct, List.of(), 0, 0, Egoera.KLASE_EGUNIK_EZ));

        int bidalitakoak = service.bidaliPlangintzaAbisuak(bidaltzailea, kontrolak);

        assertThat(bidalitakoak).isEqualTo(2);
        ArgumentCaptor<Mezua> captor = ArgumentCaptor.forClass(Mezua.class);
        verify(mezuaRepository, times(2)).save(captor.capture());
        Mezua anerenMezua = mezua(captor, ane);
        Mezua mikelenMezua = mezua(captor, mikel);
        assertThat(anerenMezua.getEdukia())
                .contains("-Programazioa – 1AW3", "-Datu-baseak – 1AW3")
                .doesNotContain("Falta diren egunak", "2026-10-15", "2026-10-19", "2026-10-16", "FCT");
        assertThat(mikelenMezua.getEdukia())
                .contains("-Programazioa – 1AW3")
                .doesNotContain("Falta diren egunak", "2026-10-15", "Datu-baseak", "FCT");
    }

    @Test
    void bidalketaAutomatikoakSistemaBidaltzaileaEtaMultzokatzeBeraErabiltzenDitu() {
        Irakaslea sistema = irakaslea(99L, "sistema");
        Irakaslea ane = irakaslea(1L, "Ane");
        List<KoadernoPlangintzaKontrola> kontrolak = List.of(
                osatuGabe(koadernoa(10L, "Programazioa", "1AW3", ane), LocalDate.of(2026, 10, 15)),
                osatuGabe(koadernoa(11L, "Datu-baseak", "1AW3", ane), LocalDate.of(2026, 10, 16)));
        when(irakasleaRepository.findByIzenaIgnoreCase("sistema")).thenReturn(Optional.of(sistema));

        int bidalitakoak = service.bidaliPlangintzaAbisuak(kontrolak);

        assertThat(bidalitakoak).isEqualTo(1);
        ArgumentCaptor<Mezua> captor = ArgumentCaptor.forClass(Mezua.class);
        verify(mezuaRepository).save(captor.capture());
        assertThat(captor.getValue().getBidaltzailea()).isSameAs(sistema);
        assertThat(captor.getValue().getEdukia())
                .contains("-Programazioa – 1AW3", "-Datu-baseak – 1AW3");
    }

    private KoadernoPlangintzaKontrola osatuGabe(Koadernoa koadernoa, LocalDate... faltaDirenak) {
        List<PlangintzaEgunKontrola> egunak = java.util.Arrays.stream(faltaDirenak)
                .map(data -> new PlangintzaEgunKontrola(data, false)).toList();
        return new KoadernoPlangintzaKontrola(koadernoa, egunak, 0, egunak.size(), Egoera.OSATU_GABE);
    }

    private Mezua mezua(ArgumentCaptor<Mezua> captor, Irakaslea hartzailea) {
        return captor.getAllValues().stream()
                .filter(m -> m.getHartzailea().getId().equals(hartzailea.getId()))
                .findFirst().orElseThrow();
    }

    private Irakaslea irakaslea(Long id, String izena) {
        Irakaslea irakaslea = new Irakaslea();
        irakaslea.setId(id);
        irakaslea.setIzena(izena);
        return irakaslea;
    }

    private Koadernoa koadernoa(Long id, String moduluIzena, String taldeIzena, Irakaslea... irakasleak) {
        Taldea taldea = new Taldea();
        taldea.setIzena(taldeIzena);
        Moduloa moduloa = new Moduloa();
        moduloa.setIzena(moduluIzena);
        moduloa.setTaldea(taldea);
        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setId(id);
        koadernoa.setModuloa(moduloa);
        koadernoa.setIrakasleak(List.of(irakasleak));
        return koadernoa;
    }
}
