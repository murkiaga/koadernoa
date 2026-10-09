package com.koadernoa.app.objektuak.irakasleak.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Rola;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;

@ExtendWith(MockitoExtension.class)
class IrakasleaProvisioningServiceTest {

    @Mock IrakasleaRepository repository;
    private IrakasleaProvisioningService service;

    @BeforeEach
    void prestatu() {
        service = new IrakasleaProvisioningService(repository);
    }

    @Test
    void lehenLoginakAurretikEsleitutakoMintegiaMantentzenDu() {
        Familia mintegia = new Familia();
        Irakaslea aurretikSortua = new Irakaslea();
        aurretikSortua.setEmaila("irakaslea@eskola.eus");
        aurretikSortua.setMintegia(mintegia);
        aurretikSortua.setRola(Rola.IRAKASLEA);
        when(repository.findByEmailaIgnoreCase("irakaslea@eskola.eus")).thenReturn(Optional.of(aurretikSortua));

        Irakaslea emaitza = service.bilatuEdoSortu(
                "IRAKASLEA@ESKOLA.EUS", "Beste izen bat", "LDAP erabiltzailea");

        assertThat(emaitza).isSameAs(aurretikSortua);
        assertThat(emaitza.getMintegia()).isSameAs(mintegia);
        verify(repository, never()).save(any());
    }

    @Test
    void eskuzkoAltakIrakasleRolaEtaMintegiaEsleitzenDitu() {
        Familia mintegia = new Familia();
        when(repository.findByEmailaIgnoreCase("ane@eskola.eus")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Irakaslea emaitza = service.sortuEskuz(" Ane@Eskola.eus ", "Ane Agirre", mintegia);

        assertThat(emaitza.getEmaila()).isEqualTo("ane@eskola.eus");
        assertThat(emaitza.getIzena()).isEqualTo("Ane Agirre");
        assertThat(emaitza.getRola()).isEqualTo(Rola.IRAKASLEA);
        assertThat(emaitza.getMintegia()).isSameAs(mintegia);
        assertThat(emaitza.getKontu_mota()).isEqualTo("LDAP erabiltzailea");
    }

    @Test
    void eskuzkoAltakEzDuDagoenKontuaAldatzen() {
        when(repository.findByEmailaIgnoreCase("ane@eskola.eus")).thenReturn(Optional.of(new Irakaslea()));

        assertThatThrownBy(() -> service.sortuEskuz("ane@eskola.eus", "Ane", new Familia()))
                .isInstanceOf(IrakasleaProvisioningService.IrakasleaDagoenekoBadagoException.class);
        verify(repository, never()).save(any());
    }
}
