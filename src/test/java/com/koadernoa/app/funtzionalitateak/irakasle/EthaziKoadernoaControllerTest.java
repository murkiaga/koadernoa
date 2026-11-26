package com.koadernoa.app.funtzionalitateak.irakasle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.mock.web.MockHttpSession;

import com.koadernoa.app.ethazi.entitateak.EthaziKoadernoa;
import com.koadernoa.app.ethazi.service.EthaziKoadernoaService;
import com.koadernoa.app.ethazi.service.EthaziDenboralizazioaService;
import com.koadernoa.app.ethazi.service.EthaziJarraipenaService;
import com.koadernoa.app.ethazi.service.KronogramaService;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.egutegia.repository.EgutegiaRepository;
import com.koadernoa.app.objektuak.egutegia.service.EgutegiaService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.repository.IkasleaRepository;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

@ExtendWith(MockitoExtension.class)
class EthaziKoadernoaControllerTest {
    @Mock EthaziKoadernoaService koadernoaService;
    @Mock IrakasleaService irakasleaService;
    @Mock IkasleaRepository ikasleak;
    @Mock ModuloaRepository moduluak;
    @Mock EgutegiaRepository egutegiak;
    @Mock EgutegiaService egutegiaService;
    @Mock KronogramaService kronogramaService;
    @Mock EthaziJarraipenaService ethaziJarraipenaService;
    @Mock EthaziDenboralizazioaService ethaziDenboralizazioaService;
    @Mock Authentication auth;

    private EthaziKoadernoaController controller;
    private EthaziKoadernoa koadernoa;
    private Irakaslea irakaslea;

    @BeforeEach
    void prestatu() {
        controller = new EthaziKoadernoaController(koadernoaService, irakasleaService, ikasleak,
                moduluak, egutegiak, egutegiaService, kronogramaService, ethaziJarraipenaService,
                ethaziDenboralizazioaService);
        Taldea taldea = new Taldea(); taldea.setId(4L); taldea.setIzena("1MS2D");
        koadernoa = new EthaziKoadernoa(); koadernoa.setId(6L); koadernoa.setTaldea(taldea);
        irakaslea = new Irakaslea(); irakaslea.setId(8L);
        when(auth.getAuthorities()).thenReturn(List.of());
        when(koadernoaService.findById(6L)).thenReturn(koadernoa);
        when(irakasleaService.getLogeatutaDagoenIrakaslea(auth)).thenReturn(irakaslea);
        when(koadernoaService.irakasleakBadaukaSarbidea(irakaslea, koadernoa)).thenReturn(true);
    }

