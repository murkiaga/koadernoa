package com.koadernoa.app.ethazi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.koadernoa.app.ethazi.entitateak.Erronka;
import com.koadernoa.app.ethazi.entitateak.EthaziKoadernoa;
import com.koadernoa.app.ethazi.entitateak.kronograma.ErronkaKronogramaJarduera;
import com.koadernoa.app.ethazi.entitateak.kronograma.ErronkaJardueraMultzoa;
import com.koadernoa.app.ethazi.entitateak.kronograma.KronogramaJardueraMota;
import com.koadernoa.app.ethazi.repository.ErronkaRepository;
import com.koadernoa.app.ethazi.repository.ErronkaJardueraMultzoaRepository;
import com.koadernoa.app.ethazi.repository.ErronkaKronogramaJardueraRepository;
import com.koadernoa.app.ethazi.repository.EthaziKoadernoaRepository;
import com.koadernoa.app.ethazi.service.EthaziKoadernoaService;
import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.TaldeOrdutegiLerroa;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.TaldeOrdutegia;
import com.koadernoa.app.objektuak.ordutegiak.repository.TaldeOrdutegiaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;

@ExtendWith(MockitoExtension.class)
class EthaziKoadernoaServiceTest {
    @Mock EthaziKoadernoaRepository ethaziKoadernoak;
    @Mock ModuloaRepository moduluak;
    @Mock IkasturteaRepository ikasturteak;
    @Mock TaldeOrdutegiaRepository taldeOrdutegiak;
    @Mock ErronkaRepository erronkak;
    @Mock ErronkaKronogramaJardueraRepository kronogramaJarduerak;
    @Mock ErronkaJardueraMultzoaRepository jardueraMultzoak;

    @Test
    void irakaslearenTaldeBakoitzekoKoadernoaSortuEtaZerrendatzenDu() {
        EthaziKoadernoaService service = service();
        Irakaslea irakaslea = new Irakaslea(); irakaslea.setId(8L);
        Taldea taldea = taldea();
        EthaziKoadernoa sortua = koadernoa(taldea);

        when(moduluak.findDistinctTaldeakByIrakasleaId(8L)).thenReturn(List.of(taldea));
        when(ethaziKoadernoak.findByTaldeaId(4L)).thenReturn(Optional.empty());
        when(ethaziKoadernoak.findByTaldeaIdInOrderByTaldeaIzenaAsc(List.of(4L))).thenReturn(List.of(sortua));

        assertThat(service.lortuIrakaslearenKoadernoak(irakaslea)).containsExactly(sortua);
        verify(ethaziKoadernoak).saveAndFlush(org.mockito.ArgumentMatchers.argThat(k -> k.getTaldea() == taldea));
        assertThat(sortua.getIzena()).isEqualTo("Ethazi koadernoa 1MS2D");
    }

    @Test
    void erronkakoModuluBakoitzaKoloreDesberdinarekinErakustenDuOrdutegian() {
        EthaziKoadernoaService service = service();
        Taldea taldea = taldea();
        EthaziKoadernoa koadernoa = koadernoa(taldea);
        Ikasturtea ikasturtea = new Ikasturtea(); ikasturtea.setId(3L); ikasturtea.setIzena("2026-2027");
        Moduloa sareak = modulua(10L, "SARE", taldea);
        Moduloa sistema = modulua(11L, "SIST", taldea);
        TaldeOrdutegia ordutegia = new TaldeOrdutegia(); ordutegia.setId(20L); ordutegia.setTaldea(taldea);
        ordutegia.setIkasturtea(ikasturtea); ordutegia.setHasieraData(LocalDate.now().minusDays(10));
        ordutegia.setLerroak(new ArrayList<>(List.of(
                lerroa(ordutegia, sareak, Astegunak.ASTELEHENA, 1),
                lerroa(ordutegia, sistema, Astegunak.ASTEARTEA, 2))));
        Erronka erronka = new Erronka();
        erronka.setModuluak(new LinkedHashSet<>(List.of(sareak, sistema)));

        when(ikasturteak.findFirstByAktiboaTrueOrderByIdDesc()).thenReturn(Optional.of(ikasturtea));
        when(taldeOrdutegiak.findByTaldea_IdAndIkasturtea_IdOrderByHasieraDataAsc(4L, 3L))
                .thenReturn(List.of(ordutegia));
        when(moduluak.findByTaldeaId(4L)).thenReturn(List.of(sareak, sistema));
        when(erronkak.findWithModuluakByZikloaIdAndIkasturteaId(7L, 3L)).thenReturn(List.of(erronka));

        var ikuspegia = service.hasieraIkuspegia(koadernoa);

        var lehenengoa = ikuspegia.gelaxkak().get("ASTELEHENA:1");
        var bigarrena = ikuspegia.gelaxkak().get("ASTEARTEA:2");
        assertThat(lehenengoa.erronkan()).isTrue();
        assertThat(bigarrena.erronkan()).isTrue();
        assertThat(lehenengoa.kolorea()).isNotEqualTo(bigarrena.kolorea());
        assertThat(ikuspegia.legenda()).extracting(EthaziKoadernoaService.ModuluLegenda::modulua)
                .containsExactly("SARE", "SIST");
    }

