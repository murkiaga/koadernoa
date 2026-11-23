package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioMomentua;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Asistentzia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Asistentzia.AsistentziaEgoera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Ebaluaketa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EstatistikaEbaluazioan;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Jarduera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Programazioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa.SaioEgoera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.UnitateDidaktikoa;
import com.koadernoa.app.objektuak.koadernoak.repository.AsistentziaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.EstatistikaEbaluazioanRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.JardueraRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.ProgramazioaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.SaioaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.EstatistikaService;
import com.koadernoa.app.objektuak.koadernoak.service.ProgramazioaService;
import com.koadernoa.app.objektuak.modulua.entitateak.MatrikulaEgoera;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;

class EstatistikaServiceTest {

    @Test
    void egindakoJarduerakLotutakoUdaEtaSaioakEmandatzatKalkulatzenDitu() {
        EstatistikaEbaluazioanRepository estatRepo = mock(EstatistikaEbaluazioanRepository.class);
        ProgramazioaRepository programazioaRepository = mock(ProgramazioaRepository.class);
        JardueraRepository jardueraRepository = mock(JardueraRepository.class);
        SaioaRepository saioaRepository = mock(SaioaRepository.class);
        MatrikulaRepository matrikulaRepository = mock(MatrikulaRepository.class);
        AsistentziaRepository asistentziaRepository = mock(AsistentziaRepository.class);
        ProgramazioaService programazioaService = mock(ProgramazioaService.class);

        EstatistikaService service = new EstatistikaService(
                estatRepo,
                programazioaRepository,
                jardueraRepository,
                saioaRepository,
                matrikulaRepository,
                asistentziaRepository,
                programazioaService);

        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setId(10L);
        Egutegia egutegia = new Egutegia();
        egutegia.setHasieraData(LocalDate.of(2025, 9, 1));
        egutegia.setLehenEbalBukaera(LocalDate.of(2025, 12, 31));
        egutegia.setBukaeraData(LocalDate.of(2026, 6, 30));
        koadernoa.setEgutegia(egutegia);

        EbaluazioMomentua momentua = new EbaluazioMomentua();
        momentua.setId(1L);
        momentua.setKodea("1_EBAL");
        momentua.setUrteOsoa(false);

        EstatistikaEbaluazioan estatistika = new EstatistikaEbaluazioan();
        estatistika.setId(100L);
        estatistika.setKoadernoa(koadernoa);
        estatistika.setEbaluazioMomentua(momentua);

        Programazioa programazioa = new Programazioa();
        programazioa.setKoadernoa(koadernoa);
        Ebaluaketa ebaluaketa = new Ebaluaketa();
        ebaluaketa.setId(77L);
        ebaluaketa.setProgramazioa(programazioa);
        ebaluaketa.setHasieraData(LocalDate.of(2025, 9, 1));
        ebaluaketa.setBukaeraData(LocalDate.of(2025, 12, 31));

        UnitateDidaktikoa lehenUda = new UnitateDidaktikoa();
        lehenUda.setId(201L);
        lehenUda.setKodea("UD1");
        lehenUda.setIzenburua("Lehen UDa");
        lehenUda.setEbaluaketa(ebaluaketa);
        UnitateDidaktikoa bigarrenUda = new UnitateDidaktikoa();
        bigarrenUda.setId(202L);
        bigarrenUda.setKodea("UD2");
        bigarrenUda.setIzenburua("Bigarren UDa");
        bigarrenUda.setEbaluaketa(ebaluaketa);
        ebaluaketa.setUnitateak(List.of(lehenUda, bigarrenUda));
        programazioa.setEbaluaketak(List.of(ebaluaketa));

        Jarduera eginda = jarduera(301L, "Testu guztiz editatua", "Ez dauka UD kodea testuan", "eginda", 1.5f, lehenUda);
        Jarduera egindaUdBera = jarduera(302L, "Eskuz sortua", null, " EGINDA ", 0.5f, lehenUda);
        Jarduera egindaUdGabe = jarduera(303L, "Beste saio bat", null, "eginda", 1f, null);
        Jarduera planifikatua = jarduera(304L, "UD2 — planifikatua", null, "planifikatua", 8f, bigarrenUda);
        Jarduera ebaluazioa = jarduera(305L, "Azterketa", null, "ebaluazioa", 4f, bigarrenUda);

        when(estatRepo.findById(100L)).thenReturn(Optional.of(estatistika));
        when(programazioaRepository.findByKoadernoaId(10L)).thenReturn(Optional.of(programazioa));
        when(programazioaService.kalkulatuEbaluazioOrduak(programazioa, koadernoa)).thenReturn(Map.of(77L, 20));
        when(jardueraRepository.findByKoadernoaIdAndDataBetweenOrderByDataAscIdAsc(
                eq(10L), eq(LocalDate.of(2025, 9, 1)), eq(LocalDate.of(2025, 12, 31))))
                .thenReturn(List.of(eginda, egindaUdBera, egindaUdGabe, planifikatua, ebaluazioa));
        when(saioaRepository.findByKoadernoa_IdAndDataBetweenAndEgoera(
                eq(10L), eq(LocalDate.of(2025, 9, 1)), eq(LocalDate.of(2025, 12, 31)), eq(SaioEgoera.AKTIBOA)))
                .thenReturn(List.of());
        when(matrikulaRepository.findByKoadernoa_IdAndEgoera(10L, MatrikulaEgoera.MATRIKULATUA))
                .thenReturn(List.of());

        service.berkalkulatuEstatistika(koadernoa, 100L);

        assertThat(estatistika.getUnitateakEmanda()).isEqualTo(1);
        assertThat(estatistika.getOrduakEmanda()).isEqualTo(3);
        assertThat(estatistika.getUnitateakAurreikusiak()).isEqualTo(2);
        assertThat(estatistika.getOrduakAurreikusiak()).isEqualTo(20);
    }

