package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.mock.web.MockHttpServletRequest;

import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.jokabidea.repository.JokabideDesegokiaRepository;

class JokabideDesegokiakKudeatzaileControllerTest {

    private JokabideDesegokiaRepository repository;
    private IkasturteaRepository ikasturteaRepository;
    private JokabideDesegokiakKudeatzaileController controller;

    @BeforeEach
    void setUp() {
        repository = mock(JokabideDesegokiaRepository.class);
        ikasturteaRepository = mock(IkasturteaRepository.class);
        controller = new JokabideDesegokiakKudeatzaileController(
                repository, mock(IrakasleaService.class), ikasturteaRepository);
        when(repository.bilatuKudeatzailearentzat(
                nullable(LocalDate.class), nullable(LocalDate.class), nullable(String.class),
                nullable(Long.class), nullable(Long.class), nullable(Boolean.class))).thenReturn(List.of());
        when(repository.findDistinctModuloakOrderByIzena()).thenReturn(List.of());
        when(repository.findDistinctTaldeakOrderByIzena()).thenReturn(List.of());
    }

    @Test
    void hasieraDataHutsaBerariazBidaltzeanEzDuDefektuaAplikatzen() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("dataHasiera", "");
        Model model = new ExtendedModelMap();

        controller.index(null, null, "Braun Jon Pepito", null, null, null, request, model);

        verify(repository).bilatuKudeatzailearentzat(
                isNull(), isNull(), org.mockito.ArgumentMatchers.eq("Braun Jon Pepito"),
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

        controller.index(null, null, null, null, null, null, request, model);

        LocalDate defektuzkoa = LocalDate.of(2025, 9, 1);
        verify(repository).bilatuKudeatzailearentzat(
                org.mockito.ArgumentMatchers.eq(defektuzkoa), isNull(), isNull(), isNull(), isNull(), isNull());
        org.junit.jupiter.api.Assertions.assertEquals(defektuzkoa, model.getAttribute("dataHasiera"));
    }
}
