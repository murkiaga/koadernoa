package com.koadernoa.app.objektuak.modulua.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.repository.IkasleaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;
import com.koadernoa.app.security.ActiveDirectoryBilaketaService;
import com.koadernoa.app.security.AdErabiltzailea;

class IkasleEmailAdServiceTest {
    private final ActiveDirectoryBilaketaService ad = mock(ActiveDirectoryBilaketaService.class);
    private final IkasleaRepository ikasleak = mock(IkasleaRepository.class);
    private final TaldeaRepository taldeak = mock(TaldeaRepository.class);
    private IkasleEmailAdService service;
    private Taldea taldea;

    @BeforeEach void prestatu() {
        service = new IkasleEmailAdService(ad, ikasleak, taldeak);
        taldea = new Taldea(); taldea.setId(10L); taldea.setIzena("1WG3D");
        when(taldeak.findById(10L)).thenReturn(Optional.of(taldea));
    }

    @Test void azentuMaiuskulaEtaBiAbizenakZiurParekatzenDitu() {
        Ikaslea i = ikaslea("ÁNE María", "López", "García");
        when(ikasleak.findByTaldea_IdOrderByAbizena1AscAbizena2AscIzenaAsc(10L)).thenReturn(List.of(i));
        when(ad.bilatuTaldekoIkasleak("1WG3D")).thenReturn(List.of(ad("ane MARIA", "lopez garcia", "ane@eskola.eus")));
        var lerroa = service.aurreikusi(10L).lerroak().get(0);
        assertThat(lerroa.egoera()).isEqualTo(IkasleEmailAdService.Egoera.ZIURRA);
        assertThat(lerroa.proposatutakoEmaila()).isEqualTo("ane@eskola.eus");
    }

    @Test void hautagaiBikoitzaZalantzaDaEtaEzDaLehenesten() {
        Ikaslea i = ikaslea("June", "Bilbao", "");
        when(ikasleak.findByTaldea_IdOrderByAbizena1AscAbizena2AscIzenaAsc(10L)).thenReturn(List.of(i));
        when(ad.bilatuTaldekoIkasleak("1WG3D")).thenReturn(List.of(
                ad("June", "Bilbao", "j1@eskola.eus"), ad("June", "Bilbao", "j2@eskola.eus")));
        var lerroa = service.aurreikusi(10L).lerroak().get(0);
        assertThat(lerroa.egoera()).isEqualTo(IkasleEmailAdService.Egoera.ZALANTZA);
        assertThat(lerroa.lehenetsita()).isFalse();
    }

    @Test void emailikGabekoAdKontuaEzAurkitutzatJotzenDu() {
        Ikaslea i = ikaslea("Markel", "Garcia", "");
        when(ikasleak.findByTaldea_IdOrderByAbizena1AscAbizena2AscIzenaAsc(10L)).thenReturn(List.of(i));
        when(ad.bilatuTaldekoIkasleak("1WG3D")).thenReturn(List.of(ad("Markel", "Garcia", null)));
        assertThat(service.aurreikusi(10L).lerroak().get(0).egoera())
                .isEqualTo(IkasleEmailAdService.Egoera.EZ_AURKITUA);
    }

    @Test void besteTaldekoIkasleaEzDuGordetzen() {
        Ikaslea i = ikaslea("Ane", "Lopez", "Garcia"); i.setId(7L);
        Taldea bestea = new Taldea(); bestea.setId(99L); i.setTaldea(bestea);
        when(ikasleak.findById(7L)).thenReturn(Optional.of(i));
        when(ikasleak.findByTaldea_IdOrderByAbizena1AscAbizena2AscIzenaAsc(10L)).thenReturn(List.of());
        when(ad.bilatuTaldekoIkasleak("1WG3D")).thenReturn(List.of(ad("Ane", "Lopez Garcia", "ane@eskola.eus")));
        var e = service.gorde(10L, Map.of(7L, new IkasleEmailAdService.Hautapena("ane@eskola.eus", false)));
        assertThat(e.eguneratuak()).isZero();
        assertThat(e.gatazkak()).isEqualTo(1);
    }

    private Ikaslea ikaslea(String izena, String a1, String a2) {
        Ikaslea i = new Ikaslea(); i.setIzena(izena); i.setAbizena1(a1); i.setAbizena2(a2); i.setTaldea(taldea); return i;
    }

    private AdErabiltzailea ad(String izena, String abizenak, String emaila) {
        return new AdErabiltzailea(emaila, "user", "user@domain", izena, abizenak,
                izena + " " + abizenak, emaila, "CN=user,OU=1WG3D,OU=Ikasleak");
    }
}
