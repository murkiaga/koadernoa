package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Ebaluaketa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.JardueraPlanifikatua;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoOrdutegiBlokea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Programazioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.UnitateDidaktikoa;
import com.koadernoa.app.objektuak.koadernoak.repository.JardueraRepository;
import com.koadernoa.app.objektuak.koadernoak.service.DenboralizazioGeneratorService;

class DenboralizazioGeneratorServiceTest {

    @Test
    void zeroOrdukoJardueraKalifikagarriaDenboralizazioanSortzenDu() {
        DenboralizazioGeneratorService service =
                new DenboralizazioGeneratorService(mock(JardueraRepository.class));
        LocalDate astelehena = LocalDate.of(2026, 9, 7);

        Egutegia egutegia = new Egutegia();
        egutegia.setHasieraData(astelehena);
        egutegia.setBukaeraData(astelehena);
        egutegia.setEgunBereziak(List.of());

        KoadernoOrdutegiBlokea blokea = new KoadernoOrdutegiBlokea();
        blokea.setHasieraData(astelehena);
        blokea.setAsteguna(Astegunak.ASTELEHENA);
        blokea.setIraupenaSlot(1);

        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setEgutegia(egutegia);
        koadernoa.setOrdutegiak(List.of(blokea));

        Programazioa programazioa = new Programazioa();
        Ebaluaketa ebaluaketa = new Ebaluaketa();
        ebaluaketa.setOrdena(1);
        UnitateDidaktikoa ud = new UnitateDidaktikoa();
        ud.setId(10L);
        ud.setKodea("UD1");
        ud.setIzenburua("Unitatea");
        ud.setOrduak(1);
        ud.setPosizioa(0);
        JardueraPlanifikatua jarrera = new JardueraPlanifikatua();
        jarrera.setIzenburua("JARRERA");
        jarrera.setOrduak(0);
        jarrera.setPosizioa(0);
        jarrera.setKalifikagarria(true);
        jarrera.setPisua(new BigDecimal("10"));
        ud.gehituAzpiJarduera(jarrera);
        ebaluaketa.setUnitateak(List.of(ud));
        programazioa.setEbaluaketak(List.of(ebaluaketa));

        var emaitza = service.generateFromProgramazioa(koadernoa, programazioa, true, false);

        assertThat(emaitza).hasSize(2);
        assertThat(emaitza.get(0).titulua()).isEqualTo("JARRERA");
        assertThat(emaitza.get(0).orduak()).isZero();
        assertThat(emaitza.get(0).kalifikagarria()).isTrue();
        assertThat(emaitza.get(0).pisua()).isEqualByComparingTo("10");
        assertThat(emaitza.get(1).orduak()).isEqualTo(1);
    }
}
