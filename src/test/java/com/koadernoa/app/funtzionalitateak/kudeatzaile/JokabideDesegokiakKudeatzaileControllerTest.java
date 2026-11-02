package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;

import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.jokabidea.repository.JokabideDesegokiaRepository;
import com.koadernoa.app.objektuak.jokabidea.entitateak.JokabideDesegokia;
import com.koadernoa.app.objektuak.jokabidea.entitateak.NeurriZuzentzailea;
import com.koadernoa.app.objektuak.jokabidea.entitateak.PortaeraArrazoia;
import com.koadernoa.app.objektuak.jokabidea.repository.NeurriZuzentzaileaRepository;
import com.koadernoa.app.objektuak.jokabidea.repository.PortaeraArrazoiaRepository;
import com.koadernoa.app.objektuak.jokabidea.service.JokabideDesegokiaPdfService;
import com.koadernoa.app.objektuak.jokabidea.service.JokabideDesegokiaService;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.mezuak.service.MezuaService;
import org.mockito.ArgumentCaptor;

class JokabideDesegokiakKudeatzaileControllerTest {

    private JokabideDesegokiaRepository repository;
    private IkasturteaRepository ikasturteaRepository;
    private MatrikulaRepository matrikulaRepository;
    private IrakasleaRepository irakasleaRepository;
    private PortaeraArrazoiaRepository portaeraArrazoiaRepository;
    private NeurriZuzentzaileaRepository neurriZuzentzaileaRepository;
    private JokabideDesegokiaPdfService pdfService;
    private JokabideDesegokiaService jokabideDesegokiaService;
    private JokabideDesegokiakKudeatzaileController controller;

    @BeforeEach
    void setUp() {
        repository = mock(JokabideDesegokiaRepository.class);
        ikasturteaRepository = mock(IkasturteaRepository.class);
        matrikulaRepository = mock(MatrikulaRepository.class);
        irakasleaRepository = mock(IrakasleaRepository.class);
        portaeraArrazoiaRepository = mock(PortaeraArrazoiaRepository.class);
        neurriZuzentzaileaRepository = mock(NeurriZuzentzaileaRepository.class);
        pdfService = mock(JokabideDesegokiaPdfService.class);
        jokabideDesegokiaService = mock(JokabideDesegokiaService.class);
        controller = new JokabideDesegokiakKudeatzaileController(
                repository, mock(IrakasleaService.class), ikasturteaRepository,
                matrikulaRepository, irakasleaRepository,
                portaeraArrazoiaRepository, neurriZuzentzaileaRepository,
                pdfService, jokabideDesegokiaService, mock(MezuaService.class));
        when(repository.bilatuKudeatzailearentzat(
                nullable(LocalDate.class), nullable(LocalDate.class), nullable(Long.class), nullable(String.class),
                nullable(Long.class), nullable(Long.class), nullable(Boolean.class))).thenReturn(List.of());
        when(repository.findDistinctModuloakOrderByIzena()).thenReturn(List.of());
        when(repository.findDistinctTaldeakOrderByIzena()).thenReturn(List.of());
    }

    @Test
    void hasieraDataHutsaBerariazBidaltzeanEzDuDefektuaAplikatzen() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("dataHasiera", "");
        Model model = new ExtendedModelMap();

        controller.index(null, null, null, "Braun Jon Pepito", null, null, null, request, model);

