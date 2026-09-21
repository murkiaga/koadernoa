package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.service.IkasturteaService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaEzabatzeEgiaztapenService;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaEzabatzeService;
import com.koadernoa.app.objektuak.jokabidea.repository.IkasleEgunOharraRepository;
import com.koadernoa.app.objektuak.jokabidea.repository.JokabideDesegokiaRepository;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.ProgramazioTxantiloiRepository;
import com.koadernoa.app.objektuak.ordutegiak.repository.IrakasleOrdutegiLerroaRepository;
import com.koadernoa.app.objektuak.ordutegiak.repository.IrakasleOrdutegiaRepository;
import com.koadernoa.app.objektuak.ordutegiak.service.OrdezkoOrdutegiService;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.repository.FamiliaRepository;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;

@ExtendWith(MockitoExtension.class)
class IrakasleKudeatzaileControllerTest {
    @Mock private IrakasleaRepository irakasleaRepository;
    @Mock private KoadernoaRepository koadernoaRepository;
    @Mock private TaldeaRepository taldeaRepository;
    @Mock private ProgramazioTxantiloiRepository txantiloiRepository;
    @Mock private JokabideDesegokiaRepository jokabideRepository;
    @Mock private IkasleEgunOharraRepository oharraRepository;
    @Mock private IrakasleaEzabatzeService ezabatzeService;

    private IrakasleKudeatzaileController controller;
    private Irakaslea irakaslea;
    private RedirectAttributesModelMap ra;

    @BeforeEach
    void prestatu() {
        var egiaztapenService = new IrakasleaEzabatzeEgiaztapenService(
                koadernoaRepository, taldeaRepository, irakasleaRepository,
                txantiloiRepository, jokabideRepository, oharraRepository);
        controller = new IrakasleKudeatzaileController(irakasleaRepository,
                mock(FamiliaRepository.class), mock(IkasturteaService.class),
                mock(IrakasleOrdutegiaRepository.class), mock(IrakasleOrdutegiLerroaRepository.class),
                koadernoaRepository, ezabatzeService, egiaztapenService, mock(OrdezkoOrdutegiService.class));
        irakaslea = new Irakaslea();
        irakaslea.setId(42L);
        ra = new RedirectAttributesModelMap();
        when(irakasleaRepository.findById(42L)).thenReturn(Optional.of(irakaslea));
    }

    @Test
    void koadernoakEzabatzeaEragoztenDuEtaIkasturteaErakustenDu() {
        Ikasturtea ikasturtea = new Ikasturtea();
        ikasturtea.setIzena("2025-2026");
        Egutegia egutegia = new Egutegia();
        egutegia.setIkasturtea(ikasturtea);
        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setEgutegia(egutegia);
        when(koadernoaRepository.findIrakaslearenKoadernoak(42L, irakaslea)).thenReturn(List.of(koadernoa));

        egiaztatuBlokeoa("Ezin izan da irakaslea ezabatu, koadernoren batekin lotuta dagoelako. Ikasturtea: 2025-2026.");
        verifyNoInteractions(taldeaRepository, txantiloiRepository, jokabideRepository, oharraRepository);
    }

    @Test
    void ikasturterikGabekoKoadernoakEreEzabatzeaEragoztenDu() {
        when(koadernoaRepository.findIrakaslearenKoadernoak(42L, irakaslea)).thenReturn(List.of(new Koadernoa()));
        egiaztatuBlokeoa("Ezin izan da irakaslea ezabatu, koadernoren batekin lotuta dagoelako.");
    }

    @Test
    void tutoretzakEzabatzeaEragoztenDuEtaTaldeaErakustenDu() {
        Taldea taldea = new Taldea();
        taldea.setIzena("1SMA");
        when(taldeaRepository.findByTutorea_Id(42L)).thenReturn(Optional.of(taldea));
        egiaztatuBlokeoa("Ezin izan da irakaslea ezabatu, 1SMA taldeko tutorea delako.");
    }

    @Test
    void ordezkoEsleipenakEzabatzeaEragoztenDu() {
        when(irakasleaRepository.existsByOrdezkoa_Id(42L)).thenReturn(true);
        egiaztatuBlokeoa("Ezin izan da irakaslea ezabatu, beste irakasle baten ordezko gisa esleituta dagoelako.");
    }

    @Test
    void programazioTxantiloiakEzabatzeaEragoztenDu() {
        when(txantiloiRepository.existsByIrakaslea_Id(42L)).thenReturn(true);
        egiaztatuBlokeoa("Ezin izan da irakaslea ezabatu, programazio-txantiloiren batekin lotuta dagoelako.");
    }

    @Test
    void jokabideErregistroakEzabatzeaEragoztenDu() {
        when(jokabideRepository.existsByIrakaslea_IdOrJasotaNork_Id(42L, 42L)).thenReturn(true);
        egiaztatuBlokeoa("Ezin izan da irakaslea ezabatu, jokabide desegokien erregistroren batekin lotuta dagoelako.");
    }

    @Test
    void egunekoOharrakEzabatzeaEragoztenDu() {
        when(oharraRepository.existsByIrakaslea_Id(42L)).thenReturn(true);
        egiaztatuBlokeoa("Ezin izan da irakaslea ezabatu, ikasleren baten eguneko ohar batekin lotuta dagoelako.");
    }

    @Test
    void loturarikGabeOhikoEzabatzeaEgitenDu() {
        // Norberak ordezkoa izateak ez du sarrerako FK loturarik sortzen.
        irakaslea.setOrdezkoa(new Irakaslea());
        assertEquals("redirect:/kudeatzaile/irakasleak", controller.ezabatuIrakaslea(42L, ra));
        verify(ezabatzeService).ezabatu(irakaslea);
        assertEquals("Irakaslea ondo ezabatu da.", ra.getFlashAttributes().get("success"));
        assertFalse(ra.getFlashAttributes().containsKey("error"));
    }

    @Test
    void aurreikusiGabekoFkAkMezuGenerikoaMantentzenDu() {
        doThrow(new DataIntegrityViolationException("FK")).when(ezabatzeService).ezabatu(irakaslea);
        assertEquals("redirect:/kudeatzaile/irakasleak", controller.ezabatuIrakaslea(42L, ra));
        assertEquals("Ezin izan da irakaslea ezabatu, beste datu batzuekin lotuta dagoelako.",
                ra.getFlashAttributes().get("error"));
        assertFalse(ra.getFlashAttributes().containsKey("success"));
    }

    @Test
    void aurkituGabekoIrakaslearenMezuaMantentzenDu() {
        when(irakasleaRepository.findById(42L)).thenReturn(Optional.empty());
        egiaztatuBlokeoa("Irakaslea ez da aurkitu.");
        verifyNoInteractions(koadernoaRepository, taldeaRepository);
    }

    private void egiaztatuBlokeoa(String mezua) {
        assertEquals("redirect:/kudeatzaile/irakasleak", controller.ezabatuIrakaslea(42L, ra));
        assertEquals(mezua, ra.getFlashAttributes().get("error"));
        assertFalse(ra.getFlashAttributes().containsKey("success"));
        verifyNoInteractions(ezabatzeService);
    }
}
