package com.koadernoa.app.ethazi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.koadernoa.app.ethazi.entitateak.EthaziKoadernoa;
import com.koadernoa.app.ethazi.service.EthaziDenboralizazioaService;
import com.koadernoa.app.ethazi.service.EthaziKoadernoaService;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.service.EgutegiaService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoOrdutegiBlokeaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.SaioaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.AsistentziaService;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

@ExtendWith(MockitoExtension.class)
class EthaziDenboralizazioaServiceTest {
    @Mock EthaziKoadernoaService koadernoaService;
    @Mock ModuloaRepository moduluak;
    @Mock KoadernoaRepository koadernoak;
    @Mock KoadernoOrdutegiBlokeaRepository blokeak;
    @Mock SaioaRepository saioak;
    @Mock AsistentziaService asistentziaService;
    @Mock EgutegiaService egutegiaService;

    private EthaziDenboralizazioaService service;
    private EthaziKoadernoa koadernoa;
    private Egutegia egutegia;
    private Irakaslea irakaslea;

    @BeforeEach
    void prestatu() {
        service = new EthaziDenboralizazioaService(koadernoaService, moduluak, koadernoak,
                blokeak, saioak, asistentziaService, egutegiaService);
        Taldea taldea = new Taldea(); taldea.setId(4L);
        koadernoa = new EthaziKoadernoa(); koadernoa.setId(6L); koadernoa.setTaldea(taldea);
        egutegia = new Egutegia();
        irakaslea = new Irakaslea(); irakaslea.setId(8L);
        when(egutegiaService.kalkulatuKlaseak(egutegia)).thenReturn(Map.of());
    }

    @Test
    void nireModuluetanNorbereJarduerakEtaOrokorrakErakustenDitu() {
        when(koadernoaService.programazioaIkuspegia(koadernoa, egutegia, irakaslea))
                .thenReturn(programazioa());
        when(koadernoak.findByModuloaIdInAktiboIkasturtea(10L)).thenReturn(List.of());
        when(koadernoak.findByModuloaIdInAktiboIkasturtea(20L)).thenReturn(List.of());

        var ikuspegia = service.prestatu(koadernoa, egutegia, irakaslea, null, 2026, 9);
        var irailarenHamabosta = ikuspegia.asteak().stream().flatMap(List::stream)
                .filter(e -> e.data().equals(LocalDate.of(2026, 9, 15))).findFirst().orElseThrow();

        assertThat(irailarenHamabosta.jarduerak())
                .extracting(EthaziDenboralizazioaService.EgunekoJarduera::id)
                .containsExactlyInAnyOrder(101L, 102L, 103L);
    }

    @Test
    void moduluZehatzeanModuluHorretakoJarduerakEtaOrokorrakBakarrikErakustenDitu() {
        when(koadernoaService.programazioaIkuspegia(koadernoa, egutegia, irakaslea))
                .thenReturn(programazioa());
        when(koadernoak.findByModuloaIdInAktiboIkasturtea(10L)).thenReturn(List.of());

        var ikuspegia = service.prestatu(koadernoa, egutegia, irakaslea, 10L, 2026, 9);
        var irailarenHamabosta = ikuspegia.asteak().stream().flatMap(List::stream)
                .filter(e -> e.data().equals(LocalDate.of(2026, 9, 15))).findFirst().orElseThrow();

        assertThat(ikuspegia.aukeratutakoModuloaId()).isEqualTo(10L);
        assertThat(irailarenHamabosta.jarduerak())
                .extracting(EthaziDenboralizazioaService.EgunekoJarduera::id)
                .containsExactlyInAnyOrder(101L, 103L);
    }

    private EthaziKoadernoaService.ProgramazioaIkuspegia programazioa() {
        var a = new EthaziKoadernoaService.ModuluAukera(10L, "PROG");
        var b = new EthaziKoadernoaService.ModuluAukera(20L, "SARE");
        LocalDate data = LocalDate.of(2026, 9, 15);
        var jardueraA = jarduera(101L, "A", false, 10L, "PROG", data);
        var jardueraB = jarduera(102L, "B", false, 20L, "SARE", data);
        var orokorra = jarduera(103L, "Komuna", true, null, "Orokorra", data);
        var erronka = new EthaziKoadernoaService.ErronkaTxartela(30L, 1, "Erronka 1", "",
                data, data.plusDays(10), List.of("PROG", "SARE"),
                List.of(jardueraA, jardueraB, orokorra), List.of(a, b), List.of(),
                List.of(jardueraA, jardueraB, orokorra), List.of());
        return new EthaziKoadernoaService.ProgramazioaIkuspegia(
                "2026-2027", List.of(erronka), List.of(), List.of(a, b));
    }

    private EthaziKoadernoaService.KronogramaJardueraTxartela jarduera(Long id, String izena,
            boolean orokorra, Long moduloaId, String badgea, LocalDate data) {
        return new EthaziKoadernoaService.KronogramaJardueraTxartela(id, izena, "", data, data,
                BigDecimal.ONE, orokorra, moduloaId, badgea, null, null, 1, false, null);
    }
}
