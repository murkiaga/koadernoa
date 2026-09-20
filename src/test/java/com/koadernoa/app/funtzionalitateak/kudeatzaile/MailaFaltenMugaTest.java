package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.egutegia.repository.MailaRepository;
import com.koadernoa.app.objektuak.ebaluazioa.repository.*;
import com.koadernoa.app.objektuak.zikloak.repository.FamiliaRepository;
import com.koadernoa.app.objektuak.modulua.repository.MintegiModuluBaimenaRepository;
import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;
import com.koadernoa.app.objektuak.jokabidea.repository.*;

class MailaFaltenMugaTest {
    private final MailaRepository mailak = mock(MailaRepository.class);
    private final KonfigurazioaController controller = new KonfigurazioaController(
            mailak, mock(FamiliaRepository.class), mock(EbaluazioMomentuaRepository.class),
            mock(EbaluazioEgoeraRepository.class), mock(EbaluazioNotaRepository.class),
            mock(EzadostasunKonfigRepository.class), mock(PendienteEbaluazioMomentuKonfigRepository.class),
            mock(AplikazioAukeraService.class), mock(MintegiModuluBaimenaRepository.class),
            mock(PortaeraArrazoiaRepository.class), mock(NeurriZuzentzaileaRepository.class));

    @Test
    void mailaBerriakHogeiLehenetsitaDu() throws Exception {
        assertThat(new Maila().getFaltenMugaPortzentaia()).isEqualTo(20);
        MockMvcBuilders.standaloneSetup(controller).build().perform(post("/kudeatzaile/konfigurazioa/mailak/sortu")
                .param("kodea", "BERRIA").param("ordena", "3"))
                .andExpect(status().is3xxRedirection());
        ArgumentCaptor<Maila> captor = ArgumentCaptor.forClass(Maila.class);
        verify(mailak).save(captor.capture());
        assertThat(captor.getValue().getFaltenMugaPortzentaia()).isEqualTo(20);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 20, 25, 100})
    void sortzeanMugaGordetzenDu(int muga) throws Exception {
        MockMvcBuilders.standaloneSetup(controller).build().perform(post("/kudeatzaile/konfigurazioa/mailak/sortu")
                .param("kodea", "BERRIA").param("faltenMugaPortzentaia", String.valueOf(muga)))
                .andExpect(status().is3xxRedirection());
        verify(mailak).save(argThat(m -> m.getFaltenMugaPortzentaia() == muga));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-1", "101", "25.5", "abc"})
    void sortzeanMugaBaliogabeaBaztertzenDu(String muga) throws Exception {
        MockMvcBuilders.standaloneSetup(controller).build().perform(post("/kudeatzaile/konfigurazioa/mailak/sortu")
                .param("kodea", "BERRIA").param("faltenMugaPortzentaia", muga))
                .andExpect(model().attributeHasFieldErrors("sortuForm", "faltenMugaPortzentaia"));
        verify(mailak, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"-1", "101", "25.5", "abc"})
    void editatzeanMugaBaliogabeakEzDuMailaAldatzen(String muga) {
        Maila maila = new Maila();
        maila.setKodea("LEHENENGOA");
        when(mailak.findById(1L)).thenReturn(Optional.of(maila));
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (muga != null) request.setParameter("faltenMugaPortzentaia", muga);
        request.setParameter("mailaKodea", "ALDATUA");
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        controller.gordeEbaluazioMomentuak(1L, request, flash);
        assertThat(flash.getFlashAttributes()).containsKey("error");
        assertThat(maila.getKodea()).isEqualTo("LEHENENGOA");
        assertThat(maila.getFaltenMugaPortzentaia()).isEqualTo(20);
        verify(mailak, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 20, 25, 100})
    void editatzeanMugaGordetzenDu(int muga) {
        Maila maila = new Maila();
        when(mailak.findById(1L)).thenReturn(Optional.of(maila));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("faltenMugaPortzentaia", String.valueOf(muga));
        controller.gordeEbaluazioMomentuak(1L, request, new RedirectAttributesModelMap());
        verify(mailak).save(maila);
        assertThat(maila.getFaltenMugaPortzentaia()).isEqualTo(muga);
    }
}
