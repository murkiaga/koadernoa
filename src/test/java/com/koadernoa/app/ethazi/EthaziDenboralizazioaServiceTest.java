package com.koadernoa.app.ethazi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;

import com.koadernoa.app.ethazi.entitateak.EthaziKoadernoa;
import com.koadernoa.app.ethazi.entitateak.EthaziDenboralizazioJarduera;
import com.koadernoa.app.ethazi.entitateak.Erronka;
import com.koadernoa.app.ethazi.entitateak.kronograma.KronogramaJardueraMota;
import com.koadernoa.app.ethazi.entitateak.kronograma.ErronkaKronogramaJarduera;
import com.koadernoa.app.ethazi.repository.EthaziDenboralizazioJardueraRepository;
import com.koadernoa.app.ethazi.service.EthaziDenboralizazioaService;
import com.koadernoa.app.ethazi.service.EthaziKoadernoaService;
import com.koadernoa.app.ethazi.service.KronogramaService;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.service.EgutegiaService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.SaioaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.AsistentziaService;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.TaldeOrdutegia;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.TaldeOrdutegiLerroa;
import com.koadernoa.app.objektuak.ordutegiak.repository.TaldeOrdutegiaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

@ExtendWith(MockitoExtension.class)
class EthaziDenboralizazioaServiceTest {
    @Mock EthaziKoadernoaService koadernoaService;
    @Mock ModuloaRepository moduluak;
    @Mock KoadernoaRepository koadernoak;
    @Mock TaldeOrdutegiaRepository taldeOrdutegiak;
    @Mock SaioaRepository saioak;
    @Mock AsistentziaService asistentziaService;
    @Mock EgutegiaService egutegiaService;
    @Mock EthaziDenboralizazioJardueraRepository denboralizazioJarduerak;
    @Mock KronogramaService kronogramaService;

    private EthaziDenboralizazioaService service;
    private EthaziKoadernoa koadernoa;
    private Egutegia egutegia;
    private Irakaslea irakaslea;

    @BeforeEach
    void prestatu() {
        service = new EthaziDenboralizazioaService(koadernoaService, moduluak, koadernoak,
                taldeOrdutegiak, saioak, asistentziaService, egutegiaService,
                denboralizazioJarduerak, kronogramaService);
        Taldea taldea = new Taldea(); taldea.setId(4L);
        koadernoa = new EthaziKoadernoa(); koadernoa.setId(6L); koadernoa.setTaldea(taldea);
        Ikasturtea ikasturtea = new Ikasturtea(); ikasturtea.setId(3L);
        egutegia = new Egutegia(); egutegia.setIkasturtea(ikasturtea);
        irakaslea = new Irakaslea(); irakaslea.setId(8L);
        lenient().when(egutegiaService.kalkulatuKlaseak(egutegia)).thenReturn(Map.of());
        lenient().when(asistentziaService.isEgunLektiboa(any(), any())).thenReturn(true);
        lenient().when(asistentziaService.effectiveAsteguna(any(), any())).thenAnswer(inv -> {
            LocalDate data = inv.getArgument(1);
            return switch (data.getDayOfWeek()) {
                case MONDAY -> Astegunak.ASTELEHENA;
                case TUESDAY -> Astegunak.ASTEARTEA;
                case WEDNESDAY -> Astegunak.ASTEAZKENA;
                case THURSDAY -> Astegunak.OSTEGUNA;
                case FRIDAY -> Astegunak.OSTIRALA;
                case SATURDAY -> Astegunak.LARUNBATA;
                case SUNDAY -> Astegunak.IGANDEA;
            };
        });
        Moduloa a = new Moduloa(); a.setId(10L); a.setKodea("PROG");
        Moduloa b = new Moduloa(); b.setId(20L); b.setKodea("SARE");
        lenient().when(moduluak.findAktiboakByTaldeaIdAndIrakasleaId(4L, 8L))
                .thenReturn(List.of(a, b));
    }

