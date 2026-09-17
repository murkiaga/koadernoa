package com.koadernoa.app.objektuak.modulua.repository;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.entitateak.MatrikulaEgoera;

class MatrikulaRepositoryTest {

    @Test
    void matrikulatuakAbizenenAraberaOrdenatutakoKontsultarekinBilatzenDitu() {
        MatrikulaRepository repository = mock(MatrikulaRepository.class, CALLS_REAL_METHODS);
        List<Matrikula> ordenatutakoMatrikulak = List.of(new Matrikula());
        when(repository.findByKoadernoaIdAndEgoeraFetchIkasleaOrderByIzena(
                7L, MatrikulaEgoera.MATRIKULATUA)).thenReturn(ordenatutakoMatrikulak);

        List<Matrikula> emaitza = repository.findByKoadernoaIdAndEgoeraMatrikulatuta(7L);

        assertSame(ordenatutakoMatrikulak, emaitza);
        verify(repository).findByKoadernoaIdAndEgoeraFetchIkasleaOrderByIzena(
                7L, MatrikulaEgoera.MATRIKULATUA);
    }
}