    @Test
    void bertaratzeOinarriaProgramaziokoEbaluazioOrduekinEtaMatrikulatutakoIkasleekinKalkulatzenDu() {
        EstatistikaEbaluazioanRepository estatRepo = mock(EstatistikaEbaluazioanRepository.class);
        ProgramazioaRepository programazioaRepository = mock(ProgramazioaRepository.class);
        JardueraRepository jardueraRepository = mock(JardueraRepository.class);
        SaioaRepository saioaRepository = mock(SaioaRepository.class);
        MatrikulaRepository matrikulaRepository = mock(MatrikulaRepository.class);
        AsistentziaRepository asistentziaRepository = mock(AsistentziaRepository.class);
        ProgramazioaService programazioaService = mock(ProgramazioaService.class);

        EstatistikaService service = new EstatistikaService(
                estatRepo,
                programazioaRepository,
                jardueraRepository,
                saioaRepository,
                matrikulaRepository,
                asistentziaRepository,
                programazioaService);

        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setId(10L);
        Egutegia egutegia = new Egutegia();
        egutegia.setHasieraData(LocalDate.of(2025, 9, 1));
        egutegia.setLehenEbalBukaera(LocalDate.of(2025, 12, 31));
        egutegia.setBukaeraData(LocalDate.of(2026, 6, 30));
        koadernoa.setEgutegia(egutegia);

        EbaluazioMomentua momentua = new EbaluazioMomentua();
        momentua.setId(1L);
        momentua.setKodea("1_EBAL");
        momentua.setUrteOsoa(false);

        EstatistikaEbaluazioan estatistika = new EstatistikaEbaluazioan();
        estatistika.setId(100L);
        estatistika.setKoadernoa(koadernoa);
        estatistika.setEbaluazioMomentua(momentua);

        Programazioa programazioa = new Programazioa();
        programazioa.setKoadernoa(koadernoa);
        Ebaluaketa ebaluaketa = new Ebaluaketa();
        ebaluaketa.setId(77L);
        ebaluaketa.setProgramazioa(programazioa);
        ebaluaketa.setHasieraData(LocalDate.of(2025, 9, 1));
        ebaluaketa.setBukaeraData(LocalDate.of(2025, 12, 31));
        programazioa.setEbaluaketak(List.of(ebaluaketa));

        Saioa saioa = new Saioa();
        saioa.setId(500L);
        saioa.setIraupenaSlot(423);
        saioa.setEgoera(SaioEgoera.AKTIBOA);
        Asistentzia hutsa = new Asistentzia();
        hutsa.setSaioa(saioa);
        hutsa.setEgoera(AsistentziaEgoera.HUTS);

        when(estatRepo.findById(100L)).thenReturn(Optional.of(estatistika));
        when(programazioaRepository.findByKoadernoaId(10L)).thenReturn(Optional.of(programazioa));
        when(programazioaService.kalkulatuEbaluazioOrduak(programazioa, koadernoa)).thenReturn(Map.of(77L, 77));
        when(jardueraRepository.findByKoadernoaIdAndDataBetweenOrderByDataAscIdAsc(
                eq(10L), eq(LocalDate.of(2025, 9, 1)), eq(LocalDate.of(2025, 12, 31))))
                .thenReturn(List.of());
        when(saioaRepository.findByKoadernoa_IdAndDataBetweenAndEgoera(
                eq(10L), eq(LocalDate.of(2025, 9, 1)), eq(LocalDate.of(2025, 12, 31)), eq(SaioEgoera.AKTIBOA)))
                .thenReturn(List.of(saioa));
        when(asistentziaRepository.findBySaioa_IdInAndEgoeraIn(eq(List.of(500L)), anyList()))
                .thenReturn(List.of(hutsa));
        // 25 ikasletik 24 bakarrik dira MATRIKULATUA; PENDIENTE_AURREKO_URTETIK egoerakoa ez da zenbatzen.
        when(matrikulaRepository.countByKoadernoa_IdAndEgoera(10L, MatrikulaEgoera.MATRIKULATUA))
                .thenReturn(24L);
        when(matrikulaRepository.findByKoadernoa_IdAndEgoera(10L, MatrikulaEgoera.MATRIKULATUA))
                .thenReturn(List.of());

        service.berkalkulatuEstatistika(koadernoa, 100L);

        assertThat(estatistika.getOrduakAurreikusiak()).isEqualTo(77);
        assertThat(estatistika.getBertaratzeOinarriOrduak()).isEqualTo(1848);
        assertThat(estatistika.getHutsegiteOrduak()).isEqualTo(423);
        assertThat(estatistika.getBertaratzePortzentaia()).isEqualTo(77.11);
        verify(matrikulaRepository).countByKoadernoa_IdAndEgoera(10L, MatrikulaEgoera.MATRIKULATUA);
        verify(estatRepo).save(estatistika);
    }

    private Jarduera jarduera(Long id, String titulua, String deskribapena, String mota, float orduak,
            UnitateDidaktikoa unitatea) {
        Jarduera jarduera = new Jarduera();
        jarduera.setId(id);
        jarduera.setTitulua(titulua);
        jarduera.setDeskribapena(deskribapena);
        jarduera.setMota(mota);
        jarduera.setOrduak(orduak);
        jarduera.setUnitatea(unitatea);
        return jarduera;
    }
}
