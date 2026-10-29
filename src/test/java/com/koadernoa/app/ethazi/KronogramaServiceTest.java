package com.koadernoa.app.ethazi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;

import com.koadernoa.app.ethazi.dto.KronogramaJardueraForm;
import com.koadernoa.app.ethazi.entitateak.Erronka;
import com.koadernoa.app.ethazi.entitateak.kronograma.ErronkaKronogramaJarduera;
import com.koadernoa.app.ethazi.entitateak.kronograma.KronogramaJardueraMota;
import com.koadernoa.app.ethazi.repository.ErronkaKronogramaJardueraRepository;
import com.koadernoa.app.ethazi.repository.ErronkaRepository;
import com.koadernoa.app.ethazi.service.KronogramaService;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunMota;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.egutegia.repository.EgutegiaRepository;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.TaldeOrdutegia;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.TaldeOrdutegiLerroa;
import com.koadernoa.app.objektuak.ordutegiak.repository.TaldeOrdutegiaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

@ExtendWith(MockitoExtension.class)
class KronogramaServiceTest {
    @Mock ErronkaRepository erronkak;
    @Mock ErronkaKronogramaJardueraRepository jarduerak;
    @Mock ModuloaRepository moduloak;
    @Mock IrakasleaRepository irakasleak;
    @Mock EgutegiaRepository egutegiak;
    @Mock TaldeOrdutegiaRepository taldeOrdutegiak;
    KronogramaService service;
    Erronka erronka;
    Moduloa sareak;
    Moduloa programazioa;

    @BeforeEach
    void setup() {
        service = new KronogramaService(erronkak, jarduerak, moduloak, irakasleak, egutegiak, taldeOrdutegiak);
        var year = new Ikasturtea(); year.setId(4L); year.setAktiboa(true);
        var maila = new Maila(); maila.setId(6L);
        sareak = modulua(10L, "SARE", "Sare lokalak");
        programazioa = modulua(20L, "PROG", "Programazioa");
        Taldea taldea = new Taldea(); taldea.setId(13L); taldea.setIzena("2SM2D");
        sareak.setTaldea(taldea); programazioa.setTaldea(taldea);
        erronka = new Erronka(); erronka.setId(7L); erronka.setIzena("Sareko zerbitzaria");
        erronka.setIkasturtea(year); erronka.setMaila(maila); erronka.setHasieraData(LocalDate.of(2026, 10, 5));
        erronka.setBukaeraData(LocalDate.of(2026, 10, 23));
        erronka.setModuluak(new LinkedHashSet<>(List.of(sareak, programazioa)));
        when(erronkak.findById(7L)).thenReturn(Optional.of(erronka));
    }

    @Test
    void koordinatzaileakJardueraKomunaSorDezake() {
        gordetzeakIdaEsleitzenDu();
        ordutegiaLehenetsia();
        var form = form(KronogramaJardueraMota.KOMUNA, null);
        form.setAurreikusitakoModuloaId(10L);

        assertThat(service.gorde(7L, form, auth("manager", "ROLE_KUDEATZAILEA"))).isEqualTo(99L);

        var captor = ArgumentCaptor.forClass(ErronkaKronogramaJarduera.class);
        verify(jarduerak).save(captor.capture());
        assertThat(captor.getValue().getModuloa()).isNull();
        assertThat(captor.getValue().getAurreikusitakoModuloa()).isSameAs(sareak);
    }

