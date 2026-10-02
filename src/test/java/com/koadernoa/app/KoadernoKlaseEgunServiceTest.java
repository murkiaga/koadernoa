package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunMota;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoOrdutegiBlokea;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoKlaseEgunService;

class KoadernoKlaseEgunServiceTest {

    private final KoadernoKlaseEgunService service = new KoadernoKlaseEgunService();

    @Test
    void hurrengoHamabostKlaseEgunakLortzenDituAsteburuakZenbatuGabe() {
        Egutegia egutegia = egutegia(LocalDate.of(2026, 10, 1), LocalDate.of(2027, 6, 30));
        List<KoadernoOrdutegiBlokea> blokeak = new ArrayList<>();
        for (Astegunak asteguna : List.of(Astegunak.ASTELEHENA, Astegunak.ASTEARTEA,
                Astegunak.ASTEAZKENA, Astegunak.OSTEGUNA, Astegunak.OSTIRALA)) {
            blokeak.add(blokea(LocalDate.of(2026, 10, 1), asteguna, 3));
        }

        List<LocalDate> egunak = service.hurrengoKlaseEgunak(
                egutegia, blokeak, LocalDate.of(2026, 10, 2), 15);

        assertThat(egunak).hasSize(15).first().isEqualTo(LocalDate.of(2026, 10, 2));
        assertThat(egunak).allMatch(data -> data.getDayOfWeek() != DayOfWeek.SATURDAY
                && data.getDayOfWeek() != DayOfWeek.SUNDAY);
    }

    @Test
    void asteburuaEzDaKlaseEgunaOrdutegianBlokeaBadagoEre() {
        Egutegia egutegia = egutegia(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertThat(service.hurrengoKlaseEgunak(
                egutegia,
                List.of(blokea(LocalDate.of(2026, 10, 1), Astegunak.LARUNBATA, 1)),
                LocalDate.of(2026, 10, 2), 2))
                .isEmpty();
    }

    @Test
    void gabonetakoOporrakSaltatuEtaOrdezkatutakoAstegunaErabiltzenDu() {
        Egutegia egutegia = egutegia(LocalDate.of(2026, 12, 1), LocalDate.of(2027, 2, 1));
        for (LocalDate data = LocalDate.of(2026, 12, 21);
                !data.isAfter(LocalDate.of(2027, 1, 6)); data = data.plusDays(1)) {
            egutegia.getEgunBereziak().add(egunBerezia(data, EgunMota.JAIEGUNA, null));
        }
        egutegia.getEgunBereziak().add(egunBerezia(
                LocalDate.of(2027, 1, 7), EgunMota.ORDEZKATUA, Astegunak.ASTELEHENA));

        List<LocalDate> egunak = service.hurrengoKlaseEgunak(
                egutegia,
                List.of(blokea(LocalDate.of(2026, 12, 1), Astegunak.ASTELEHENA, 1)),
                LocalDate.of(2026, 12, 14), 3);

        assertThat(egunak).containsExactly(
                LocalDate.of(2026, 12, 14),
                LocalDate.of(2027, 1, 7),
                LocalDate.of(2027, 1, 11));
    }

    @Test
    void ikasturteAmaieranGeratzenDirenBostEgunakBakarrikEskatzenDitu() {
        Egutegia egutegia = egutegia(LocalDate.of(2026, 9, 1), LocalDate.of(2027, 6, 30));
        List<KoadernoOrdutegiBlokea> blokeak = List.of(
                blokea(LocalDate.of(2026, 9, 1), Astegunak.ASTELEHENA, 1),
                blokea(LocalDate.of(2026, 9, 1), Astegunak.ASTEARTEA, 1),
                blokea(LocalDate.of(2026, 9, 1), Astegunak.ASTEAZKENA, 1),
                blokea(LocalDate.of(2026, 9, 1), Astegunak.OSTEGUNA, 1),
                blokea(LocalDate.of(2026, 9, 1), Astegunak.OSTIRALA, 1));

        assertThat(service.hurrengoKlaseEgunak(
                egutegia, blokeak, LocalDate.of(2027, 6, 24), 15))
                .containsExactly(
                        LocalDate.of(2027, 6, 24), LocalDate.of(2027, 6, 25),
                        LocalDate.of(2027, 6, 28), LocalDate.of(2027, 6, 29),
                        LocalDate.of(2027, 6, 30));
    }

    @Test
    void ordutegiarenHasieraEtaTarteHutsaErrespetatzenDitu() {
        Egutegia egutegia = egutegia(LocalDate.of(2026, 10, 1), LocalDate.of(2027, 1, 31));
        KoadernoOrdutegiBlokea urrikoTarteHutsa = tarteHutsa(LocalDate.of(2026, 10, 1), false);
        KoadernoOrdutegiBlokea azarokoOrdutegia = blokea(
                LocalDate.of(2026, 11, 1), Astegunak.ASTELEHENA, 1);
        KoadernoOrdutegiBlokea urtarrilekoTarteHutsa = tarteHutsa(LocalDate.of(2027, 1, 1), false);

        List<LocalDate> egunak = service.hurrengoKlaseEgunak(egutegia,
                List.of(urrikoTarteHutsa, azarokoOrdutegia, urtarrilekoTarteHutsa),
                LocalDate.of(2026, 10, 1), 100);

        assertThat(egunak).isNotEmpty()
                .allMatch(data -> !data.isBefore(LocalDate.of(2026, 11, 1)))
                .allMatch(data -> data.isBefore(LocalDate.of(2027, 1, 1)));
    }

    @Test
    void dualOrdutegiaBaztertuEtaAurrekoOrdutegiakIndarreanJarraituDu() {
        Egutegia egutegia = egutegia(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31));
        List<LocalDate> egunak = service.hurrengoKlaseEgunak(egutegia,
                List.of(
                        blokea(LocalDate.of(2026, 11, 1), Astegunak.ASTELEHENA, 1),
                        tarteHutsa(LocalDate.of(2026, 12, 1), true)),
                LocalDate.of(2026, 11, 1), 10);

        assertThat(egunak)
                .contains(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 7))
                .allMatch(data -> data.getDayOfWeek() == DayOfWeek.MONDAY);
    }

    private Egutegia egutegia(LocalDate hasiera, LocalDate bukaera) {
        Egutegia egutegia = new Egutegia();
        egutegia.setHasieraData(hasiera);
        egutegia.setBukaeraData(bukaera);
        egutegia.setEgunBereziak(new ArrayList<>());
        return egutegia;
    }

    private KoadernoOrdutegiBlokea blokea(LocalDate hasiera, Astegunak asteguna, int iraupena) {
        KoadernoOrdutegiBlokea blokea = new KoadernoOrdutegiBlokea();
        blokea.setHasieraData(hasiera);
        blokea.setAsteguna(asteguna);
        blokea.setHasieraSlot(1);
        blokea.setIraupenaSlot(iraupena);
        return blokea;
    }

    private KoadernoOrdutegiBlokea tarteHutsa(LocalDate hasiera, boolean duala) {
        KoadernoOrdutegiBlokea blokea = new KoadernoOrdutegiBlokea();
        blokea.setHasieraData(hasiera);
        blokea.setTarteHutsa(!duala);
        blokea.setDualOrdutegia(duala);
        return blokea;
    }

    private EgunBerezi egunBerezia(LocalDate data, EgunMota mota, Astegunak ordezkatua) {
        EgunBerezi eguna = new EgunBerezi();
        eguna.setData(data);
        eguna.setMota(mota);
        eguna.setOrdezkatua(ordezkatua);
        return eguna;
    }
}