    @Test
    void sarbideaTaldekoModuluEsleipenetikEgiaztatzenDu() {
        EthaziKoadernoaService service = service();
        Irakaslea irakaslea = new Irakaslea(); irakaslea.setId(8L);
        when(moduluak.existsByTaldeaIdAndIrakasleakIdAndAktiboTrue(4L, 8L)).thenReturn(true);

        assertThat(service.irakasleakBadaukaSarbidea(irakaslea, koadernoa(taldea()))).isTrue();
    }

    @Test
    void programazioanErronkakDatazOrdenatuEtaEbaluazioTarteakEzkerreanPrestatzenDitu() {
        EthaziKoadernoaService service = service();
        Taldea taldea = taldea();
        EthaziKoadernoa koadernoa = koadernoa(taldea);
        Ikasturtea ikasturtea = new Ikasturtea(); ikasturtea.setId(3L); ikasturtea.setIzena("2026-2027");
        Maila maila = new Maila(); maila.setId(2L);
        Moduloa sareak = modulua(10L, "SARE", taldea); sareak.setMaila(maila); sareak.setAktibo(true);
        Moduloa sistemak = modulua(11L, "SIST", taldea); sistemak.setMaila(maila); sistemak.setAktibo(true);
        Erronka lehenengoa = erronka(31L, 101L, "Hasierako erronka", LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 11, 20), ikasturtea, maila, sareak);
        Erronka bigarrena = erronka(32L, 102L, "Bigarren erronka", LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 3, 20), ikasturtea, maila, sareak);
        Erronka hirugarrena = erronka(33L, 103L, "Azken erronka", LocalDate.of(2027, 4, 10),
                LocalDate.of(2027, 6, 10), ikasturtea, maila, sareak);
        Egutegia egutegia = new Egutegia(); egutegia.setHasieraData(LocalDate.of(2026, 9, 1));
        egutegia.setLehenEbalBukaera(LocalDate.of(2026, 12, 20));
        egutegia.setBigarrenEbalBukaera(LocalDate.of(2027, 3, 31));
        egutegia.setBukaeraData(LocalDate.of(2027, 6, 30));
        egutegia.setEgunBereziak(List.of());
        TaldeOrdutegia ordutegia = new TaldeOrdutegia(); ordutegia.setTaldea(taldea);
        ordutegia.setIkasturtea(ikasturtea); ordutegia.setHasieraData(LocalDate.of(2026, 9, 1));
        ordutegia.setLerroak(new ArrayList<>(List.of(
                lerroa(ordutegia, sareak, Astegunak.ASTELEHENA, 1))));

        when(ikasturteak.findFirstByAktiboaTrueOrderByIdDesc()).thenReturn(Optional.of(ikasturtea));
        when(moduluak.findByTaldeaId(4L)).thenReturn(List.of(sareak));
        when(erronkak.findWithModuluakByZikloaIdAndIkasturteaId(7L, 3L))
                .thenReturn(List.of(hirugarrena, bigarrena, lehenengoa));

        Irakaslea irakaslea = new Irakaslea(); irakaslea.setId(8L);
        ErronkaKronogramaJarduera modulukoJarduera = jarduera(
                51L, lehenengoa, KronogramaJardueraMota.MODULUA, sareak, null, "Sareak prestatu");
        ErronkaKronogramaJarduera jardueraOrokorra = jarduera(
                52L, lehenengoa, KronogramaJardueraMota.KOMUNA, null, sareak, "Aurkezpena");
        ErronkaJardueraMultzoa prestaketa = new ErronkaJardueraMultzoa();
        prestaketa.setId(70L); prestaketa.setErronka(lehenengoa); prestaketa.setIzena("Prestaketa");
        prestaketa.setOrdena(1);
        modulukoJarduera.setMultzoa(prestaketa);
        jardueraOrokorra.setMultzoa(prestaketa);
        when(kronogramaJarduerak.findByErronkaIdInOrderByHasieraDataAscBukaeraDataAscIdAsc(
                List.of(31L, 32L, 33L))).thenReturn(List.of(modulukoJarduera, jardueraOrokorra));
        when(jardueraMultzoak.findByErronkaIdInOrderByOrdenaAscIdAsc(List.of(31L, 32L, 33L)))
                .thenReturn(List.of(prestaketa));
        when(moduluak.findAktiboakByTaldeaIdAndIrakasleaId(4L, 8L)).thenReturn(List.of(sareak, sistemak));
        when(taldeOrdutegiak.findByTaldea_IdAndIkasturtea_IdOrderByHasieraDataAsc(4L, 3L))
                .thenReturn(List.of(ordutegia));

        var ikuspegia = service.programazioaIkuspegia(koadernoa, egutegia, irakaslea);

        assertThat(ikuspegia.erronkak()).extracting(EthaziKoadernoaService.ErronkaTxartela::izena)
                .containsExactly("Hasierako erronka", "Bigarren erronka", "Azken erronka");
        assertThat(ikuspegia.ebaluazioTarteak()).extracting(EthaziKoadernoaService.EbaluazioTartea::izena)
                .containsExactly("1. ebal", "2. ebal", "3. ebal");
        assertThat(ikuspegia.ebaluazioTarteak()).extracting(EthaziKoadernoaService.EbaluazioTartea::hasieraErronka)
                .containsExactly(1, 2, 3);
        assertThat(ikuspegia.moduluAukerak()).extracting(EthaziKoadernoaService.ModuluAukera::etiketa)
                .containsExactly("SARE");
        assertThat(ikuspegia.erronkak().get(0).jarduerak())
                .extracting(EthaziKoadernoaService.KronogramaJardueraTxartela::badgea)
                .containsExactly("SARE", "Orokorra");
        assertThat(ikuspegia.erronkak().get(0).jarduerak().get(1).aurreikusitakoModuloa())
                .isEqualTo("SARE");
        assertThat(ikuspegia.erronkak().get(0).moduluOrduak()).singleElement()
                .satisfies(orduak -> {
                    assertThat(orduak.orduErabilgarriak()).isEqualTo(10);
                    assertThat(orduak.orduProgramatuak()).isEqualByComparingTo("4.00");
                });
        assertThat(ikuspegia.erronkak().get(0).multzoak()).singleElement()
                .satisfies(m -> {
                    assertThat(m.izena()).isEqualTo("Prestaketa");
                    assertThat(m.jarduerak()).hasSize(2);
                    assertThat(m.hasieraData()).isEqualTo(LocalDate.of(2026, 9, 15));
                    assertThat(m.bukaeraData()).isEqualTo(LocalDate.of(2026, 9, 15));
                });
    }

    private EthaziKoadernoaService service() {
        return new EthaziKoadernoaService(ethaziKoadernoak, moduluak, ikasturteak, taldeOrdutegiak,
                erronkak, kronogramaJarduerak, jardueraMultzoak);
    }

    private Taldea taldea() {
        Zikloa zikloa = new Zikloa(); zikloa.setId(7L);
        Taldea taldea = new Taldea(); taldea.setId(4L); taldea.setIzena("1MS2D"); taldea.setZikloa(zikloa);
        return taldea;
    }

    private EthaziKoadernoa koadernoa(Taldea taldea) {
        EthaziKoadernoa k = new EthaziKoadernoa(); k.setId(6L); k.setTaldea(taldea); return k;
    }

    private Moduloa modulua(Long id, String kodea, Taldea taldea) {
        Moduloa m = new Moduloa(); m.setId(id); m.setKodea(kodea); m.setTaldea(taldea); return m;
    }

    private Erronka erronka(Long id, Long bertsioa, String izena, LocalDate hasiera, LocalDate bukaera,
            Ikasturtea ikasturtea, Maila maila, Moduloa modulua) {
        Erronka e = new Erronka(); e.setId(id); e.setBertsioTaldea(bertsioa); e.setIzena(izena);
        e.setDeskribapena(izena + " deskribapena"); e.setHasieraData(hasiera); e.setBukaeraData(bukaera);
        e.setIkasturtea(ikasturtea); e.setMaila(maila); e.setHizkuntza(Hizkuntza.EUSKARA);
        e.setModuluak(new LinkedHashSet<>(List.of(modulua))); return e;
    }

    private TaldeOrdutegiLerroa lerroa(TaldeOrdutegia ordutegia, Moduloa modulua,
            Astegunak eguna, int ordua) {
        TaldeOrdutegiLerroa l = new TaldeOrdutegiLerroa(); l.setTaldeOrdutegia(ordutegia);
        l.setModuloa(modulua); l.setAsteguna(eguna); l.setOrduZenbakia(ordua); return l;
    }

    private ErronkaKronogramaJarduera jarduera(Long id, Erronka erronka, KronogramaJardueraMota mota,
            Moduloa moduloa, Moduloa aurreikusitakoa, String izenburua) {
        ErronkaKronogramaJarduera j = new ErronkaKronogramaJarduera();
        j.setId(id); j.setErronka(erronka); j.setMota(mota); j.setModuloa(moduloa);
        j.setAurreikusitakoModuloa(aurreikusitakoa); j.setIzenburua(izenburua);
        j.setHasieraData(LocalDate.of(2026, 9, 15)); j.setBukaeraData(LocalDate.of(2026, 9, 15));
        j.setIraupenOrduak(new BigDecimal("2.00"));
        return j;
    }
}
