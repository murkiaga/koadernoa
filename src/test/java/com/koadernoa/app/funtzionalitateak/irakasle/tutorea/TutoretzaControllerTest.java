package com.koadernoa.app.funtzionalitateak.irakasle.tutorea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.koadernoa.app.funtzionalitateak.irakasle.baimenak.ikasleakkontsultatu.IkasleakKontsultatuController;
import com.koadernoa.app.funtzionalitateak.irakasle.baimenak.ikasleakkontsultatu.IkasleakKontsultatuService;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;
import com.koadernoa.app.objektuak.zikloak.repository.ZikloaRepository;

class TutoretzaControllerTest {

    @Test
    void asistentziakJatorrizkoKalkuluZerbitzuaBerrerabiltzenDu() {
        TutoretzaService tutoretzaService = mock(TutoretzaService.class);
        IkasleakKontsultatuService kontsultaService = mock(IkasleakKontsultatuService.class);
        TutoretzaController controller = new TutoretzaController(tutoretzaService, kontsultaService);
        Authentication auth = mock(Authentication.class);
        Taldea taldea = new Taldea(); taldea.setId(11L);
        Ikaslea ikaslea = new Ikaslea(); ikaslea.setId(30L);
        Ikasturtea ikasturtea = new Ikasturtea(); ikasturtea.setId(25L);
        LocalDate gaur = LocalDate.now();
        var datuak = new IkasleakKontsultatuService.IkaslearenAsistentziaDatuak(
                ikaslea, ikasturtea, gaur.getYear(), gaur.getMonthValue(), "urria 2026", List.of(), List.of());
        when(tutoretzaService.baimenduTaldea(11L, auth)).thenReturn(taldea);
        when(tutoretzaService.baimenduIkaslea(11L, 30L)).thenReturn(ikaslea);
        when(kontsultaService.getAsistentziaDatuak(30L, gaur.getYear(), gaur.getMonthValue()))
                .thenReturn(datuak);
        Model model = new ConcurrentModel();

        assertEquals("irakasleak/tutorea/asistentzia-kontrola",
                controller.asistentziaKontrola(11L, 30L, null, null, auth, model));
        assertEquals(datuak.lerroak(), model.getAttribute("lerroak"));
        verify(kontsultaService).getAsistentziaDatuak(30L, gaur.getYear(), gaur.getMonthValue());
    }

    @Test
    void jatorrizkoTaldeArgazkienUrlaBerdinDabil() throws Exception {
        IkasleakKontsultatuService service = mock(IkasleakKontsultatuService.class);
        Taldea taldea = new Taldea(); taldea.setId(11L);
        when(service.getTaldea(11L)).thenReturn(taldea);
        when(service.getTaldekoIkasleak(11L)).thenReturn(List.of());
        when(service.getIkasleakDituztenTaldeak()).thenReturn(List.of(taldea));
        var controller = new IkasleakKontsultatuController(
                service, mock(ZikloaRepository.class), mock(TaldeaRepository.class));

        MockMvcBuilders.standaloneSetup(controller).build()
                .perform(get("/irakasle/ikasleak-kontsultatu/taldeak/11/argazkiak"))
                .andExpect(status().isOk())
                .andExpect(view().name("irakasleak/baimenak/ikasleakKontsultatu/talde-argazkiak"));
    }
}
