package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.koadernoa.app.objektuak.egutegia.repository.MailaRepository;
import com.koadernoa.app.objektuak.egutegia.service.EgutegiaService;
import com.koadernoa.app.objektuak.egutegia.service.IkasturteaService;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;

class EgutegiaControllerTest {

    @Test
    void ezabatuOndorenIkasturtearenFitxaraItzultzenDa() {
        EgutegiaService egutegiaService = mock(EgutegiaService.class);
        KoadernoaRepository koadernoaRepository = mock(KoadernoaRepository.class);
        EgutegiaController controller = new EgutegiaController(
                mock(IkasturteaService.class),
                egutegiaService,
                koadernoaRepository,
                mock(MailaRepository.class));
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);
        when(koadernoaRepository.existsByEgutegia_Id(8L)).thenReturn(false);

        String redirect = controller.ezabatuEgutegia(8L, 3L, redirectAttributes);

        verify(egutegiaService).ezabatu(8L);
        assertEquals("redirect:/kudeatzaile/ikasturteak/3", redirect);
    }
}
