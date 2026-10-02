package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.koadernoa.app.objektuak.konfigurazioa.entitateak.AplikazioAukera;
import com.koadernoa.app.objektuak.konfigurazioa.repository.AplikazioAukeraRepository;
import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;

class AplikazioAukeraServiceTest {

    private final AplikazioAukeraRepository repository = mock(AplikazioAukeraRepository.class);
    private final AplikazioAukeraService service = new AplikazioAukeraService(repository);

    @Test
    void plangintzaKontrolAutomatikoaDefektuzDesaktibatutaDago() {
        when(repository.findById(AplikazioAukeraService.PLANGINTZA_KONTROL_AUTOMATIKOA))
                .thenReturn(Optional.empty());

        assertThat(service.isPlangintzaKontrolAutomatikoaAktibo()).isFalse();
    }

    @Test
    void plangintzaKontrolAutomatikoarenBalioaKeyValueGisaGordetzenDu() {
        when(repository.findById(AplikazioAukeraService.PLANGINTZA_KONTROL_AUTOMATIKOA))
                .thenReturn(Optional.empty());

        service.setPlangintzaKontrolAutomatikoa(true);

        ArgumentCaptor<AplikazioAukera> captor = ArgumentCaptor.forClass(AplikazioAukera.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getGiltza())
                .isEqualTo(AplikazioAukeraService.PLANGINTZA_KONTROL_AUTOMATIKOA);
        assertThat(captor.getValue().getBalioa()).isEqualTo("true");
    }
}
