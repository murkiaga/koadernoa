package com.koadernoa.app.ethazi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.koadernoa.app.ethazi.entitateak.Erronka;
import com.koadernoa.app.ethazi.entitateak.ErronkaTaldeKidea;
import com.koadernoa.app.ethazi.entitateak.ErronkaTaldea;
import com.koadernoa.app.ethazi.entitateak.EthaziJarraipenGelaxka;
import com.koadernoa.app.ethazi.entitateak.EthaziKoadernoa;
import com.koadernoa.app.ethazi.repository.ErronkaRepository;
import com.koadernoa.app.ethazi.repository.ErronkaTaldeKideaRepository;
import com.koadernoa.app.ethazi.repository.EthaziJarraipenGelaxkaRepository;
import com.koadernoa.app.ethazi.service.EthaziJarraipenaService;
import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.egutegia.repository.EgutegiaRepository;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.jokabidea.entitateak.IkasleEgunOharra;
import com.koadernoa.app.objektuak.jokabidea.repository.IkasleEgunOharraRepository;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Asistentzia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa;
import com.koadernoa.app.objektuak.koadernoak.repository.AsistentziaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.SaioaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.repository.IkasleaRepository;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.TaldeOrdutegiLerroa;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.TaldeOrdutegia;
import com.koadernoa.app.objektuak.ordutegiak.repository.TaldeOrdutegiaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;

@ExtendWith(MockitoExtension.class)
class EthaziJarraipenaServiceTest {
    @Mock IkasturteaRepository ikasturteak;
    @Mock ErronkaRepository erronkak;
    @Mock ErronkaTaldeKideaRepository taldeKideak;
    @Mock EthaziJarraipenGelaxkaRepository gelaxkak;
    @Mock IkasleaRepository ikasleak;
    @Mock ModuloaRepository moduluak;
    @Mock TaldeOrdutegiaRepository taldeOrdutegiak;
    @Mock EgutegiaRepository egutegiak;
    @Mock SaioaRepository saioak;
    @Mock AsistentziaRepository asistentziak;
    @Mock IkasleEgunOharraRepository egunOharrak;

    private EthaziJarraipenaService service;
    private EthaziKoadernoa koadernoa;
    private Ikasturtea ikasturtea;
    private Egutegia egutegia;
    private Maila maila;
    private Moduloa prog;
    private Moduloa datu;
    private Erronka erronka;
    private Ikaslea ane;
    private Ikaslea beñat;
    private Irakaslea irakaslea;
    private TaldeOrdutegia ordutegia;

