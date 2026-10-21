package com.koadernoa.app.objektuak.zikloak.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;
import com.koadernoa.app.objektuak.zikloak.repository.ZikloaRepository;

class ZikloaServiceTest {

    private final ZikloaRepository repository = mock(ZikloaRepository.class);
    private final ZikloaService service = new ZikloaService(repository);

    @Test
    void ethaziPisuakGordetzenDituBaturaEhunDenean() {
        Zikloa zikloa = new Zikloa();
        when(repository.findById(4L)).thenReturn(Optional.of(zikloa));

        service.eguneratuEthaziPisuak(4L, new BigDecimal("65"), new BigDecimal("35"));

        assertThat(zikloa.getEthaziTeknikoenPisua()).isEqualByComparingTo("65.00");
        assertThat(zikloa.getEthaziZeharkakoenPisua()).isEqualByComparingTo("35.00");
        verify(repository).save(zikloa);
    }

    @Test
    void zeharkakoEbaluazioPisuakGordetzenDituBaturaEhunDenean() {
        Zikloa zikloa = new Zikloa();
        when(repository.findById(4L)).thenReturn(Optional.of(zikloa));

        service.eguneratuEthaziPisuak(4L, new BigDecimal("70"), new BigDecimal("30"),
            new BigDecimal("80"), new BigDecimal("15"), new BigDecimal("5"));

        assertThat(zikloa.getEthaziHeteroebaluazioPisua()).isEqualByComparingTo("80.00");
        assertThat(zikloa.getEthaziKoebaluazioPisua()).isEqualByComparingTo("15.00");
        assertThat(zikloa.getEthaziAutoebaluazioPisua()).isEqualByComparingTo("5.00");
    }

    @Test
    void ethaziPisuakEzDituGordetzenBaturaEhunEzDenean() {
        assertThatThrownBy(() -> service.eguneratuEthaziPisuak(
                4L, new BigDecimal("70"), new BigDecimal("20")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("%100");
    }

    @Test
    void ethaziPisuakZeroEtaEhunArteanEgonBeharDira() {
        assertThatThrownBy(() -> service.eguneratuEthaziPisuak(
                4L, new BigDecimal("101"), new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 eta 100 artean");
    }

    @Test
    void zikloarenOhikoEdizioakAurretikEzarritakoEthaziPisuakMantentzenDitu() {
        Zikloa aurrekoa = new Zikloa();
        aurrekoa.setEthaziTeknikoenPisua(new BigDecimal("60.00"));
        aurrekoa.setEthaziZeharkakoenPisua(new BigDecimal("40.00"));
        Zikloa editatua = new Zikloa();
        editatua.setId(4L);
        when(repository.findById(4L)).thenReturn(Optional.of(aurrekoa));

        service.save(editatua);

        assertThat(editatua.getEthaziTeknikoenPisua()).isEqualByComparingTo("60.00");
        assertThat(editatua.getEthaziZeharkakoenPisua()).isEqualByComparingTo("40.00");
        verify(repository).save(editatua);
    }
}
