package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.repository.EgutegiaRepository;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.IrakasleOrdutegia;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.IrakasleOrdutegiLerroa;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.TaldeOrdutegia;
import com.koadernoa.app.objektuak.ordutegiak.repository.IrakasleOrdutegiaRepository;
import com.koadernoa.app.objektuak.ordutegiak.repository.TaldeOrdutegiaRepository;
import com.koadernoa.app.objektuak.ordutegiak.service.TaldeOrdutegiService;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;

@ExtendWith(MockitoExtension.class)
class TaldeOrdutegiServiceTest {
    @Mock TaldeOrdutegiaRepository taldeOrdutegiak;
    @Mock IrakasleOrdutegiaRepository irakasleOrdutegiak;
    @Mock ModuloaRepository moduluak;
    @Mock TaldeaRepository taldeak;
    @Mock IkasturteaRepository ikasturteak;
    @Mock EgutegiaRepository egutegiak;

    @Test
    void irakasleenGainjarritakoSaioakGelaxkaBakarGisaGordetzenDitu() {
        TaldeOrdutegiService service = new TaldeOrdutegiService(taldeOrdutegiak, irakasleOrdutegiak,
                moduluak, taldeak, ikasturteak, egutegiak);
        Ikasturtea ikasturtea = new Ikasturtea(); ikasturtea.setId(4L); ikasturtea.setAktiboa(true);
        Taldea taldea = new Taldea(); taldea.setId(13L); taldea.setIzena("2SM2D");
        Irakaslea ane = irakaslea(1L, "Ane");
        Irakaslea bea = irakaslea(2L, "Bea");
        Moduloa modulua = new Moduloa(); modulua.setId(33L); modulua.setKodea("BUAA");
        modulua.setTaldea(taldea); modulua.setIrakasleak(new ArrayList<>(List.of(ane, bea)));
        TaldeOrdutegia helburua = new TaldeOrdutegia(); helburua.setId(70L);
        helburua.setTaldea(taldea); helburua.setIkasturtea(ikasturtea);
        helburua.setHasieraData(LocalDate.of(2026, 9, 1));

        when(taldeOrdutegiak.findWithLerroakById(70L)).thenReturn(Optional.of(helburua));
        when(moduluak.findWithIrakasleakByTaldeaId(13L)).thenReturn(List.of(modulua));
        when(irakasleOrdutegiak.findAllByIkasturteaIdAndIrakasleaIds(org.mockito.ArgumentMatchers.eq(4L), anyList()))
                .thenReturn(List.of(ordutegia(ane, taldea), ordutegia(bea, taldea)));

        var emaitza = service.eguneratuIrakasleetatik(13L, 70L, null);

        assertThat(emaitza.gelaxkaKopurua()).isEqualTo(2);
        assertThat(emaitza.gatazkaKopurua()).isZero();
        assertThat(helburua.getLerroak()).hasSize(2)
                .allSatisfy(l -> assertThat(l.getModuloa()).isSameAs(modulua));

        when(ikasturteak.findFirstByAktiboaTrueOrderByIdDesc()).thenReturn(Optional.of(ikasturtea));
        when(taldeOrdutegiak.findByTaldea_IdAndIkasturtea_IdOrderByHasieraDataAsc(13L, 4L))
                .thenReturn(List.of(helburua));
        var ikuspegia = service.ikuspegia(13L, 70L);
        assertThat(ikuspegia.moduluEtiketak().get(33L)).isEqualTo("BUAA\nAne, Bea");
    }

    private Irakaslea irakaslea(Long id, String izena) {
        Irakaslea i = new Irakaslea(); i.setId(id); i.setIzena(izena); return i;
    }

    private IrakasleOrdutegia ordutegia(Irakaslea irakaslea, Taldea taldea) {
        IrakasleOrdutegia o = new IrakasleOrdutegia(); o.setIrakaslea(irakaslea);
        IrakasleOrdutegiLerroa l = new IrakasleOrdutegiLerroa(); l.setTaldea(taldea);
        l.setTaldeKodea(taldea.getIzena()); l.setModuluKodea("BUAA");
        l.setAsteguna(Astegunak.ASTELEHENA); l.setOrduZenbakia(1); l.setSaioKopurua(2);
        o.gehituLerroa(l);
        return o;
    }
}
