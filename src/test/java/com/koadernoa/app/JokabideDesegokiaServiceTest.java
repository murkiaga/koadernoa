package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Rola;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.jokabidea.entitateak.JokabideDesegokia;
import com.koadernoa.app.objektuak.jokabidea.repository.JokabideDesegokiaRepository;
import com.koadernoa.app.objektuak.jokabidea.service.JokabideDesegokiaService;
import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;
import com.koadernoa.app.objektuak.mezuak.entitateak.Mezua;
import com.koadernoa.app.objektuak.mezuak.repository.MezuaRepository;
import com.koadernoa.app.objektuak.mezuak.service.MezuaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

class JokabideDesegokiaServiceTest {
    private final JokabideDesegokiaRepository jokabideRepository = mock(JokabideDesegokiaRepository.class);
    private final AplikazioAukeraService aukerak = mock(AplikazioAukeraService.class);
    private final IrakasleaRepository irakasleaRepository = mock(IrakasleaRepository.class);
    private final MezuaRepository mezuaRepository = mock(MezuaRepository.class);
    private final MezuaService mezuaService = new MezuaService(mezuaRepository, irakasleaRepository);
    private final JokabideDesegokiaService service = new JokabideDesegokiaService(
            jokabideRepository, aukerak, irakasleaRepository, mezuaService);

    @BeforeEach
    void prestatuGordetzea() {
        when(jokabideRepository.saveAndFlush(any(JokabideDesegokia.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void sortzeanMezuBakarraBidaltzenDioKonfiguratutakoArduradunariDatuEgokiekin() {
        Irakaslea sortzailea = irakaslea(10L, "Mikel", Rola.IRAKASLEA);
        Irakaslea arduraduna = irakaslea(20L, "Leire", Rola.KUDEATZAILEA);
        Irakaslea besteKudeatzailea = irakaslea(21L, "Jon", Rola.KUDEATZAILEA);
        JokabideDesegokia jokabidea = jokabidea(sortzailea);
        when(aukerak.getJokabideDesegokienArduradunaId()).thenReturn(arduraduna.getId());
        when(irakasleaRepository.findById(arduraduna.getId())).thenReturn(Optional.of(arduraduna));

        JokabideDesegokia gordeta = service.sortu(jokabidea);

        assertThat(gordeta).isSameAs(jokabidea);
        verify(jokabideRepository).saveAndFlush(jokabidea);
        ArgumentCaptor<Mezua> captor = ArgumentCaptor.forClass(Mezua.class);
        verify(mezuaRepository, times(1)).save(captor.capture());
        Mezua mezua = captor.getValue();
        assertThat(mezua.getBidaltzailea()).isSameAs(sortzailea);
        assertThat(mezua.getHartzailea()).isSameAs(arduraduna).isNotSameAs(besteKudeatzailea);
        assertThat(mezua.getEdukia()).contains(
                "Jokabide desegoki berria",
                "Ikaslea: Ane Agirre Etxebarria",
                "Taldea: 1SMA",
                "Modulua: Sare lokalak",
                "Data: 2026-10-08",
                "Sortzailea: Mikel");
    }

    @Test
    void arduradunikGabeJokabideaGordetzenDuEtaEzDuMezurikSortzen() {
        JokabideDesegokia jokabidea = jokabidea(irakaslea(10L, "Mikel", Rola.IRAKASLEA));
        when(aukerak.getJokabideDesegokienArduradunaId()).thenReturn(null);

        JokabideDesegokia gordeta = service.sortu(jokabidea);

        assertThat(gordeta).isSameAs(jokabidea);
        verify(jokabideRepository).saveAndFlush(jokabidea);
        verify(mezuaRepository, never()).save(any());
    }

    private JokabideDesegokia jokabidea(Irakaslea sortzailea) {
        Ikaslea ikaslea = new Ikaslea();
        ikaslea.setIzena("Ane");
        ikaslea.setAbizena1("Agirre");
        ikaslea.setAbizena2("Etxebarria");
        Taldea taldea = new Taldea();
        taldea.setIzena("1SMA");
        Moduloa moduloa = new Moduloa();
        moduloa.setIzena("Sare lokalak");
        moduloa.setTaldea(taldea);
        JokabideDesegokia jokabidea = new JokabideDesegokia();
        jokabidea.setId(30L);
        jokabidea.setIkaslea(ikaslea);
        jokabidea.setModuloa(moduloa);
        jokabidea.setIrakaslea(sortzailea);
        jokabidea.setData(LocalDate.of(2026, 10, 8));
        return jokabidea;
    }

    private Irakaslea irakaslea(Long id, String izena, Rola rola) {
        Irakaslea irakaslea = new Irakaslea();
        irakaslea.setId(id);
        irakaslea.setIzena(izena);
        irakaslea.setRola(rola);
        return irakaslea;
    }
}
