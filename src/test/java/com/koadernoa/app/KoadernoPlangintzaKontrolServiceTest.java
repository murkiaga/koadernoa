package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunMota;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.service.IkasturteaService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoOrdutegiBlokea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.repository.JardueraRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoOrdutegiBlokeaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoKlaseEgunService;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoPlangintzaKontrolService;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoPlangintzaKontrola;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoPlangintzaKontrola.Egoera;

class KoadernoPlangintzaKontrolServiceTest {

    private final IkasturteaService ikasturteaService = mock(IkasturteaService.class);
    private final KoadernoaRepository koadernoaRepository = mock(KoadernoaRepository.class);
    private final JardueraRepository jardueraRepository = mock(JardueraRepository.class);
    private final KoadernoOrdutegiBlokeaRepository blokeRepository = mock(KoadernoOrdutegiBlokeaRepository.class);
    private final KoadernoKlaseEgunService klaseEgunService = new KoadernoKlaseEgunService();
    private final AplikazioAukeraService aukeraService = mock(AplikazioAukeraService.class);
    private final KoadernoPlangintzaKontrolService service = new KoadernoPlangintzaKontrolService(
            ikasturteaService, koadernoaRepository, jardueraRepository, blokeRepository, klaseEgunService,
            aukeraService);

    @Test
    void jardueraBatEgunekoNahikoaDaEtaEgoerakZuzenKalkulatzenDitu() {
        LocalDate gaur = LocalDate.of(2026, 10, 2);
        Ikasturtea ikasturtea = new Ikasturtea();
        ikasturtea.setId(9L);
        Egutegia egutegia = egutegia();
        Koadernoa ondo = koadernoa(1L, egutegia);
        Koadernoa osatuGabe = koadernoa(2L, egutegia);
        Koadernoa klaseEgunikEz = koadernoa(3L, egutegia);
        KoadernoOrdutegiBlokea ondoBlokea = blokea(ondo, Astegunak.ASTELEHENA, 3);
        KoadernoOrdutegiBlokea osatuGabeBlokea = blokea(osatuGabe, Astegunak.ASTEARTEA, 1);
        List<LocalDate> lanEgunak = klaseEgunService.hurrengoLanEgunak(egutegia, gaur, 10);
        List<LocalDate> ondoEgunak = List.copyOf(klaseEgunService.egunekoSlotak(
                egutegia, List.of(ondoBlokea), lanEgunak.get(0), lanEgunak.get(9)).keySet());
        List<LocalDate> osatuGabeEgunak = List.copyOf(klaseEgunService.egunekoSlotak(
                egutegia, List.of(osatuGabeBlokea), lanEgunak.get(0), lanEgunak.get(9)).keySet());
        LocalDate azkenData = ondoEgunak.get(ondoEgunak.size() - 1)
                .isAfter(osatuGabeEgunak.get(osatuGabeEgunak.size() - 1))
                ? ondoEgunak.get(ondoEgunak.size() - 1) : osatuGabeEgunak.get(osatuGabeEgunak.size() - 1);

        when(ikasturteaService.getAktiboa()).thenReturn(Optional.of(ikasturtea));
        when(koadernoaRepository.findByIkasturteaIdWithPlangintzaKontrolRelations(9L))
                .thenReturn(List.of(ondo, osatuGabe, klaseEgunikEz));
        when(blokeRepository.findByIkasturteaId(9L)).thenReturn(List.of(ondoBlokea, osatuGabeBlokea));
        when(aukeraService.getPlangintzaKontrolLanegunak()).thenReturn(10);
        List<Object[]> jardueraDatak = new ArrayList<>();
        ondoEgunak.forEach(data -> jardueraDatak.add(new Object[] {1L, data}));
        osatuGabeEgunak.subList(0, osatuGabeEgunak.size() - 1)
                .forEach(data -> jardueraDatak.add(new Object[] {2L, data}));
        when(jardueraRepository.findJardueraDatak(List.of(1L, 2L, 3L), gaur, azkenData))
                .thenReturn(jardueraDatak);

        List<KoadernoPlangintzaKontrola> kontrolak = service.lortuKontrola(gaur);

        assertThat(kontrolak).extracting(KoadernoPlangintzaKontrola::egoera)
                .containsExactly(Egoera.ONDO, Egoera.OSATU_GABE, Egoera.KLASE_EGUNIK_EZ);
        assertThat(kontrolak.get(0).planifikatutakoEgunak()).isEqualTo(ondoEgunak.size());
        assertThat(kontrolak.get(1).faltaDirenEgunak())
                .containsExactly(osatuGabeEgunak.get(osatuGabeEgunak.size() - 1));
        assertThat(kontrolak.get(2).kontrolatuBeharrekoEgunak()).isZero();
        verify(jardueraRepository).findJardueraDatak(List.of(1L, 2L, 3L), gaur, azkenData);
    }

