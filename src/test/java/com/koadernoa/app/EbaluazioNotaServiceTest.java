package com.koadernoa.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioMomentua;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioNota;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioNotaRepository;
import com.koadernoa.app.objektuak.ebaluazioa.service.EbaluazioNotaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;

class EbaluazioNotaServiceTest {

    @Test
    void notaHustenDeneanAurrekoNotaEzabatzenDu() {
        EbaluazioNotaRepository repo = org.mockito.Mockito.mock(EbaluazioNotaRepository.class);
        EbaluazioNotaService service = new EbaluazioNotaService(repo);
        Matrikula matrikula = matrikula(304L);
        EbaluazioMomentua momentua = momentua(1L, false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("nota_304_1", "");

        String error = service.gordeNotak(null, List.of(momentua), List.of(matrikula), request, false);

        assertNull(error);
        verify(repo).deleteByMatrikulaIdAndMomentuaId(304L, 1L);
        verify(repo, never()).save(any(EbaluazioNota.class));
    }

    @Test
    void hamartarrakDesaktibatutaDaudeneanNotaDezimalaBaztertzenDu() {
        EbaluazioNotaRepository repo = org.mockito.Mockito.mock(EbaluazioNotaRepository.class);
        EbaluazioNotaService service = new EbaluazioNotaService(repo);
        Matrikula matrikula = matrikula(304L);
        EbaluazioMomentua momentua = momentua(1L, false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("nota_304_1", "5,5");

        String error = service.gordeNotak(null, List.of(momentua), List.of(matrikula), request, false);

        assertTrue(error.contains("zenbaki osoa izan behar du"));
        verify(repo, never()).save(any(EbaluazioNota.class));
    }

    @Test
    void hamartarrakDesaktibatutaDaudeneanBostPuntuZeroBaztertzenDu() {
        EbaluazioNotaRepository repo = org.mockito.Mockito.mock(EbaluazioNotaRepository.class);
        EbaluazioNotaService service = new EbaluazioNotaService(repo);
        Matrikula matrikula = matrikula(304L);
        EbaluazioMomentua momentua = momentua(1L, false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("nota_304_1", "5.0");

        String error = service.gordeNotak(null, List.of(momentua), List.of(matrikula), request, false);

        assertTrue(error.contains("zenbaki osoa izan behar du"));
        verify(repo, never()).save(any(EbaluazioNota.class));
    }

    @Test
    void hamartarrakAktibatutaDaudeneanNotaDezimalaGordetzenDu() {
        EbaluazioNotaRepository repo = org.mockito.Mockito.mock(EbaluazioNotaRepository.class);
        EbaluazioNotaService service = new EbaluazioNotaService(repo);
        Matrikula matrikula = matrikula(304L);
        EbaluazioMomentua momentua = momentua(1L, true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("nota_304_1", "5,5");
        when(repo.findByMatrikulaAndEbaluazioMomentua(matrikula, momentua)).thenReturn(Optional.empty());

        String error = service.gordeNotak(null, List.of(momentua), List.of(matrikula), request, false);

        assertNull(error);
        verify(repo).save(any(EbaluazioNota.class));
    }

    @Test
    void notaBistaratzekoHamartarrakDesaktibatutaDaudeneanOsoaEmatenDu() {
        EbaluazioNota nota = new EbaluazioNota();
        nota.setNota(5.0);
        nota.setEbaluazioMomentua(momentua(1L, false));

        assertEquals("5", nota.getNotaBistaratzeko());
    }

    private Matrikula matrikula(Long id) {
        Matrikula matrikula = new Matrikula();
        matrikula.setId(id);
        return matrikula;
    }

    private EbaluazioMomentua momentua(Long id, boolean onartuHamartarrak) {
        EbaluazioMomentua momentua = new EbaluazioMomentua();
        momentua.setId(id);
        momentua.setIzena("1. ebaluazioa");
        momentua.setOnartuNotaZenbakizkoa(true);
        momentua.setOnartuHamartarrak(onartuHamartarrak);
        return momentua;
    }
}