    @Test
    void taldekoIkasleGuztiakAbizenenAraberaKargatzenDitu() {
        Ikaslea ikaslea = new Ikaslea(); ikaslea.setId(10L); ikaslea.setIzena("Ane");
        when(ikasleak.findByTaldea_IdOrderByAbizena1AscAbizena2AscIzenaAsc(4L)).thenReturn(List.of(ikaslea));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.ikasleak(auth, koadernoa, new MockHttpSession(), model,
                new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("irakasleak/ethazi/ikasleak");
        assertThat(model.get("ikasleak")).isEqualTo(List.of(ikaslea));
        assertThat(model.get("kop")).isEqualTo(1);
    }

    @Test
    void taldekoMailarenEgutegiAktiboaKargatzenDu() {
        Maila maila = new Maila(); maila.setId(2L);
        Moduloa modulua = new Moduloa(); modulua.setId(20L); modulua.setMaila(maila); modulua.setAktibo(true);
        Ikasturtea ikasturtea = new Ikasturtea(); ikasturtea.setIzena("2026-2027");
        Egutegia egutegia = new Egutegia(); egutegia.setId(30L); egutegia.setIkasturtea(ikasturtea);
        egutegia.setHasieraData(LocalDate.of(2026, 9, 1)); egutegia.setBukaeraData(LocalDate.of(2027, 6, 30));
        egutegia.setEgunBereziak(List.of());
        when(moduluak.findByTaldeaId(4L)).thenReturn(List.of(modulua));
        when(egutegiak.findByIkasturtea_AktiboaTrueAndMaila_Id(2L)).thenReturn(Optional.of(egutegia));
        when(egutegiaService.prestatuHilabetekoEgutegiak(egutegia)).thenReturn(Map.of());
        when(egutegiaService.kalkulatuKlaseak(egutegia)).thenReturn(Map.of());
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.egutegia(auth, koadernoa, new MockHttpSession(), model,
                new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("irakasleak/ethazi/egutegia");
        assertThat(model.get("egutegia")).isSameAs(egutegia);
        assertThat(model.get("ikasturtea")).isSameAs(ikasturtea);
    }

    @Test
    void programazioIkuspegiaKoadernoEtaTaldeEgutegiarekinKargatzenDu() {
        Maila maila = new Maila(); maila.setId(2L);
        Moduloa modulua = new Moduloa(); modulua.setMaila(maila); modulua.setAktibo(true);
        Egutegia egutegia = new Egutegia();
        var ikuspegia = new EthaziKoadernoaService.ProgramazioaIkuspegia(
                "2026-2027", List.of(), List.of(), List.of());
        when(moduluak.findByTaldeaId(4L)).thenReturn(List.of(modulua));
        when(egutegiak.findByIkasturtea_AktiboaTrueAndMaila_Id(2L)).thenReturn(Optional.of(egutegia));
        when(koadernoaService.programazioaIkuspegia(koadernoa, egutegia, irakaslea)).thenReturn(ikuspegia);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.programazioa(auth, koadernoa, new MockHttpSession(), model,
                new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("irakasleak/ethazi/programazioa");
        assertThat(model.get("programazioaIkuspegia")).isSameAs(ikuspegia);
    }

    @Test
    void ebaluazioakIrakaslearenLehenErronkaEtaModuluraBideratzenDu() {
        var modulua = new EthaziKoadernoaService.ModuluAukera(33L, "BUAA");
        var erronka = new EthaziKoadernoaService.ErronkaTxartela(4L, 1, "Abiadura-kaxa", null,
                null, null, List.of("BUAA"), List.of(), List.of(modulua), List.of(), List.of(), List.of());
        var ikuspegia = new EthaziKoadernoaService.ProgramazioaIkuspegia(
                "2026-2027", List.of(erronka), List.of(), List.of(modulua));
        when(moduluak.findByTaldeaId(4L)).thenReturn(List.of());
        when(koadernoaService.programazioaIkuspegia(koadernoa, null, irakaslea)).thenReturn(ikuspegia);

        String view = controller.ebaluazioa(auth, koadernoa, null, null, "TEKNIKOA",
                new MockHttpSession(), new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/ethazi/erronkak/4/errubrikak"
                + "?moduloaId=33&gaitasunMota=TEKNIKOA&koadernoEbaluazioa=true");
    }

    @Test
    void ebaluazioakMezuBistaErakustenDuEbaluatzekoErronkarikEzBadago() {
        var ikuspegia = new EthaziKoadernoaService.ProgramazioaIkuspegia(
                "2026-2027", List.of(), List.of(), List.of());
        when(moduluak.findByTaldeaId(4L)).thenReturn(List.of());
        when(koadernoaService.programazioaIkuspegia(koadernoa, null, irakaslea)).thenReturn(ikuspegia);

        String view = controller.ebaluazioa(auth, koadernoa, null, null, "TEKNIKOA",
                new MockHttpSession(), new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("irakasleak/ethazi/ebaluazioa");
    }

    @Test
    void denboralizazioaModuluFiltroEtaHilabetearekinKargatzenDu() {
        Maila maila = new Maila(); maila.setId(2L);
        Moduloa modulua = new Moduloa(); modulua.setMaila(maila); modulua.setAktibo(true);
        Egutegia egutegia = new Egutegia();
        var ikuspegia = new EthaziDenboralizazioaService.DenboralizazioaIkuspegia(
                "2026-2027", "azaroa 2026", 2026, 11, 20L, List.of(), List.of());
        when(moduluak.findByTaldeaId(4L)).thenReturn(List.of(modulua));
        when(egutegiak.findByIkasturtea_AktiboaTrueAndMaila_Id(2L)).thenReturn(Optional.of(egutegia));
        when(ethaziDenboralizazioaService.prestatu(
                koadernoa, egutegia, irakaslea, 20L, 2026, 11)).thenReturn(ikuspegia);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.denboralizazioa(auth, koadernoa, 20L, 2026, 11, "egutegia", null, null,
                new MockHttpSession(), model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("irakasleak/ethazi/denboralizazioa");
        assertThat(model.get("denboralizazioa")).isSameAs(ikuspegia);
    }

    @Test
    void faltenBistaDenboralizazioanKargatzenDu() {
        Maila maila = new Maila(); maila.setId(2L);
        Moduloa modulua = new Moduloa(); modulua.setMaila(maila); modulua.setAktibo(true);
        Egutegia egutegia = new Egutegia();
        var ikuspegia = new EthaziJarraipenaService.JarraipenaIkuspegia(
                "2026-2027", List.of(), 20L, "PROG", 20, List.of(), List.of());
        when(moduluak.findByTaldeaId(4L)).thenReturn(List.of(modulua));
        when(egutegiak.findByIkasturtea_AktiboaTrueAndMaila_Id(2L)).thenReturn(Optional.of(egutegia));
        when(ethaziJarraipenaService.prestatu(koadernoa, egutegia, irakaslea, false, 20L))
                .thenReturn(ikuspegia);
        when(ethaziDenboralizazioaService.prestatu(
                koadernoa, egutegia, irakaslea, 20L, 2026, 11)).thenReturn(
                        new EthaziDenboralizazioaService.DenboralizazioaIkuspegia(
                                "2026-2027", "azaroa 2026", 2026, 11, 20L, List.of(), List.of()));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.denboralizazioa(auth, koadernoa, 20L, 2026, 11, "faltak", null, null,
                new MockHttpSession(), model,
                new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("irakasleak/ethazi/denboralizazioa");
        assertThat(model.get("bista")).isEqualTo("faltak");
        assertThat(model.get("jarraipena")).isSameAs(ikuspegia);
    }

    @Test
    void asistentziatikItzultzeanNireModuluakFiltroaMantentzenDu() {
        LocalDate data = LocalDate.of(2026, 11, 4);
        Maila maila = new Maila(); maila.setId(2L);
        Moduloa modulua = new Moduloa(); modulua.setId(20L); modulua.setMaila(maila); modulua.setAktibo(true);
        Egutegia egutegia = new Egutegia();
        Koadernoa asistentziaKoadernoa = new Koadernoa(); asistentziaKoadernoa.setId(30L);
        when(moduluak.findByTaldeaId(4L)).thenReturn(List.of(modulua));
        when(egutegiak.findByIkasturtea_AktiboaTrueAndMaila_Id(2L)).thenReturn(Optional.of(egutegia));
        when(ethaziDenboralizazioaService.lortuAsistentziaKoadernoa(
                koadernoa, irakaslea, 20L)).thenReturn(asistentziaKoadernoa);
        MockHttpSession session = new MockHttpSession();

        String redirect = controller.denboralizazioAsistentzia(auth, koadernoa, 20L, data,
                2026, 11, null, "astea", LocalDate.of(2026, 11, 2), session,
                new RedirectAttributesModelMap());

        assertThat(redirect).isEqualTo("redirect:/irakasle/asistentzia?data=2026-11-04"
                + "&itzuliEthazi=true&itzuliEthaziUrtea=2026&itzuliEthaziHilabetea=11"
                + "&itzuliEthaziEgutegiMota=astea&itzuliEthaziAsteHasiera=2026-11-02");
        assertThat(session.getAttribute("koadernoAktiboa")).isSameAs(asistentziaKoadernoa);
    }

}
