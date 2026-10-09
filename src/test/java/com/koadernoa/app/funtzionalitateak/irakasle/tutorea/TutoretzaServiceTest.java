package com.koadernoa.app.funtzionalitateak.irakasle.tutorea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.koadernoa.app.funtzionalitateak.irakasle.baimenak.ikasleakkontsultatu.IkasleakKontsultatuService;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

class TutoretzaServiceTest {

    private IkasleakKontsultatuService kontsultaService;
    private IrakasleaService irakasleaService;
    private IkasturteaRepository ikasturteaRepository;
    private MatrikulaRepository matrikulaRepository;
    private TutoretzaService service;
    private Taldea taldea;
    private Irakaslea tutorea;
    private Ikasturtea ikasturtea;

    @BeforeEach
    void setUp() {
        kontsultaService = mock(IkasleakKontsultatuService.class);
        irakasleaService = mock(IrakasleaService.class);
        ikasturteaRepository = mock(IkasturteaRepository.class);
        matrikulaRepository = mock(MatrikulaRepository.class);
        service = new TutoretzaService(
                kontsultaService, irakasleaService, ikasturteaRepository, matrikulaRepository);
        tutorea = new Irakaslea();
        tutorea.setId(7L);
        taldea = new Taldea();
        taldea.setId(11L);
        taldea.setTutorea(tutorea);
        ikasturtea = new Ikasturtea();
        ikasturtea.setId(25L);
        ikasturtea.setAktiboa(true);
        when(kontsultaService.getTaldea(11L)).thenReturn(taldea);
    }

    @Test
    void taldekoTutoreakBereTaldeanSarDaiteke() {
        Authentication auth = auth("ROLE_IRAKASLEA");
        when(irakasleaService.getLogeatutaDagoenIrakaslea(auth)).thenReturn(tutorea);
        assertEquals(taldea, service.baimenduTaldea(11L, auth));
    }

    @Test
    void besteIrakasleakForbiddenJasotzenDu() {
        Authentication auth = auth("ROLE_IRAKASLEA");
        Irakaslea bestea = new Irakaslea();
        bestea.setId(8L);
        when(irakasleaService.getLogeatutaDagoenIrakaslea(auth)).thenReturn(bestea);
        assertThrows(AccessDeniedException.class, () -> service.baimenduTaldea(11L, auth));
    }

    @Test
    void kudeatzaileaEdozeinTaldetanSarDaiteke() {
        Authentication auth = auth("ROLE_KUDEATZAILEA");
        assertEquals(taldea, service.baimenduTaldea(11L, auth));
        verify(irakasleaService, never()).getLogeatutaDagoenIrakaslea(auth);
    }

    @Test
    void adminaEdozeinTaldetanSarDaiteke() {
        Authentication auth = auth("ROLE_ADMIN");
        assertEquals(taldea, service.baimenduTaldea(11L, auth));
        verify(irakasleaService, never()).getLogeatutaDagoenIrakaslea(auth);
    }

    @Test
    void taldekoaEzDenIkasleaEzinDaIkusi() {
        when(ikasturteaRepository.findFirstByAktiboaTrueOrderByIdDesc())
                .thenReturn(Optional.of(ikasturtea));
        when(matrikulaRepository.existsTaldekoMatrikulaByIkasturtea(11L, 30L, 25L))
                .thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> service.baimenduIkaslea(11L, 30L));
    }

    @Test
    void balidazioakIkasturteAktiboaBakarrikErabiltzenDu() {
        Ikaslea ikaslea = new Ikaslea();
        ikaslea.setId(30L);
        when(ikasturteaRepository.findFirstByAktiboaTrueOrderByIdDesc())
                .thenReturn(Optional.of(ikasturtea));
        when(matrikulaRepository.existsTaldekoMatrikulaByIkasturtea(11L, 30L, 25L))
                .thenReturn(true);
        when(kontsultaService.getIkaslea(30L)).thenReturn(ikaslea);
        assertEquals(ikaslea, service.baimenduIkaslea(11L, 30L));
        verify(matrikulaRepository).existsTaldekoMatrikulaByIkasturtea(11L, 30L, 25L);
    }

    private Authentication auth(String authority) {
        return new UsernamePasswordAuthenticationToken(
                "irakaslea@example.eus", "-", List.of(new SimpleGrantedAuthority(authority)));
    }
}
