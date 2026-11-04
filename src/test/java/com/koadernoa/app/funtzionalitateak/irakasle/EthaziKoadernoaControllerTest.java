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
import com.koadernoa.app.ethazi.service.KronogramaService;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.egutegia.repository.EgutegiaRepository;
import com.koadernoa.app.objektuak.egutegia.service.EgutegiaService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
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
    @Mock Authentication auth;

    private EthaziKoadernoaController controller;
    private EthaziKoadernoa koadernoa;
    private Irakaslea irakaslea;

    @BeforeEach
    void prestatu() {
        controller = new EthaziKoadernoaController(koadernoaService, irakasleaService, ikasleak,
                moduluak, egutegiak, egutegiaService, kronogramaService);
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
}
