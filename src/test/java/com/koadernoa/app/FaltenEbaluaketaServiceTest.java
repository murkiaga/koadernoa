package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoOrdutegiBlokea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoOrdutegiBlokeaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.FaltenEbaluaketaService;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoKlaseEgunService;

class FaltenEbaluaketaServiceTest {

    @Test
    void egutegikoHiruEbaluazioakEtaOrdutegikoOrduakKalkulatzenDitu() {
        KoadernoOrdutegiBlokeaRepository repository = mock(KoadernoOrdutegiBlokeaRepository.class);
        FaltenEbaluaketaService service = new FaltenEbaluaketaService(
                repository, new KoadernoKlaseEgunService());

        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setId(7L);
        Egutegia egutegia = new Egutegia();
        egutegia.setHasieraData(LocalDate.of(2026, 9, 1));
        egutegia.setLehenEbalBukaera(LocalDate.of(2026, 12, 18));
        egutegia.setBigarrenEbalBukaera(LocalDate.of(2027, 3, 19));
        egutegia.setBukaeraData(LocalDate.of(2027, 6, 18));
        egutegia.setEgunBereziak(new ArrayList<>());
        koadernoa.setEgutegia(egutegia);

        KoadernoOrdutegiBlokea blokea = new KoadernoOrdutegiBlokea();
        blokea.setKoadernoa(koadernoa);
        blokea.setHasieraData(egutegia.getHasieraData());
        blokea.setAsteguna(Astegunak.ASTELEHENA);
        blokea.setHasieraSlot(1);
        blokea.setIraupenaSlot(2);
        when(repository.findByKoadernoa_Id(7L)).thenReturn(List.of(blokea));

        var aukerak = service.lortuAukerak(koadernoa);

        assertThat(aukerak).hasSize(3);
        assertThat(aukerak.get(0).hasieraData()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(aukerak.get(0).bukaeraData()).isEqualTo(LocalDate.of(2026, 12, 18));
        assertThat(aukerak.get(1).hasieraData()).isEqualTo(LocalDate.of(2026, 12, 19));
        assertThat(aukerak.get(2).bukaeraData()).isEqualTo(LocalDate.of(2027, 6, 18));
        assertThat(aukerak).allMatch(aukera -> aukera.klaseOrduak() > 0);
    }

    @Test
    void koadernoariEzDagokionEbaluazioaBaztertzenDu() {
        KoadernoOrdutegiBlokeaRepository repository = mock(KoadernoOrdutegiBlokeaRepository.class);
        FaltenEbaluaketaService service = new FaltenEbaluaketaService(
                repository, new KoadernoKlaseEgunService());
        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setId(9L);
        Egutegia egutegia = new Egutegia();
        egutegia.setHasieraData(LocalDate.of(2026, 9, 1));
        egutegia.setBukaeraData(LocalDate.of(2027, 6, 18));
        egutegia.setEgunBereziak(List.of());
        koadernoa.setEgutegia(egutegia);
        when(repository.findByKoadernoa_Id(9L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.lortuAukera(koadernoa, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ez da koaderno honetakoa");
    }
}
