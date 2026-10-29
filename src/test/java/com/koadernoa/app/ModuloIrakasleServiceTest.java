package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;
import com.koadernoa.app.objektuak.modulua.service.ModuloIrakasleService;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.IrakasleOrdutegia;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.IrakasleOrdutegiLerroa;
import com.koadernoa.app.objektuak.ordutegiak.repository.IrakasleOrdutegiaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

@ExtendWith(MockitoExtension.class)
class ModuloIrakasleServiceTest {
    @Mock ModuloaRepository moduluak;
    @Mock IrakasleaRepository irakasleak;
    @Mock IrakasleOrdutegiaRepository ordutegiak;
    ModuloIrakasleService service;
    Taldea taldea;
    Moduloa sareak;
    Irakaslea ane;

    @BeforeEach
    void setup() {
        service = new ModuloIrakasleService(moduluak, irakasleak, ordutegiak);
        taldea = new Taldea(); taldea.setId(9L); taldea.setIzena("2MS2D");
        sareak = new Moduloa(); sareak.setId(33L); sareak.setKodea("SRED");
        sareak.setIzena("Sareko zerbitzuak"); sareak.setTaldea(taldea);
        ane = new Irakaslea(); ane.setId(5L); ane.setIzena("Ane"); ane.setEmaila("ane@example.com");
    }

    @Test
    void ordutegietatikTaldeEtaKodeBerdinekoModuluanIrakasleaBehinGehitzenDu() {
        IrakasleOrdutegia ordutegia = ordutegia(lerroa(Astegunak.ASTELEHENA, 1), lerroa(Astegunak.ASTEARTEA, 2));
        ordutegia.getLerroak().forEach(l -> l.setTaldea(null)); // Eskuzko lerro zaharrek kodea bakarrik zuten.
        when(moduluak.findAll()).thenReturn(List.of(sareak));
        when(ordutegiak.findAllAktiboak()).thenReturn(List.of(ordutegia));

        var emaitza = service.esleituOrdutegietatik();

        assertThat(emaitza.gehituak()).isEqualTo(1);
        assertThat(emaitza.lotuGabeak()).isZero();
        assertThat(sareak.getIrakasleak()).containsExactly(ane);
        verify(moduluak).saveAll(any());
    }

    @Test
    void taldeOrdutegiakEsleitutakoIrakaslearenModuluaErakustenDu() {
        sareak.setIrakasleak(new ArrayList<>(List.of(ane)));
        IrakasleOrdutegiLerroa lerroa = lerroa(Astegunak.ASTELEHENA, 2);
        lerroa.setTaldea(null); // taldeKodea erabiliz ere parekatu behar du.
        lerroa.setSaioKopurua(2);
        lerroa.setGelaKodea("G01");
        when(moduluak.findWithIrakasleakByTaldeaId(9L)).thenReturn(List.of(sareak));
        when(ordutegiak.findAllAktiboakByIrakasleaIds(List.of(5L))).thenReturn(List.of(ordutegia(lerroa)));

        var emaitza = service.taldeOrdutegia(9L);

        assertThat(emaitza.lerroak().get(1).gelaxkak().get(0).edukiak()).singleElement()
                .satisfies(e -> {
                    assertThat(e.moduluKodea()).isEqualTo("SRED");
                    assertThat(e.irakasleIzena()).isEqualTo("Ane");
                    assertThat(e.gela()).isEqualTo("G01");
                });
        assertThat(emaitza.lerroak().get(2).gelaxkak().get(0).edukiak()).hasSize(1);
    }

    private IrakasleOrdutegia ordutegia(IrakasleOrdutegiLerroa... lerroak) {
        IrakasleOrdutegia ordutegia = new IrakasleOrdutegia();
        ordutegia.setIrakaslea(ane);
        for (IrakasleOrdutegiLerroa lerroa : lerroak) ordutegia.gehituLerroa(lerroa);
        return ordutegia;
    }

    private IrakasleOrdutegiLerroa lerroa(Astegunak eguna, int ordua) {
        IrakasleOrdutegiLerroa lerroa = new IrakasleOrdutegiLerroa();
        lerroa.setId((long) ordua);
        lerroa.setAsteguna(eguna);
        lerroa.setOrduZenbakia(ordua);
        lerroa.setSaioKopurua(1);
        lerroa.setModuluKodea("sred");
        lerroa.setTaldea(taldea);
        lerroa.setTaldeKodea("2MS2D");
        return lerroa;
    }
}