        verify(repository).bilatuKudeatzailearentzat(
                isNull(), isNull(), isNull(), org.mockito.ArgumentMatchers.eq("Braun Jon Pepito"),
                isNull(), isNull(), isNull());
        org.junit.jupiter.api.Assertions.assertNull(model.getAttribute("dataHasiera"));
    }

    @Test
    void hasieraDataParametrorikEzDagoeneanIkasturteHasieraAplikatzenDu() {
        Ikasturtea ikasturtea = new Ikasturtea();
        ikasturtea.setIzena("2025-2026");
        when(ikasturteaRepository.findFirstByAktiboaTrueOrderByIdDesc()).thenReturn(Optional.of(ikasturtea));
        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ExtendedModelMap();

        controller.index(null, null, null, null, null, null, null, request, model);

        LocalDate defektuzkoa = LocalDate.of(2025, 9, 1);
        verify(repository).bilatuKudeatzailearentzat(
                org.mockito.ArgumentMatchers.eq(defektuzkoa), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
        org.junit.jupiter.api.Assertions.assertEquals(defektuzkoa, model.getAttribute("dataHasiera"));
    }

    @Test
    void kudeatzaileakAukeratutakoIrakaslearenIzeneanSortzenDu() throws Exception {
        Ikasturtea ikasturtea = new Ikasturtea();
        ikasturtea.setAktiboa(true);
        Egutegia egutegia = new Egutegia();
        egutegia.setIkasturtea(ikasturtea);
        Moduloa moduloa = new Moduloa();
        moduloa.setId(8L);
        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setId(4L);
        koadernoa.setEgutegia(egutegia);
        koadernoa.setModuloa(moduloa);
        Ikaslea ikaslea = new Ikaslea();
        ikaslea.setId(9L);
        Matrikula matrikula = new Matrikula();
        matrikula.setId(3L);
        matrikula.setIkaslea(ikaslea);
        matrikula.setKoadernoa(koadernoa);
        Irakaslea irakaslea = new Irakaslea();
        irakaslea.setId(12L);
        PortaeraArrazoia arrazoia = new PortaeraArrazoia();
        arrazoia.setId(15L);
        arrazoia.setAktibo(true);
        NeurriZuzentzailea neurria = new NeurriZuzentzailea();
        neurria.setId(16L);
        neurria.setAktibo(true);
        when(matrikulaRepository.findMatrikulatuakAktiboIkasturteanByIkaslea(9L)).thenReturn(List.of(matrikula));
        when(irakasleaRepository.findById(12L)).thenReturn(Optional.of(irakaslea));
        when(portaeraArrazoiaRepository.findById(15L)).thenReturn(Optional.of(arrazoia));
        when(neurriZuzentzaileaRepository.findById(16L)).thenReturn(Optional.of(neurria));
        when(repository.findFirstByIkasleaIdAndKoadernoaIdAndDataOrderByCreatedAtDesc(9L, 4L, LocalDate.of(2026, 11, 2)))
                .thenReturn(Optional.empty());
        when(pdfService.sortu(any(JokabideDesegokia.class)))
                .thenReturn(new JokabideDesegokiaPdfService.SortutakoPdfa("/tmp/proba.pdf", "proba.pdf"));

        String view = controller.sortu(9L, 12L, "Ikaslea", "Irakaslea",
                LocalDate.of(2026, 11, 2), 15L, 16L,
                "  Pasiloan gertatutakoa  ", "  Hausnarketa idatzia  ",
                mock(Authentication.class), new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/kudeatzaile/jokabide-desegokiak?sortuta=true");
        ArgumentCaptor<JokabideDesegokia> captor = ArgumentCaptor.forClass(JokabideDesegokia.class);
        verify(jokabideDesegokiaService).sortu(captor.capture());
        JokabideDesegokia sortutakoa = captor.getValue();
        assertThat(sortutakoa.getIrakaslea()).isSameAs(irakaslea);
        assertThat(sortutakoa.getIkaslea()).isSameAs(ikaslea);
        assertThat(sortutakoa.getKoadernoa()).isSameAs(koadernoa);
        assertThat(sortutakoa.getModuloa()).isNull();
        assertThat(sortutakoa.getDeskribapenZehatza()).isEqualTo("Pasiloan gertatutakoa");
        assertThat(sortutakoa.getAplikatutakoNeurria()).isEqualTo("Hausnarketa idatzia");
        assertThat(sortutakoa.getPdfPath()).isEqualTo("/tmp/proba.pdf");
    }
}
