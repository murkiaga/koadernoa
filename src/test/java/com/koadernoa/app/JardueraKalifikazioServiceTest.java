package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Ebaluaketa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Jarduera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.JardueraKalifikazioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Programazioa;
import com.koadernoa.app.objektuak.koadernoak.repository.JardueraKalifikazioaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.JardueraRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.ProgramazioaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.JardueraKalifikazioService;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.entitateak.MatrikulaEgoera;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;

class JardueraKalifikazioServiceTest {
    private JardueraRepository jardueraRepository;
    private JardueraKalifikazioaRepository kalifikazioaRepository;
    private MatrikulaRepository matrikulaRepository;
    private ProgramazioaRepository programazioaRepository;
    private JardueraKalifikazioService service;

    @BeforeEach
    void setUp() {
        jardueraRepository = mock(JardueraRepository.class);
        kalifikazioaRepository = mock(JardueraKalifikazioaRepository.class);
        matrikulaRepository = mock(MatrikulaRepository.class);
        programazioaRepository = mock(ProgramazioaRepository.class);
        service = new JardueraKalifikazioService(jardueraRepository, kalifikazioaRepository,
                matrikulaRepository, programazioaRepository);
    }

    @Test
    void ebaluaketarenJarduerakTaldekatuEtaGuztiraPonderatzenDu() {
        Ebaluaketa lehenengoa = ebaluaketa(11L, 1, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 20));
        Ebaluaketa bigarrena = ebaluaketa(12L, 2, LocalDate.of(2027, 1, 7), LocalDate.of(2027, 3, 31));
        Programazioa programazioa = new Programazioa();
        programazioa.setEbaluaketak(List.of(bigarrena, lehenengoa));

        Jarduera azterketa = jarduera(21L, LocalDate.of(2026, 10, 10), "40");
        Jarduera lana = jarduera(22L, LocalDate.of(2026, 11, 10), "60");
        Matrikula matrikula = matrikula(31L);
        JardueraKalifikazioa azterketaNota = kalifikazioa(azterketa, matrikula, "8");
        JardueraKalifikazioa lanNota = kalifikazioa(lana, matrikula, "6");

        when(programazioaRepository.findByKoadernoaIdFetchEbaluaketak(1L)).thenReturn(Optional.of(programazioa));
        when(jardueraRepository.findByKoadernoaIdAndKalifikagarriaTrueOrderByDataAscIdAsc(1L))
                .thenReturn(List.of(azterketa, lana));
        when(matrikulaRepository.findByKoadernoaIdAndEgoeraFetchIkasleaOrderByIzena(
                1L, MatrikulaEgoera.MATRIKULATUA)).thenReturn(List.of(matrikula));
        when(kalifikazioaRepository.findByKoadernoId(1L)).thenReturn(List.of(azterketaNota, lanNota));

        var data = service.prestatu(1L, null, null);

        assertThat(data.ebaluaketak()).containsExactly(lehenengoa, bigarrena);
        assertThat(data.hautatutakoEbaluaketaId()).isEqualTo(11L);
        assertThat(data.hautatutakoJarduerak()).containsExactly(azterketa, lana);
        assertThat(data.guztirakoak().get(31L)).isEqualByComparingTo("6.80");
    }

    @Test
    void hamarBainoNotaHandiagoaEzDuOnartzen() {
        Ebaluaketa ebaluaketa = ebaluaketa(11L, 1, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 20));
        Programazioa programazioa = new Programazioa();
        programazioa.setEbaluaketak(List.of(ebaluaketa));
        Jarduera jarduera = jarduera(21L, LocalDate.of(2026, 10, 10), "100");
        Matrikula matrikula = matrikula(31L);
        when(programazioaRepository.findByKoadernoaIdFetchEbaluaketak(1L)).thenReturn(Optional.of(programazioa));
        when(jardueraRepository.findByKoadernoaIdAndKalifikagarriaTrueOrderByDataAscIdAsc(1L))
                .thenReturn(List.of(jarduera));
        when(matrikulaRepository.findByKoadernoaIdAndEgoeraFetchIkasleaOrderByIzena(
                1L, MatrikulaEgoera.MATRIKULATUA)).thenReturn(List.of(matrikula));
        when(kalifikazioaRepository.findByKoadernoId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.gorde(1L, 11L, Map.of(
                "pisua_21", new String[] {"100"},
                "nota_31_21", new String[] {"10.01"})))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 eta 10");
    }

    private Ebaluaketa ebaluaketa(Long id, int ordena, LocalDate hasiera, LocalDate bukaera) {
        Ebaluaketa e = new Ebaluaketa();
        e.setId(id);
        e.setOrdena(ordena);
        e.setHasieraData(hasiera);
        e.setBukaeraData(bukaera);
        return e;
    }

    private Jarduera jarduera(Long id, LocalDate data, String pisua) {
        Jarduera j = new Jarduera();
        j.setId(id);
        j.setData(data);
        j.setKalifikagarria(true);
        j.setPisua(new BigDecimal(pisua));
        return j;
    }

    private Matrikula matrikula(Long id) {
        Matrikula m = new Matrikula();
        m.setId(id);
        m.setIkaslea(new Ikaslea());
        return m;
    }

    private JardueraKalifikazioa kalifikazioa(Jarduera jarduera, Matrikula matrikula, String nota) {
        JardueraKalifikazioa k = new JardueraKalifikazioa();
        k.setJarduera(jarduera);
        k.setMatrikula(matrikula);
        k.setNota(new BigDecimal(nota));
        return k;
    }
}