    @BeforeEach
    void prestatu() {
        service = new EthaziJarraipenaService(ikasturteak, erronkak, taldeKideak, gelaxkak,
                ikasleak, moduluak, taldeOrdutegiak, egutegiak, saioak, asistentziak, egunOharrak);
        Zikloa zikloa = new Zikloa(); zikloa.setId(7L);
        Taldea taldea = new Taldea(); taldea.setId(4L); taldea.setIzena("1DAM"); taldea.setZikloa(zikloa);
        koadernoa = new EthaziKoadernoa(); koadernoa.setId(6L); koadernoa.setTaldea(taldea);
        maila = new Maila(); maila.setId(2L); maila.setFaltenMugaPortzentaia(20);
        ikasturtea = new Ikasturtea(); ikasturtea.setId(3L); ikasturtea.setIzena("2026-2027");
        ikasturtea.setAktiboa(true);
        egutegia = new Egutegia(); egutegia.setId(8L); egutegia.setMaila(maila); egutegia.setIkasturtea(ikasturtea);
        egutegia.setHasieraData(LocalDate.of(2026, 9, 1));
        egutegia.setBukaeraData(LocalDate.of(2027, 6, 30)); egutegia.setEgunBereziak(List.of());
        prog = modulua(10L, "PROG", taldea);
        datu = modulua(11L, "DATU", taldea);
        erronka = new Erronka(); erronka.setId(20L); erronka.setIzena("Lehen erronka");
        erronka.setZikloa(zikloa); erronka.setMaila(maila); erronka.setIkasturtea(ikasturtea);
        erronka.setHizkuntza(Hizkuntza.EUSKARA); erronka.setHasieraData(LocalDate.of(2026, 11, 2));
        erronka.setBukaeraData(LocalDate.of(2026, 11, 3));
        erronka.setModuluak(new LinkedHashSet<>(List.of(prog, datu)));
        ane = ikaslea(30L, "Ane", "Alberdi", taldea);
        beñat = ikaslea(31L, "Beñat", "Bengoetxea", taldea);
        irakaslea = new Irakaslea(); irakaslea.setId(40L);
        ordutegia = new TaldeOrdutegia(); ordutegia.setId(50L); ordutegia.setTaldea(taldea);
        ordutegia.setIkasturtea(ikasturtea); ordutegia.setHasieraData(LocalDate.of(2026, 9, 1));
        ordutegia.setLerroak(List.of(
                lerroa(Astegunak.ASTELEHENA, 1, prog),
                lerroa(Astegunak.ASTELEHENA, 2, datu),
                lerroa(Astegunak.ASTEARTEA, 1, prog)));

        lenient().when(ikasturteak.findFirstByAktiboaTrueOrderByIdDesc()).thenReturn(Optional.of(ikasturtea));
        lenient().when(moduluak.findByTaldeaId(4L)).thenReturn(List.of(prog, datu));
        lenient().when(moduluak.findAktiboakByTaldeaIdAndIrakasleaId(4L, 40L)).thenReturn(List.of(prog));
        lenient().when(erronkak.findWithModuluakByZikloaIdAndIkasturteaId(7L, 3L)).thenReturn(List.of(erronka));
        lenient().when(taldeOrdutegiak.findByTaldea_IdAndIkasturtea_IdOrderByHasieraDataAsc(4L, 3L))
                .thenReturn(List.of(ordutegia));
        lenient().when(ikasleak.findByTaldea_IdOrderByAbizena1AscAbizena2AscIzenaAsc(4L))
                .thenReturn(List.of(ane, beñat));
    }

    @Test
    void asistentziaFaltaJarraipenekoGelaxkarekinSinkronizatzenDu() {
        Koadernoa modulukoKoadernoa = new Koadernoa();
        modulukoKoadernoa.setId(70L); modulukoKoadernoa.setModuloa(prog);
        modulukoKoadernoa.setEgutegia(egutegia);
        LocalDate data = LocalDate.of(2026, 11, 2);
        Saioa saioa = new Saioa(); saioa.setId(80L); saioa.setKoadernoa(modulukoKoadernoa);
        saioa.setData(data); saioa.setHasieraSlot(1); saioa.setIraupenaSlot(1);
        Matrikula matrikula = new Matrikula(); matrikula.setId(90L);
        matrikula.setKoadernoa(modulukoKoadernoa); matrikula.setIkaslea(ane);
        Asistentzia falta = new Asistentzia(); falta.setSaioa(saioa); falta.setMatrikula(matrikula);
        falta.setEgoera(Asistentzia.AsistentziaEgoera.HUTS);
        when(saioak.findByKoadernoaIdAndData(70L, data)).thenReturn(List.of(saioa));
        when(asistentziak.findBySaioaInAndMatrikulaIn(List.of(saioa), List.of(matrikula)))
                .thenReturn(List.of(falta));
        when(gelaxkak.findByEthaziKoadernoaIdAndErronkaIdAndIkasleaIdAndModuloaIdAndData(
                6L, 20L, 30L, 10L, data)).thenReturn(Optional.empty());

        service.sinkronizatuAsistentzia(koadernoa, modulukoKoadernoa, saioa, matrikula);

        verify(gelaxkak).save(org.mockito.ArgumentMatchers.argThat(g ->
                g.getErronka() == erronka && g.getIkaslea() == ane && g.getModuloa() == prog
                        && g.getFaltaOrduak().compareTo(BigDecimal.ONE) == 0));
    }

