package com.koadernoa.app.denboralizazioa;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunMota;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Jarduera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoOrdutegiBlokea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Programazioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.UnitateDidaktikoa;
import com.koadernoa.app.objektuak.koadernoak.repository.JardueraRepository;
import com.koadernoa.app.objektuak.koadernoak.service.DenboralizazioGeneratorService;

@ExtendWith(MockitoExtension.class)
class DenboralizazioGeneratorServiceTest {

    @Mock
    JardueraRepository jardueraRepository;

    @InjectMocks
    DenboralizazioGeneratorService service;

    Koadernoa k;
    Egutegia e;
    Programazioa p;

    LocalDate mon; // 2025-10-06
    LocalDate tue; // 2025-10-07
    LocalDate wed; // 2025-10-08
    LocalDate thu; // 2025-10-09
    LocalDate fri; // 2025-10-10

    @BeforeEach
    void setUp() {
        // Datak (2025-10-06 astelehena)
        mon = LocalDate.of(2025, 10, 6);
        tue = mon.plusDays(1);
        wed = mon.plusDays(2);
        thu = mon.plusDays(3);
        fri = mon.plusDays(4);

        // Egutegia
        e = new Egutegia();
        e.setHasieraData(mon);
        e.setBukaeraData(fri);

        // Egun bereziak: astelehena EZ_LEKTIBOA (baztertu), asteazkena ORDEZKATUA->ASTELEHENA
        List<EgunBerezi> bereziak = new ArrayList<>();
        EgunBerezi ezLektibo = new EgunBerezi();
        ezLektibo.setData(mon);
        ezLektibo.setMota(EgunMota.EZ_LEKTIBOA);
        bereziak.add(ezLektibo);

        EgunBerezi ordez = new EgunBerezi();
        ordez.setData(wed);
        ordez.setMota(EgunMota.ORDEZKATUA);
        ordez.setOrdezkatua(Astegunak.ASTELEHENA); // asteazkena → astelehena bezala tratatu
        bereziak.add(ordez);

        e.setEgunBereziak(bereziak);

        // Koadernoa + ordutegiak
        k = new Koadernoa();
        k.setEgutegia(e);

        List<KoadernoOrdutegiBlokea> blok = new ArrayList<>();
        // Astelehena: 2 slot
        KoadernoOrdutegiBlokea bMon = new KoadernoOrdutegiBlokea();
        bMon.setKoadernoa(k);
        bMon.setAsteguna(Astegunak.ASTELEHENA);
        bMon.setHasieraSlot(1);
        bMon.setIraupenaSlot(2);
        blok.add(bMon);
        // Asteazkena: 1 slot (baina ordezkatuta egongo da → astelehena 2 slot erabili beharko dira)
        KoadernoOrdutegiBlokea bWed = new KoadernoOrdutegiBlokea();
        bWed.setKoadernoa(k);
        bWed.setAsteguna(Astegunak.ASTEAZKENA);
        bWed.setHasieraSlot(3);
        bWed.setIraupenaSlot(1);
        blok.add(bWed);
        // Ostirala: 1 slot
        KoadernoOrdutegiBlokea bFri = new KoadernoOrdutegiBlokea();
        bFri.setKoadernoa(k);
        bFri.setAsteguna(Astegunak.OSTIRALA);
        bFri.setHasieraSlot(1);
        bFri.setIraupenaSlot(1);
        blok.add(bFri);

        k.setOrdutegiak(blok);

        // Programazioa: UD1 (3h), azpijarduerarik gabe
        p = new Programazioa();
        p.setKoadernoa(k);
        UnitateDidaktikoa ud1 = new UnitateDidaktikoa();
        ud1.setProgramazioa(p);
        ud1.setKodea("UD1");
        ud1.setIzenburua("Sarrera");
        ud1.setOrduak(3);
        ud1.setPosizioa(1);
        p.getUnitateak().add(ud1);
    }

    @Test
    @DisplayName("ORDEZKATUA → ordezko asteguneko slot kopurua erabiltzen da (asteazkena→astelehena=2 slot)")
    void testOrdezkatuAppliesEffectiveWeekdaySlots() {
        // preview=true: ez da DB-n idazten
        var preview = service.generateFromProgramazioa(k, p, true, false);

        // Sortutako aurreikuspena: asteazkena (ORDEZKATUA→ASTELEHENA) 2 ordu + ostirala 1 ordu
        assertFalse(preview.isEmpty());
        // Lehen sarrera: asteazkena 2025-10-08, 2h
        var first = preview.get(0);
        assertEquals(wed, first.data());
        assertEquals(2.0f, first.orduak(), 0.001);
        // Bigarren sarrera: ostirala 2025-10-10, 1h
        assertEquals(fri, preview.get(1).data());
        assertEquals(1.0f, preview.get(1).orduak(), 0.001);

        // Astelehena EZ_LEKTIBOA zenez, ez da inolako jarduerarik 2025-10-06an
        assertTrue(preview.stream().noneMatch(pi -> pi.data().equals(mon)));

        // Ez da DB-deposiziorik egin (preview=true)
        verify(jardueraRepository, never()).saveAll(anyIterable());
        verify(jardueraRepository, never()).deleteByKoadernoaAndDataBetweenAndMota(any(), any(), any(), any());
    }

    @Test
    @DisplayName("replaceExisting=true → tartean PLANIFIKATUA ezabatzen da eta gero sortu")
    void testReplaceExistingDeletesThenSaves() {
        // preview=false & replaceExisting=true
        var out = service.generateFromProgramazioa(k, p, false, true);
        assertEquals(2, out.size()); // 2+1 ordu banaketa

        // deleteBetween deia kapturatu
        ArgumentCaptor<LocalDate> fromCap = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> toCap = ArgumentCaptor.forClass(LocalDate.class);
        verify(jardueraRepository, times(1)).deleteByKoadernoaAndDataBetweenAndMota(eq(k), fromCap.capture(), toCap.capture(), eq("planifikatua"));
        assertEquals(wed, fromCap.getValue()); // lehen slot eguna: 2025-10-08
        assertEquals(fri, toCap.getValue());   // azken slot eguna: 2025-10-10

        // saveAll deitua izan da entitateak sortzeko
        ArgumentCaptor<Iterable<Jarduera>> listCap = ArgumentCaptor.forClass(Iterable.class);
        verify(jardueraRepository, times(1)).saveAll(listCap.capture());
        int count = 0;
        for (Jarduera j : listCap.getValue()) count++;
        assertEquals(2, count);
    }
}