    @Test
    void nireModuluetanNorbereJarduerakEtaOrokorrakErakustenDitu() {
        when(koadernoak.findByModuloaIdInAktiboIkasturtea(10L)).thenReturn(List.of());
        when(koadernoak.findByModuloaIdInAktiboIkasturtea(20L)).thenReturn(List.of());
        when(denboralizazioJarduerak.findByEthaziKoadernoaIdAndDataBetweenOrderByDataAscIdAsc(
                6L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(gordetakoJarduerak());

        var ikuspegia = service.prestatu(koadernoa, egutegia, irakaslea, null, 2026, 9);
        var irailarenHamabosta = ikuspegia.asteak().stream().flatMap(List::stream)
                .filter(e -> e.data().equals(LocalDate.of(2026, 9, 15))).findFirst().orElseThrow();

        assertThat(irailarenHamabosta.jarduerak())
                .extracting(EthaziDenboralizazioaService.EgunekoJarduera::id)
                .containsExactlyInAnyOrder(101L, 102L, 103L);
    }

    @Test
    void moduluZehatzeanModuluHorretakoJarduerakEtaOrokorrakBakarrikErakustenDitu() {
        when(koadernoak.findByModuloaIdInAktiboIkasturtea(10L)).thenReturn(List.of());
        when(denboralizazioJarduerak.findByEthaziKoadernoaIdAndDataBetweenOrderByDataAscIdAsc(
                6L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(gordetakoJarduerak());

        var ikuspegia = service.prestatu(koadernoa, egutegia, irakaslea, 10L, 2026, 9);
        var irailarenHamabosta = ikuspegia.asteak().stream().flatMap(List::stream)
                .filter(e -> e.data().equals(LocalDate.of(2026, 9, 15))).findFirst().orElseThrow();

        assertThat(ikuspegia.aukeratutakoModuloaId()).isEqualTo(10L);
        assertThat(irailarenHamabosta.jarduerak())
                .extracting(EthaziDenboralizazioaService.EgunekoJarduera::id)
                .containsExactlyInAnyOrder(101L, 103L);
    }

    @Test
    void asistentziaBotoiaEthaziOrdutegikoEgunetanBakarrikDago() {
        Koadernoa asistentziaKoadernoa = new Koadernoa(); asistentziaKoadernoa.setId(70L);
        Moduloa prog = new Moduloa(); prog.setId(10L);
        TaldeOrdutegia lehenBertsioa = ordutegia(LocalDate.of(2026, 9, 1),
                lerroa(Astegunak.ASTELEHENA, 1, prog));
        TaldeOrdutegia bigarrenBertsioa = ordutegia(LocalDate.of(2026, 9, 14),
                lerroa(Astegunak.ASTELEHENA, 1, prog),
                lerroa(Astegunak.ASTEARTEA, 2, prog));
        when(taldeOrdutegiak.findByTaldea_IdAndIkasturtea_IdOrderByHasieraDataAsc(4L, 3L))
                .thenReturn(List.of(lehenBertsioa, bigarrenBertsioa));
        when(koadernoak.findByModuloaIdInAktiboIkasturtea(10L))
                .thenReturn(List.of(asistentziaKoadernoa));
        when(saioak.findByKoadernoaIdAndDataBetweenOrderByDataAscHasieraSlotAsc(
                70L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of());
        when(denboralizazioJarduerak.findByEthaziKoadernoaIdAndDataBetweenOrderByDataAscIdAsc(
                6L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of());

        var ikuspegia = service.prestatu(koadernoa, egutegia, irakaslea, 10L, 2026, 9);
        var asteartea = ikuspegia.asteak().stream().flatMap(List::stream)
                .filter(e -> e.data().equals(LocalDate.of(2026, 9, 15))).findFirst().orElseThrow();
        var asteazkena = ikuspegia.asteak().stream().flatMap(List::stream)
                .filter(e -> e.data().equals(LocalDate.of(2026, 9, 16))).findFirst().orElseThrow();

        assertThat(asteartea.asistentziak()).hasSize(1);
        assertThat(asteartea.asistentziak().get(0).moduloaId()).isEqualTo(10L);
        assertThat(asteartea.asistentziak().get(0).irekita()).isFalse();
        assertThat(asteazkena.asistentziak()).isEmpty();
    }

    @Test
    void ethaziOrdutegikoSlotetarakoSaioakSortzenDitu() {
        Koadernoa asistentziaKoadernoa = new Koadernoa(); asistentziaKoadernoa.setId(70L);
        Moduloa prog = new Moduloa(); prog.setId(10L);
        when(taldeOrdutegiak.findByTaldea_IdAndIkasturtea_IdOrderByHasieraDataAsc(4L, 3L))
                .thenReturn(List.of(ordutegia(LocalDate.of(2026, 9, 1),
                        lerroa(Astegunak.ASTEARTEA, 2, prog),
                        lerroa(Astegunak.ASTEARTEA, 4, prog))));
        when(saioak.findByKoadernoaIdAndData(70L, LocalDate.of(2026, 9, 15)))
                .thenReturn(List.of());

        service.sortuEthaziAsistentziaSaioak(koadernoa, egutegia,
                asistentziaKoadernoa, 10L, LocalDate.of(2026, 9, 15));

        verify(saioak).save(argThat(s -> s.getHasieraSlot() == 2 && s.getIraupenaSlot() == 1));
        verify(saioak).save(argThat(s -> s.getHasieraSlot() == 4 && s.getIraupenaSlot() == 1));
    }

    @Test
    void bolkatzeakAurrekoaOrdezkatuEtaEgunekoOrduakSnapshotGisaGordetzenDitu() {
        when(koadernoaService.programazioaIkuspegia(koadernoa, egutegia, irakaslea))
                .thenReturn(programazioa());
        Erronka erronka = new Erronka(); erronka.setId(30L); erronka.setIzena("Erronka 1");
        Moduloa modulua = new Moduloa(); modulua.setId(10L); modulua.setKodea("PROG");
        ErronkaKronogramaJarduera jatorria = new ErronkaKronogramaJarduera();
        jatorria.setId(101L); jatorria.setErronka(erronka); jatorria.setIzenburua("APIa");
        jatorria.setDeskribapena("Diseinua"); jatorria.setMota(KronogramaJardueraMota.MODULUA);
        jatorria.setModuloa(modulua);
        LocalDate data = LocalDate.of(2026, 9, 15);
        Authentication auth = mock(Authentication.class);
        when(kronogramaService.denboralizazioBanaketa(30L, auth)).thenReturn(
                new KronogramaService.DenboralizazioBanaketa(erronka, List.of(
                        new KronogramaService.DenboralizazioEgunJarduera(
                                jatorria, data, BigDecimal.valueOf(1.5)))));

        int kopurua = service.bolkatu(koadernoa, egutegia, irakaslea, 30L, auth);

        assertThat(kopurua).isEqualTo(1);
        verify(denboralizazioJarduerak).deleteByEthaziKoadernoaIdAndErronkaId(6L, 30L);
        verify(denboralizazioJarduerak).saveAll(argThat(gordetakoak -> {
            EthaziDenboralizazioJarduera gordea = gordetakoak.iterator().next();
            return "APIa".equals(gordea.getIzenburua())
                    && "Erronka 1".equals(gordea.getErronkaIzena())
                    && data.equals(gordea.getData())
                    && BigDecimal.valueOf(1.5).equals(gordea.getOrduak())
                    && gordea.getModuloa() == modulua;
        }));
    }

    @Test
    void norbereModulukoJardueraEditatuEtaMugituDaiteke() {
        Erronka erronka = new Erronka(); erronka.setId(30L); erronka.setIzena("Erronka 1");
        Moduloa modulua = new Moduloa(); modulua.setId(10L); modulua.setKodea("PROG");
        EthaziDenboralizazioJarduera jarduera = gordea(101L, "APIa",
                KronogramaJardueraMota.MODULUA, modulua, erronka, LocalDate.of(2026, 9, 15));
        when(denboralizazioJarduerak.findByIdAndEthaziKoadernoaId(101L, 6L))
                .thenReturn(java.util.Optional.of(jarduera));

        service.eguneratuJarduera(koadernoa, irakaslea, 101L, "API berria", "Xehetasuna",
                BigDecimal.valueOf(2.5), "Eginda");
        service.mugituJarduera(koadernoa, irakaslea, 101L, LocalDate.of(2026, 9, 16));

        assertThat(jarduera.getIzenburua()).isEqualTo("API berria");
        assertThat(jarduera.getOharrak()).isEqualTo("Eginda");
        assertThat(jarduera.getOrduak()).isEqualByComparingTo("2.5");
        assertThat(jarduera.getData()).isEqualTo(LocalDate.of(2026, 9, 16));
    }

    @Test
    void jardueraOrokorrariOharrakJarriBaiBainaEzinDaMugitu() {
        Erronka erronka = new Erronka(); erronka.setId(30L); erronka.setIzena("Erronka 1");
        EthaziDenboralizazioJarduera jarduera = gordea(103L, "Aurkezpena",
                KronogramaJardueraMota.KOMUNA, null, erronka, LocalDate.of(2026, 9, 15));
        when(denboralizazioJarduerak.findByIdAndEthaziKoadernoaId(103L, 6L))
                .thenReturn(java.util.Optional.of(jarduera));

        service.eguneratuJarduera(koadernoa, irakaslea, 103L, "Ez aldatu", "Ez aldatu",
                BigDecimal.TEN, "Ohar komuna");

        assertThat(jarduera.getIzenburua()).isEqualTo("Aurkezpena");
        assertThat(jarduera.getOharrak()).isEqualTo("Ohar komuna");
        assertThatThrownBy(() -> service.mugituJarduera(
                koadernoa, irakaslea, 103L, LocalDate.of(2026, 9, 16)))
                .isInstanceOf(AccessDeniedException.class);
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

    private List<EthaziDenboralizazioJarduera> gordetakoJarduerak() {
        LocalDate data = LocalDate.of(2026, 9, 15);
        Erronka erronka = new Erronka(); erronka.setId(30L); erronka.setIzena("Erronka 1");
        Moduloa a = new Moduloa(); a.setId(10L); a.setKodea("PROG");
        Moduloa b = new Moduloa(); b.setId(20L); b.setKodea("SARE");
        return List.of(gordea(101L, "A", KronogramaJardueraMota.MODULUA, a, erronka, data),
                gordea(102L, "B", KronogramaJardueraMota.MODULUA, b, erronka, data),
                gordea(103L, "Komuna", KronogramaJardueraMota.KOMUNA, null, erronka, data));
    }

    private EthaziDenboralizazioJarduera gordea(Long id, String izena,
            KronogramaJardueraMota mota, Moduloa modulua, Erronka erronka, LocalDate data) {
        EthaziDenboralizazioJarduera j = new EthaziDenboralizazioJarduera();
        j.setId(id); j.setIzenburua(izena); j.setMota(mota); j.setModuloa(modulua);
        j.setErronka(erronka); j.setErronkaIzena(erronka.getIzena());
        j.setData(data); j.setOrduak(BigDecimal.ONE);
        return j;
    }

    private TaldeOrdutegia ordutegia(LocalDate hasiera, TaldeOrdutegiLerroa... lerroak) {
        TaldeOrdutegia o = new TaldeOrdutegia();
        o.setHasieraData(hasiera); o.setLerroak(List.of(lerroak));
        return o;
    }

    private TaldeOrdutegiLerroa lerroa(Astegunak asteguna, int ordua, Moduloa modulua) {
        TaldeOrdutegiLerroa l = new TaldeOrdutegiLerroa();
        l.setAsteguna(asteguna); l.setOrduZenbakia(ordua); l.setModuloa(modulua);
        return l;
    }
}
