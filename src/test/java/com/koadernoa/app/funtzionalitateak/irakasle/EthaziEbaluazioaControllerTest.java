package com.koadernoa.app.funtzionalitateak.irakasle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;

import com.koadernoa.app.ethazi.entitateak.Erronka;
import com.koadernoa.app.ethazi.entitateak.EthaziKoadernoa;
import com.koadernoa.app.ethazi.entitateak.errubrikak.ErronkaErrubrika;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.GaitasunMota;
import com.koadernoa.app.ethazi.service.EthaziKoadernoaService;
import com.koadernoa.app.ethazi.service.KinielaService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

@ExtendWith(MockitoExtension.class)
class EthaziEbaluazioaControllerTest {

    @Mock KinielaService kinielaService;
    @Mock EthaziKoadernoaService koadernoaService;
    @Mock IrakasleaService irakasleaService;
    @Mock Authentication auth;

    private EthaziEbaluazioaController controller;
    private EthaziKoadernoa koadernoa;
    private Irakaslea irakaslea;

    @BeforeEach
    void prestatu() {
        controller = new EthaziEbaluazioaController(kinielaService, koadernoaService, irakasleaService);
        Taldea taldea = new Taldea(); taldea.setId(4L);
        koadernoa = new EthaziKoadernoa(); koadernoa.setId(6L); koadernoa.setTaldea(taldea);
        irakaslea = new Irakaslea(); irakaslea.setId(8L);
        when(koadernoaService.findById(6L)).thenReturn(koadernoa);
        when(irakasleaService.getLogeatutaDagoenIrakaslea(auth)).thenReturn(irakaslea);
        when(koadernoaService.irakasleakBadaukaSarbidea(irakaslea, koadernoa)).thenReturn(true);
    }

    @Test
    void irakaslearenModuluakBakarrikErakustenDituEtaTxantiloiBereziaErabiltzenDu() {
        Moduloa berea = new Moduloa(); berea.setId(33L); berea.setKodea("BUAA");
        Moduloa bestea = new Moduloa(); bestea.setId(44L); bestea.setKodea("EIE");
        Erronka erronka = new Erronka(); erronka.setId(4L);
        erronka.setModuluak(new LinkedHashSet<>(List.of(berea, bestea)));
        var modulua = new EthaziKoadernoaService.ModuluAukera(33L, "BUAA");
        var erronkaAukera = new EthaziKoadernoaService.ErronkaTxartela(4L, 1, "Erronka", null,
                null, null, List.of("BUAA"), List.of(), List.of(modulua), List.of(), List.of(), List.of());
        when(koadernoaService.programazioaIkuspegia(koadernoa, null, irakaslea))
                .thenReturn(new EthaziKoadernoaService.ProgramazioaIkuspegia(
                        "2026-2027", List.of(erronkaAukera), List.of(), List.of(modulua)));
        when(kinielaService.erronka(4L)).thenReturn(erronka);
        when(kinielaService.erronkaKalifikatuDezake(auth, 4L, 33L)).thenReturn(true);
        when(kinielaService.errubrikaIkasleTaldeak(4L)).thenReturn(List.of());
        when(kinielaService.errubrika(4L, 33L)).thenReturn(new ErronkaErrubrika());
        when(kinielaService.errubrikaIturriak(4L, 33L)).thenReturn(List.of());
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.errubrikak(4L, 33L, GaitasunMota.TEKNIKOA,
                null, null, null, koadernoa, auth, new MockHttpSession(), model);

        assertThat(view).isEqualTo("irakasleak/ethazi/errubrikak");
        assertThat(model.get("irakasleEbaluazioa")).isEqualTo(true);
        assertThat(model.get("errubrikaEditatuDezake")).isEqualTo(true);
        assertThat((List<?>) model.get("moduluak")).extracting("id").containsExactly(33L);
    }

    @Test
    void zeharkakoErrubrikaEzDaEditagarriaIrakasleBistan() {
        Moduloa berea = new Moduloa(); berea.setId(33L); berea.setKodea("BUAA");
        Erronka erronka = new Erronka(); erronka.setId(4L);
        erronka.setModuluak(new LinkedHashSet<>(List.of(berea)));
        var modulua = new EthaziKoadernoaService.ModuluAukera(33L, "BUAA");
        var erronkaAukera = new EthaziKoadernoaService.ErronkaTxartela(4L, 1, "Erronka", null,
                null, null, List.of("BUAA"), List.of(), List.of(modulua), List.of(), List.of(), List.of());
        when(koadernoaService.programazioaIkuspegia(koadernoa, null, irakaslea))
                .thenReturn(new EthaziKoadernoaService.ProgramazioaIkuspegia(
                        "2026-2027", List.of(erronkaAukera), List.of(), List.of(modulua)));
        when(kinielaService.erronka(4L)).thenReturn(erronka);
        when(kinielaService.erronkaKalifikatuDezake(auth, 4L, 33L)).thenReturn(true);
        when(kinielaService.errubrikaIkasleTaldeak(4L)).thenReturn(List.of());
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.errubrikak(4L, 33L, GaitasunMota.ZEHARKAKOA,
                null, null, null, koadernoa, auth, new MockHttpSession(), model);

        assertThat(view).isEqualTo("irakasleak/ethazi/errubrikak");
        assertThat(model.get("errubrikaEditatuDezake")).isEqualTo(false);
    }
}