    @Test
    void irakasleakEzinDuJardueraKomunikSortu() {
        assertThatThrownBy(() -> service.gorde(7L, form(KronogramaJardueraMota.KOMUNA, null),
            auth("ane@example.com", "ROLE_IRAKASLEA"))).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void irakasleakBereModulukoJardueraSorDezakeBainaEzBesteModulukoa() {
        gordetzeakIdaEsleitzenDu();
        ordutegiaLehenetsia();
        var teacher = irakaslea();
        when(irakasleak.findByEmailaIgnoreCase("ane@example.com")).thenReturn(Optional.of(teacher));
        when(moduloak.existsByIdAndIrakasleak_Id(10L, 30L)).thenReturn(true);
        var auth = auth("ane@example.com", "ROLE_IRAKASLEA");

        assertThat(service.gorde(7L, form(KronogramaJardueraMota.MODULUA, 10L), auth)).isEqualTo(99L);
        assertThatThrownBy(() -> service.gorde(7L, form(KronogramaJardueraMota.MODULUA, 20L), auth))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void jardueraEzinDaErronkarenDatetatikKanpoGorde() {
        var form = form(KronogramaJardueraMota.KOMUNA, null);
        form.setHasieraData(LocalDate.of(2026, 10, 24));

        assertThatThrownBy(() -> service.gorde(7L, form, auth("manager", "ROLE_ADMIN")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("erronkaren daten barruan");
    }

    @Test
    void ikuspegiakErronkaHorretakoJarduerakBakarrikEskatzenDituEtaAsteakSortzenDitu() {
        ordutegiaLehenetsia();
        when(jarduerak.findByErronkaIdOrderByHasieraDataAscBukaeraDataAscIdAsc(7L)).thenReturn(List.of());
        var view = service.ikuspegia(7L, auth("manager", "ROLE_ADMIN"));

        verify(jarduerak).findByErronkaIdOrderByHasieraDataAscBukaeraDataAscIdAsc(7L);
        assertThat(view.asteak()).hasSize(3);
        assertThat(view.asteak()).allSatisfy(week -> assertThat(week.lerroak()).hasSize(3));
        assertThat(view.asteak()).flatExtracting(w -> w.egunak())
                .allSatisfy(data -> assertThat(data.getDayOfWeek())
                        .isNotIn(java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY));
    }

    @Test
    void moduloOrduakTaldeOrdutegiHistorikotikEtaEgutegiarekinKalkulatzenDitu() {
        Taldea taldea = sareak.getTaldea();
        sareak.setKodea("BUAA");
        erronka.setModuluak(new LinkedHashSet<>(List.of(sareak)));
        erronka.setHasieraData(LocalDate.of(2026, 10, 6)); // asteartea
        erronka.setBukaeraData(LocalDate.of(2026, 10, 15)); // osteguna

        TaldeOrdutegia ordutegia = taldeOrdutegia(LocalDate.of(2026, 9, 1), sareak,
                Astegunak.ASTELEHENA, 1, Astegunak.ASTEARTEA, 1,
                Astegunak.ASTEARTEA, 2, Astegunak.OSTEGUNA, 3);
        TaldeOrdutegia ordutegiBerria = taldeOrdutegia(LocalDate.of(2026, 10, 14), sareak,
                Astegunak.ASTEAZKENA, 1);
        ordutegiBerria.setId(51L);
        when(taldeOrdutegiak.findByTaldea_IdAndIkasturtea_IdOrderByHasieraDataAsc(13L, 4L))
                .thenReturn(List.of(ordutegia, ordutegiBerria));

        Egutegia egutegia = new Egutegia();
        egutegia.setHasieraData(LocalDate.of(2026, 9, 1));
        egutegia.setBukaeraData(LocalDate.of(2027, 6, 30));
        EgunBerezi jaia = new EgunBerezi(); jaia.setData(LocalDate.of(2026, 10, 13)); jaia.setMota(EgunMota.JAIEGUNA);
        egutegia.setEgunBereziak(new java.util.ArrayList<>(List.of(jaia)));
        when(egutegiak.findByIkasturtea_IdAndMaila_Id(4L, 6L)).thenReturn(Optional.of(egutegia));
        when(jarduerak.findByErronkaIdOrderByHasieraDataAscBukaeraDataAscIdAsc(7L)).thenReturn(List.of());

        var view = service.ikuspegia(7L, auth("manager", "ROLE_ADMIN"));
        var laburpena = view.asteak().get(0).lerroak().get(1).orduLaburpena();

        assertThat(laburpena.astekoOrduak()).isEqualTo(4);
        assertThat(laburpena.erronkakoOrduak()).isEqualTo(5);
    }

    @Test
    void biOrdukoJardueraAstelehenetikAsteazkeneraBanatzenDuAstearteanKlaserikEzBadago() {
        gordetzeakIdaEsleitzenDu();
        TaldeOrdutegia ordutegia = taldeOrdutegia(LocalDate.of(2026, 9, 1), sareak,
                Astegunak.ASTELEHENA, 1, Astegunak.ASTEAZKENA, 1);
        when(taldeOrdutegiak.findByTaldea_IdAndIkasturtea_IdOrderByHasieraDataAsc(13L, 4L))
                .thenReturn(List.of(ordutegia));
        var form = form(KronogramaJardueraMota.MODULUA, 10L);
        form.setHasieraData(LocalDate.of(2026, 10, 5));
        form.setIraupenOrduak(new BigDecimal("2.00"));

        service.gorde(7L, form, auth("manager", "ROLE_ADMIN"));

        var captor = ArgumentCaptor.forClass(ErronkaKronogramaJarduera.class);
        verify(jarduerak).save(captor.capture());
        assertThat(captor.getValue().getBukaeraData()).isEqualTo(LocalDate.of(2026, 10, 7));
    }

    @Test
    void jardueraKomunakModulukoOrduaErreserbatuEtaJardueraEspezifikoaHurrengoKlaseEguneraLuzatzenDu() {
        gordetzeakIdaEsleitzenDu();
        TaldeOrdutegia ordutegia = taldeOrdutegia(LocalDate.of(2026, 9, 1), sareak,
                Astegunak.ASTELEHENA, 1, Astegunak.ASTELEHENA, 2);
        when(taldeOrdutegiak.findByTaldea_IdAndIkasturtea_IdOrderByHasieraDataAsc(13L, 4L))
                .thenReturn(List.of(ordutegia));
        ErronkaKronogramaJarduera komuna = new ErronkaKronogramaJarduera();
        komuna.setId(80L); komuna.setErronka(erronka); komuna.setMota(KronogramaJardueraMota.KOMUNA);
        komuna.setAurreikusitakoModuloa(sareak); komuna.setIzenburua("Aurkezpen komuna");
        komuna.setHasieraData(LocalDate.of(2026, 10, 5)); komuna.setBukaeraData(LocalDate.of(2026, 10, 5));
        komuna.setIraupenOrduak(new BigDecimal("1.00"));
        when(jarduerak.findByErronkaIdOrderByHasieraDataAscBukaeraDataAscIdAsc(7L))
                .thenReturn(List.of(komuna));
        var form = form(KronogramaJardueraMota.MODULUA, 10L);
        form.setHasieraData(LocalDate.of(2026, 10, 5));
        form.setIraupenOrduak(new BigDecimal("2.00"));

        service.gorde(7L, form, auth("manager", "ROLE_ADMIN"));

        var captor = ArgumentCaptor.forClass(ErronkaKronogramaJarduera.class);
        verify(jarduerak).save(captor.capture());
        ErronkaKronogramaJarduera modulukoJarduera = captor.getValue();
        assertThat(modulukoJarduera.getBukaeraData()).isEqualTo(LocalDate.of(2026, 10, 12));

        when(jarduerak.findByErronkaIdOrderByHasieraDataAscBukaeraDataAscIdAsc(7L))
                .thenReturn(List.of(komuna, modulukoJarduera));
        var ikuspegia = service.ikuspegia(7L, auth("manager", "ROLE_ADMIN"));
        assertThat(ikuspegia.asteak().get(0).lerroak().get(0).blokeak()).isEmpty();
        var lehenAstekoSareak = ikuspegia.asteak().get(0).lerroak().stream()
                .filter(l -> Long.valueOf(10L).equals(l.moduloaId())).findFirst().orElseThrow();
        assertThat(lehenAstekoSareak.blokeak())
                .extracting(b -> b.jarduera().getMota(), b -> b.orduak())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(KronogramaJardueraMota.KOMUNA, new BigDecimal("1.00")),
                        org.assertj.core.groups.Tuple.tuple(KronogramaJardueraMota.MODULUA, new BigDecimal("1.00")));
        var bigarrenAstekoSareak = ikuspegia.asteak().get(1).lerroak().stream()
                .filter(l -> Long.valueOf(10L).equals(l.moduloaId())).findFirst().orElseThrow();
        assertThat(bigarrenAstekoSareak.blokeak()).singleElement()
                .satisfies(b -> assertThat(b.orduak()).isEqualByComparingTo("1.00"));
    }

    private KronogramaJardueraForm form(KronogramaJardueraMota mota, Long moduloaId) {
        var form = new KronogramaJardueraForm();
        form.setIzenburua("Jarduera"); form.setMota(mota); form.setModuloaId(moduloaId);
        form.setHasieraData(LocalDate.of(2026, 10, 7));
        form.setIraupenOrduak(new BigDecimal("2.5"));
        return form;
    }

    private Moduloa modulua(Long id, String kodea, String izena) {
        var m = new Moduloa(); m.setId(id); m.setKodea(kodea); m.setIzena(izena); return m;
    }

    private Irakaslea irakaslea() {
        var i = new Irakaslea(); i.setId(30L); i.setIzena("Ane"); i.setEmaila("ane@example.com"); return i;
    }

    private void ordutegiaLehenetsia() {
        TaldeOrdutegia ordutegia = taldeOrdutegia(LocalDate.of(2026, 9, 1), sareak,
                Astegunak.ASTEAZKENA, 1, Astegunak.ASTEAZKENA, 2, Astegunak.ASTEAZKENA, 3);
        ordutegia.getLerroak().add(lerroa(ordutegia, programazioa, Astegunak.ASTEAZKENA, 4));
        ordutegia.getLerroak().add(lerroa(ordutegia, programazioa, Astegunak.ASTEAZKENA, 5));
        ordutegia.getLerroak().add(lerroa(ordutegia, programazioa, Astegunak.ASTEAZKENA, 6));
        when(taldeOrdutegiak.findByTaldea_IdAndIkasturtea_IdOrderByHasieraDataAsc(13L, 4L))
                .thenReturn(List.of(ordutegia));
    }

    private TaldeOrdutegia taldeOrdutegia(LocalDate hasiera, Moduloa moduloa, Object... egunaOrdua) {
        TaldeOrdutegia ordutegia = new TaldeOrdutegia(); ordutegia.setId(50L);
        ordutegia.setTaldea(moduloa.getTaldea()); ordutegia.setIkasturtea(erronka.getIkasturtea());
        ordutegia.setHasieraData(hasiera);
        for (int i = 0; i < egunaOrdua.length; i += 2) {
            ordutegia.getLerroak().add(lerroa(ordutegia, moduloa,
                    (Astegunak) egunaOrdua[i], (Integer) egunaOrdua[i + 1]));
        }
        return ordutegia;
    }

    private TaldeOrdutegiLerroa lerroa(TaldeOrdutegia ordutegia, Moduloa moduloa,
            Astegunak eguna, int ordua) {
        var lerroa = new TaldeOrdutegiLerroa(); lerroa.setTaldeOrdutegia(ordutegia);
        lerroa.setModuloa(moduloa); lerroa.setAsteguna(eguna); lerroa.setOrduZenbakia(ordua);
        return lerroa;
    }

    private void gordetzeakIdaEsleitzenDu() {
        when(jarduerak.save(any())).thenAnswer(invocation -> {
            ErronkaKronogramaJarduera saved = invocation.getArgument(0); saved.setId(99L); return saved;
        });
    }

    private TestingAuthenticationToken auth(String principal, String role) {
        return new TestingAuthenticationToken(principal, null, role);
    }
}