    @Test
    void hurrengoHamarLanegunetakoKlaseakBakarrikKontrolatzenDituJaiegunakKontuanHartuta() {
        LocalDate gaur = LocalDate.of(2026, 10, 6);
        Ikasturtea ikasturtea = new Ikasturtea();
        ikasturtea.setId(9L);
        Egutegia egutegia = egutegia();
        egutegia.setEgunBereziak(new ArrayList<>(List.of(
                egunBerezia(LocalDate.of(2026, 10, 12), EgunMota.JAIEGUNA),
                egunBerezia(LocalDate.of(2026, 10, 13), EgunMota.JAIEGUNA))));
        Koadernoa koadernoa = koadernoa(1L, egutegia);
        KoadernoOrdutegiBlokea blokea = blokea(koadernoa, Astegunak.ASTEARTEA, 1);

        when(ikasturteaService.getAktiboa()).thenReturn(Optional.of(ikasturtea));
        when(koadernoaRepository.findByIkasturteaIdWithPlangintzaKontrolRelations(9L))
                .thenReturn(List.of(koadernoa));
        when(blokeRepository.findByIkasturteaId(9L)).thenReturn(List.of(blokea));
        when(aukeraService.getPlangintzaKontrolLanegunak()).thenReturn(10);
        when(jardueraRepository.findJardueraDatak(
                List.of(1L), gaur, LocalDate.of(2026, 10, 20)))
                .thenReturn(List.<Object[]>of(new Object[] {1L, LocalDate.of(2026, 10, 20)}));

        KoadernoPlangintzaKontrola kontrola = service.lortuKontrola(gaur).get(0);

        assertThat(klaseEgunService.hurrengoLanEgunak(egutegia, gaur, 10))
                .containsExactly(
                        LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 9),
                        LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 16),
                        LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 21),
                        LocalDate.of(2026, 10, 22));
        assertThat(kontrola.egoera()).isEqualTo(Egoera.ONDO);
        assertThat(kontrola.egunak()).extracting(KoadernoPlangintzaKontrola.PlangintzaEgunKontrola::data)
                .containsExactly(LocalDate.of(2026, 10, 20));
    }

    @Test
    void ikasturteAktiborikEzDagoeneanEzDuKontsultarikEgiten() {
        when(ikasturteaService.getAktiboa()).thenReturn(Optional.empty());

        assertThat(service.lortuKontrola(LocalDate.of(2026, 10, 2))).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(koadernoaRepository, jardueraRepository, blokeRepository);
    }

    @Test
    void osatuGabekoKoadernoetakoEmailakBakarrikEtaDistinctItzultzenDitu() {
        Irakaslea ane = irakaslea(1L, " Ane@Example.com ");
        Irakaslea aneBikoiztua = irakaslea(2L, "ane@example.com");
        Irakaslea mikel = irakaslea(3L, "mikel@example.com");
        Koadernoa osatuGabea = koadernoa(1L, egutegia());
        osatuGabea.setIrakasleak(List.of(ane, mikel));
        Koadernoa besteOsatuGabea = koadernoa(2L, egutegia());
        besteOsatuGabea.setIrakasleak(List.of(aneBikoiztua));
        Koadernoa klaseEgunikEz = koadernoa(3L, egutegia());
        klaseEgunikEz.setIrakasleak(List.of(irakaslea(4L, "ez@example.com")));

        var emailak = service.abisuHartzaileEmailak(List.of(
                kontrola(osatuGabea, Egoera.OSATU_GABE),
                kontrola(besteOsatuGabea, Egoera.OSATU_GABE),
                kontrola(klaseEgunikEz, Egoera.KLASE_EGUNIK_EZ)));

        assertThat(emailak).containsExactly("Ane@Example.com", "mikel@example.com");
    }

    private KoadernoPlangintzaKontrola kontrola(Koadernoa koadernoa, Egoera egoera) {
        LocalDate data = LocalDate.of(2026, 10, 5);
        return new KoadernoPlangintzaKontrola(koadernoa,
                egoera == Egoera.KLASE_EGUNIK_EZ ? List.of()
                        : List.of(new KoadernoPlangintzaKontrola.PlangintzaEgunKontrola(data, false)),
                0, egoera == Egoera.KLASE_EGUNIK_EZ ? 0 : 1, egoera);
    }

    private Egutegia egutegia() {
        Egutegia egutegia = new Egutegia();
        egutegia.setHasieraData(LocalDate.of(2026, 9, 1));
        egutegia.setBukaeraData(LocalDate.of(2027, 6, 30));
        egutegia.setEgunBereziak(new ArrayList<>());
        return egutegia;
    }

    private Koadernoa koadernoa(Long id, Egutegia egutegia) {
        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setId(id);
        koadernoa.setEgutegia(egutegia);
        return koadernoa;
    }

    private KoadernoOrdutegiBlokea blokea(Koadernoa koadernoa, Astegunak asteguna, int iraupena) {
        KoadernoOrdutegiBlokea blokea = new KoadernoOrdutegiBlokea();
        blokea.setKoadernoa(koadernoa);
        blokea.setHasieraData(koadernoa.getEgutegia().getHasieraData());
        blokea.setAsteguna(asteguna);
        blokea.setHasieraSlot(1);
        blokea.setIraupenaSlot(iraupena);
        return blokea;
    }

    private Irakaslea irakaslea(Long id, String emaila) {
        Irakaslea irakaslea = new Irakaslea();
        irakaslea.setId(id);
        irakaslea.setEmaila(emaila);
        return irakaslea;
    }

    private EgunBerezi egunBerezia(LocalDate data, EgunMota mota) {
        EgunBerezi eguna = new EgunBerezi();
        eguna.setData(data);
        eguna.setMota(mota);
        return eguna;
    }
}