    @Test
    void orokorrakModuluGuztienOrduakEtaFaltakBatzenDituEtaTaldekaOrdenatzenDu() {
        ErronkaTaldea bat = erronkaTaldea(60L, 1, "1. taldea");
        ErronkaTaldea bi = erronkaTaldea(61L, 2, "2. taldea");
        when(taldeKideak.findByErronkaId(20L)).thenReturn(List.of(kidea(beñat, bi), kidea(ane, bat)));
        when(gelaxkak.findByEthaziKoadernoaIdAndErronkaIdIn(6L, List.of(20L))).thenReturn(List.of(
                gelaxka(ane, prog, new BigDecimal("0.50")),
                gelaxka(ane, datu, new BigDecimal("0.50"))));

        var ikuspegia = service.prestatu(koadernoa, egutegia, irakaslea, false, null);

        assertThat(ikuspegia.erronkak()).singleElement().satisfies(e -> {
            assertThat(e.programaOrduak()).isEqualTo(3);
            assertThat(e.lehenetsitaUzkurtuta()).isTrue();
            assertThat(e.egunak()).extracting(EthaziJarraipenaService.EgunZutabea::data)
                    .containsExactly(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 3));
        });
        assertThat(ikuspegia.ikasleak()).extracting(EthaziJarraipenaService.IkasleLerroa::izenOsoa)
                .containsExactly("Alberdi, Ane", "Bengoetxea, Beñat");
        var laburpena = ikuspegia.ikasleak().get(0).laburpena(20L);
        assertThat(laburpena.faltaOrduak()).isEqualByComparingTo("1.00");
        assertThat(laburpena.portzentaia()).isCloseTo(33.33, org.assertj.core.data.Offset.offset(0.01));
        assertThat(laburpena.suspendituta()).isTrue();
        assertThat(ikuspegia.ikasleak().get(0).taldea()).isEqualTo("1. taldea");
    }

    @Test
    void moduluFiltroakModuluarenOrduakBakarrikErabiltzenDitu() {
        when(taldeKideak.findByErronkaId(20L)).thenReturn(List.of());
        when(gelaxkak.findByEthaziKoadernoaIdAndErronkaIdIn(6L, List.of(20L)))
                .thenReturn(List.of(gelaxka(ane, prog, new BigDecimal("0.50"))));

        var ikuspegia = service.prestatu(koadernoa, egutegia, irakaslea, false, 10L);

        assertThat(ikuspegia.aukeratutakoModuloaId()).isEqualTo(10L);
        assertThat(ikuspegia.erronkak().get(0).programaOrduak()).isEqualTo(2);
        assertThat(ikuspegia.ikasleak().get(0).laburpena(20L).portzentaia()).isEqualTo(25d);
        assertThat(ikuspegia.ikasleak().get(0).laburpena(20L).egoeraEtiketa())
                .isEqualTo("Modulua suspendituta");
    }

    @Test
    void oharIkonoakAsistentziakoOharrakErabiltzenDituEtaEzJarraipenZaharrekoak() {
        Koadernoa progKoadernoa = new Koadernoa(); progKoadernoa.setId(70L);
        progKoadernoa.setModuloa(prog); progKoadernoa.setEgutegia(egutegia);
        IkasleEgunOharra asistentziaOharra = new IkasleEgunOharra();
        asistentziaOharra.setIkaslea(ane); asistentziaOharra.setKoadernoa(progKoadernoa);
        asistentziaOharra.setData(LocalDate.of(2026, 11, 2));
        asistentziaOharra.setTestua("Asistentzian idatzitako oharra");
        Koadernoa datuKoadernoa = new Koadernoa(); datuKoadernoa.setId(71L);
        datuKoadernoa.setModuloa(datu); datuKoadernoa.setEgutegia(egutegia);
        IkasleEgunOharra bigarrenOharra = new IkasleEgunOharra();
        bigarrenOharra.setIkaslea(ane); bigarrenOharra.setKoadernoa(datuKoadernoa);
        bigarrenOharra.setData(LocalDate.of(2026, 11, 2));
        bigarrenOharra.setTestua("Beste moduluko oharra");
        EthaziJarraipenGelaxka zaharra = gelaxka(beñat, prog, BigDecimal.ZERO);
        zaharra.setOharra("Jarraipen zaharreko oharra");
        when(gelaxkak.findByEthaziKoadernoaIdAndErronkaIdIn(6L, List.of(20L)))
                .thenReturn(List.of(zaharra));
        when(egunOharrak
                .findByKoadernoa_Moduloa_Taldea_IdAndKoadernoa_Egutegia_Ikasturtea_IdAndDataBetween(
                        4L, 3L, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 3)))
                .thenReturn(List.of(asistentziaOharra, bigarrenOharra));

        var ikuspegia = service.prestatu(koadernoa, egutegia, irakaslea, false, null);
        var anerenGelaxka = ikuspegia.ikasleak().stream().filter(i -> i.id().equals(30L))
                .findFirst().orElseThrow().gelaxka(20L, LocalDate.of(2026, 11, 2));
        var benatenGelaxka = ikuspegia.ikasleak().stream().filter(i -> i.id().equals(31L))
                .findFirst().orElseThrow().gelaxka(20L, LocalDate.of(2026, 11, 2));

        assertThat(anerenGelaxka.oharra()).isTrue();
        assertThat(anerenGelaxka.oharrak()).extracting(EthaziJarraipenaService.OharXehetasuna::modulua)
                .containsExactly("PROG", "DATU");
        assertThat(benatenGelaxka.oharra()).isFalse();

        var modulukoIkuspegia = service.prestatu(koadernoa, egutegia, irakaslea, false, 10L);
        var modulukoOharrak = modulukoIkuspegia.ikasleak().stream().filter(i -> i.id().equals(30L))
                .findFirst().orElseThrow().gelaxka(20L, LocalDate.of(2026, 11, 2)).oharrak();
        assertThat(modulukoOharrak).singleElement().satisfies(o -> {
            assertThat(o.modulua()).isEqualTo("PROG");
            assertThat(o.testua()).isEqualTo("Asistentzian idatzitako oharra");
        });
    }

    @Test
    void ezinDaEgunekoModuluOrduakGainditzenDituenFaltarikGorde() {
        when(ikasleak.findById(30L)).thenReturn(Optional.of(ane));
        when(erronkak.findWithModuluakById(20L)).thenReturn(Optional.of(erronka));
        when(egutegiak.findByIkasturtea_IdAndMaila_Id(3L, 2L)).thenReturn(Optional.of(egutegia));
        var eskaera = new EthaziJarraipenaService.GelaxkaGordeEskaera(30L, 20L,
                LocalDate.of(2026, 11, 2), 10L, List.of(
                        new EthaziJarraipenaService.ModuloGelaxkaGorde(10L, new BigDecimal("1.25"), "")));

        assertThatThrownBy(() -> service.gordeGelaxka(koadernoa, irakaslea, false, eskaera))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 eta 1 artekoak");
    }

    @Test
    void faltaEtaOharraGordetzenDitu() {
        when(ikasleak.findById(30L)).thenReturn(Optional.of(ane));
        when(erronkak.findWithModuluakById(20L)).thenReturn(Optional.of(erronka));
        when(egutegiak.findByIkasturtea_IdAndMaila_Id(3L, 2L)).thenReturn(Optional.of(egutegia));
        when(gelaxkak.findByEthaziKoadernoaIdAndErronkaIdAndIkasleaIdAndModuloaIdAndData(
                6L, 20L, 30L, 10L, LocalDate.of(2026, 11, 2))).thenReturn(Optional.empty());
        var eskaera = new EthaziJarraipenaService.GelaxkaGordeEskaera(30L, 20L,
                LocalDate.of(2026, 11, 2), 10L, List.of(
                        new EthaziJarraipenaService.ModuloGelaxkaGorde(10L, new BigDecimal("0.50"), " Medikuarengana ")));

        service.gordeGelaxka(koadernoa, irakaslea, false, eskaera);

        verify(gelaxkak).save(any(EthaziJarraipenGelaxka.class));
    }

    @Test
    void orokorrekoXehetasunakKontsultaSoilikDira() {
        when(ikasleak.findById(30L)).thenReturn(Optional.of(ane));
        when(erronkak.findWithModuluakById(20L)).thenReturn(Optional.of(erronka));
        when(egutegiak.findByIkasturtea_IdAndMaila_Id(3L, 2L)).thenReturn(Optional.of(egutegia));

        var xehetasuna = service.gelaxkaXehetasuna(koadernoa, irakaslea, false,
                30L, 20L, LocalDate.of(2026, 11, 2), null);

        assertThat(xehetasuna.moduluak()).hasSize(2)
                .allSatisfy(g -> assertThat(g.editagarria()).isFalse());
    }

    @Test
    void orokorretikEzinDaGelaxkarikGorde() {
        var eskaera = new EthaziJarraipenaService.GelaxkaGordeEskaera(30L, 20L,
                LocalDate.of(2026, 11, 2), null, List.of(
                        new EthaziJarraipenaService.ModuloGelaxkaGorde(10L, new BigDecimal("0.50"), "Oharra")));

        assertThatThrownBy(() -> service.gordeGelaxka(koadernoa, irakaslea, false, eskaera))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("modulu konkretu bat aukeratu");
    }

    private Moduloa modulua(Long id, String kodea, Taldea taldea) {
        Moduloa m = new Moduloa(); m.setId(id); m.setKodea(kodea); m.setIzena(kodea);
        m.setTaldea(taldea); m.setMaila(maila); m.setAktibo(true); return m;
    }

    private Ikaslea ikaslea(Long id, String izena, String abizena, Taldea taldea) {
        Ikaslea i = new Ikaslea(); i.setId(id); i.setIzena(izena); i.setAbizena1(abizena); i.setTaldea(taldea); return i;
    }

    private TaldeOrdutegiLerroa lerroa(Astegunak asteguna, int ordua, Moduloa modulua) {
        TaldeOrdutegiLerroa l = new TaldeOrdutegiLerroa(); l.setAsteguna(asteguna);
        l.setOrduZenbakia(ordua); l.setModuloa(modulua); l.setTaldeOrdutegia(ordutegia); return l;
    }

    private ErronkaTaldea erronkaTaldea(Long id, int ordena, String izena) {
        ErronkaTaldea t = new ErronkaTaldea(); t.setId(id); t.setErronka(erronka);
        t.setOrdena(ordena); t.setIzena(izena); return t;
    }

    private ErronkaTaldeKidea kidea(Ikaslea ikaslea, ErronkaTaldea taldea) {
        ErronkaTaldeKidea k = new ErronkaTaldeKidea(); k.setErronka(erronka);
        k.setIkaslea(ikaslea); k.setTaldea(taldea); return k;
    }

    private EthaziJarraipenGelaxka gelaxka(Ikaslea ikaslea, Moduloa modulua, BigDecimal orduak) {
        EthaziJarraipenGelaxka g = new EthaziJarraipenGelaxka(); g.setEthaziKoadernoa(koadernoa);
        g.setErronka(erronka); g.setIkaslea(ikaslea); g.setModuloa(modulua);
        g.setData(LocalDate.of(2026, 11, 2)); g.setFaltaOrduak(orduak); return g;
    }
}
