package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoPlangintzaKontrolService;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoPlangintzaKontrola;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoPlangintzaKontrola.Egoera;
import com.koadernoa.app.objektuak.koadernoak.service.PlangintzaKontrolScheduler;
import com.koadernoa.app.objektuak.mezuak.service.MezuaService;

class PlangintzaKontrolSchedulerTest {

    private final AplikazioAukeraService aukeraService = mock(AplikazioAukeraService.class);
    private final KoadernoPlangintzaKontrolService kontrolService = mock(KoadernoPlangintzaKontrolService.class);
    private final MezuaService mezuaService = mock(MezuaService.class);
    private final PlangintzaKontrolScheduler scheduler = new PlangintzaKontrolScheduler(
            aukeraService, kontrolService, mezuaService);

    @Test
    void desaktibatutaDagoeneanEzDuKontrolikExekutatzen() {
        when(aukeraService.isPlangintzaKontrolAutomatikoaAktibo()).thenReturn(false);

        scheduler.egunerokoKontrola();

        verifyNoInteractions(kontrolService, mezuaService);
    }

    @Test
    void aktibatutaDagoeneanEskuzkoKontrolServiceBeraEtaMezuMetodoaErabiltzenDitu() {
        KoadernoPlangintzaKontrola osatuGabe = kontrola(1L, Egoera.OSATU_GABE);
        KoadernoPlangintzaKontrola ondo = kontrola(2L, Egoera.ONDO);
        KoadernoPlangintzaKontrola klaseEgunikEz = kontrola(3L, Egoera.KLASE_EGUNIK_EZ);
        when(aukeraService.isPlangintzaKontrolAutomatikoaAktibo()).thenReturn(true);
        when(kontrolService.lortuKontrola()).thenReturn(List.of(osatuGabe, ondo, klaseEgunikEz));
        when(mezuaService.bidaliPlangintzaAbisuak(List.of(osatuGabe))).thenReturn(1);

        scheduler.egunerokoKontrola();

        verify(kontrolService).lortuKontrola();
        verify(mezuaService).bidaliPlangintzaAbisuak(List.of(osatuGabe));
    }

    @Test
    void koadernoGuztiakOndoEdoKlaseEgunikGabeDaudeneanEzDuMezurikBidaltzen() {
        when(aukeraService.isPlangintzaKontrolAutomatikoaAktibo()).thenReturn(true);
        when(kontrolService.lortuKontrola()).thenReturn(List.of(
                kontrola(1L, Egoera.ONDO), kontrola(2L, Egoera.KLASE_EGUNIK_EZ)));

        scheduler.egunerokoKontrola();

        verify(mezuaService, never()).bidaliPlangintzaAbisuak(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void erroreaLogeatuEtaSchedulerretikEzDuKanporaBotatzen() {
        when(aukeraService.isPlangintzaKontrolAutomatikoaAktibo()).thenReturn(true);
        when(kontrolService.lortuKontrola()).thenThrow(new IllegalStateException("proba"));

        assertThatCode(scheduler::egunerokoKontrola).doesNotThrowAnyException();
    }

    @Test
    void astegunetanZortzietanMadrilgoOrduEremuanExekutatzenDa() throws NoSuchMethodException {
        Scheduled scheduled = PlangintzaKontrolScheduler.class
                .getMethod("egunerokoKontrola")
                .getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("0 0 7 * * MON-FRI");
        assertThat(scheduled.zone()).isEqualTo("Europe/Madrid");
    }

    private KoadernoPlangintzaKontrola kontrola(Long id, Egoera egoera) {
        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setId(id);
        return new KoadernoPlangintzaKontrola(koadernoa, List.of(), 0, 0, egoera);
    }
}
