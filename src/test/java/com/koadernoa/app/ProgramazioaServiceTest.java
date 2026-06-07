package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Ebaluaketa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoOrdutegiBlokea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Programazioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.UnitateDidaktikoa;
import com.koadernoa.app.objektuak.koadernoak.repository.EbaluaketaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.JardueraPlanifikatuaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.JardueraRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.ProgramazioaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.UnitateDidaktikoaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.DenboralizazioGeneratorService;
import com.koadernoa.app.objektuak.koadernoak.service.ProgramazioaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;

class ProgramazioaServiceTest {

    @Test
    void syncDualUdForProgramazioaReusesDualUdMovedToAnotherEbaluaketa() {
        ProgramazioaRepository programazioaRepository = mock(ProgramazioaRepository.class);
        ProgramazioaService service = new ProgramazioaService(
                programazioaRepository,
                mock(UnitateDidaktikoaRepository.class),
                mock(JardueraPlanifikatuaRepository.class),
                mock(JardueraRepository.class),
                mock(EbaluaketaRepository.class),
                mock(KoadernoaRepository.class),
                mock(DenboralizazioGeneratorService.class));

        Koadernoa koadernoa = new Koadernoa();
        Moduloa moduloa = new Moduloa();
        moduloa.setDualOrduak(120);
        koadernoa.setModuloa(moduloa);
        Egutegia egutegia = new Egutegia();
        egutegia.setHasieraData(LocalDate.of(2025, 9, 1));
        egutegia.setBukaeraData(LocalDate.of(2026, 6, 30));
        koadernoa.setEgutegia(egutegia);
        KoadernoOrdutegiBlokea dualBlokea = new KoadernoOrdutegiBlokea();
        dualBlokea.setDualOrdutegia(true);
        dualBlokea.setHasieraData(LocalDate.of(2026, 2, 25));
        koadernoa.setOrdutegiak(List.of(dualBlokea));

        Programazioa programazioa = new Programazioa();
        Ebaluaketa first = ebaluaketa(programazioa, 1L, LocalDate.of(2025, 9, 1), LocalDate.of(2025, 12, 31));
        Ebaluaketa second = ebaluaketa(programazioa, 2L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 24));
        Ebaluaketa third = ebaluaketa(programazioa, 3L, LocalDate.of(2026, 2, 25), LocalDate.of(2026, 6, 30));
        programazioa.setEbaluaketak(new ArrayList<>(List.of(first, second, third)));

        UnitateDidaktikoa movedDualUd = new UnitateDidaktikoa();
        movedDualUd.setId(10L);
        movedDualUd.setProgramazioa(programazioa);
        movedDualUd.setEbaluaketa(first);
        movedDualUd.setKodea("DUAL-20260225");
        movedDualUd.setIzenburua("DUALA");
        movedDualUd.setOrduak(100);
        first.getUnitateak().add(movedDualUd);

        service.syncDualUdForProgramazioa(koadernoa, programazioa);

        long dualUdCount = programazioa.getEbaluaketak().stream()
                .flatMap(e -> e.getUnitateak().stream())
                .filter(u -> "DUAL-20260225".equals(u.getKodea()))
                .count();
        assertThat(dualUdCount).isEqualTo(1);
        assertThat(movedDualUd.getEbaluaketa()).isSameAs(third);
        assertThat(movedDualUd.getOrduak()).isEqualTo(120);
        verify(programazioaRepository).save(programazioa);
    }

    @Test
    void deleteUdClearsScheduledActivitiesBeforeDeletingTheUnit() {
        UnitateDidaktikoaRepository udRepository = mock(UnitateDidaktikoaRepository.class);
        JardueraRepository jardueraRepository = mock(JardueraRepository.class);
        ProgramazioaService service = new ProgramazioaService(
                mock(ProgramazioaRepository.class),
                udRepository,
                mock(JardueraPlanifikatuaRepository.class),
                jardueraRepository,
                mock(EbaluaketaRepository.class),
                mock(KoadernoaRepository.class),
                mock(DenboralizazioGeneratorService.class));
        UnitateDidaktikoa ud = new UnitateDidaktikoa();
        ud.setId(10L);
        when(udRepository.findById(10L)).thenReturn(java.util.Optional.of(ud));

        service.deleteUd(10L);

        var inOrder = org.mockito.Mockito.inOrder(jardueraRepository, udRepository);
        inOrder.verify(jardueraRepository).clearUnitateaByUnitateaId(10L);
        inOrder.verify(udRepository).delete(ud);
    }

    @Test
    void syncDualUdForProgramazioaDeletesObsoleteDualUdWhenScheduleIsRemoved() {
        ProgramazioaRepository programazioaRepository = mock(ProgramazioaRepository.class);
        UnitateDidaktikoaRepository udRepository = mock(UnitateDidaktikoaRepository.class);
        JardueraRepository jardueraRepository = mock(JardueraRepository.class);
        ProgramazioaService service = new ProgramazioaService(
                programazioaRepository,
                udRepository,
                mock(JardueraPlanifikatuaRepository.class),
                jardueraRepository,
                mock(EbaluaketaRepository.class),
                mock(KoadernoaRepository.class),
                mock(DenboralizazioGeneratorService.class));

        Koadernoa koadernoa = new Koadernoa();
        Moduloa moduloa = new Moduloa();
        moduloa.setDualOrduak(120);
        koadernoa.setModuloa(moduloa);
        koadernoa.setOrdutegiak(List.of());

        Programazioa programazioa = new Programazioa();
        Ebaluaketa ebaluaketa = ebaluaketa(programazioa, 1L, LocalDate.of(2025, 9, 1), LocalDate.of(2026, 6, 30));
        programazioa.setEbaluaketak(new ArrayList<>(List.of(ebaluaketa)));
        UnitateDidaktikoa dualUd = new UnitateDidaktikoa();
        dualUd.setId(10L);
        dualUd.setProgramazioa(programazioa);
        dualUd.setEbaluaketa(ebaluaketa);
        dualUd.setKodea("DUAL-20260225");
        ebaluaketa.getUnitateak().add(dualUd);

        service.syncDualUdForProgramazioa(koadernoa, programazioa);

        assertThat(ebaluaketa.getUnitateak()).isEmpty();
        verify(jardueraRepository).clearUnitateaByUnitateaId(10L);
        verify(udRepository).delete(dualUd);
        verify(programazioaRepository).save(programazioa);
    }

    @Test
    void syncDualUdForProgramazioaDeletesExistingDualUdWhenDualHoursAreZero() {
        ProgramazioaRepository programazioaRepository = mock(ProgramazioaRepository.class);
        UnitateDidaktikoaRepository udRepository = mock(UnitateDidaktikoaRepository.class);
        JardueraRepository jardueraRepository = mock(JardueraRepository.class);
        ProgramazioaService service = new ProgramazioaService(
                programazioaRepository,
                udRepository,
                mock(JardueraPlanifikatuaRepository.class),
                jardueraRepository,
                mock(EbaluaketaRepository.class),
                mock(KoadernoaRepository.class),
                mock(DenboralizazioGeneratorService.class));

        Koadernoa koadernoa = new Koadernoa();
        Moduloa moduloa = new Moduloa();
        moduloa.setDualOrduak(0);
        koadernoa.setModuloa(moduloa);
        KoadernoOrdutegiBlokea dualBlokea = new KoadernoOrdutegiBlokea();
        dualBlokea.setDualOrdutegia(true);
        dualBlokea.setHasieraData(LocalDate.of(2026, 2, 25));
        koadernoa.setOrdutegiak(List.of(dualBlokea));

        Programazioa programazioa = new Programazioa();
        Ebaluaketa ebaluaketa = ebaluaketa(programazioa, 1L, LocalDate.of(2025, 9, 1), LocalDate.of(2026, 6, 30));
        programazioa.setEbaluaketak(new ArrayList<>(List.of(ebaluaketa)));
        UnitateDidaktikoa dualUd = new UnitateDidaktikoa();
        dualUd.setId(10L);
        dualUd.setProgramazioa(programazioa);
        dualUd.setEbaluaketa(ebaluaketa);
        dualUd.setKodea("DUAL-20260225");
        ebaluaketa.getUnitateak().add(dualUd);

        service.syncDualUdForProgramazioa(koadernoa, programazioa);

        assertThat(ebaluaketa.getUnitateak()).isEmpty();
        verify(jardueraRepository).clearUnitateaByUnitateaId(10L);
        verify(udRepository).delete(dualUd);
        verify(programazioaRepository).save(programazioa);
    }

    @Test
    void ebalOrduErabilgarriakUsesTarteHutsaAsZeroHourScheduleFromStartDate() {
        ProgramazioaService service = new ProgramazioaService(
                mock(ProgramazioaRepository.class),
                mock(UnitateDidaktikoaRepository.class),
                mock(JardueraPlanifikatuaRepository.class),
                mock(JardueraRepository.class),
                mock(EbaluaketaRepository.class),
                mock(KoadernoaRepository.class),
                mock(DenboralizazioGeneratorService.class));

        Egutegia egutegia = new Egutegia();
        egutegia.setHasieraData(LocalDate.of(2025, 9, 1));
        egutegia.setBukaeraData(LocalDate.of(2025, 9, 12));

        Koadernoa koadernoa = new Koadernoa();
        Moduloa moduloa = new Moduloa();
        moduloa.setDualOrduak(0);
        koadernoa.setModuloa(moduloa);
        koadernoa.setEgutegia(egutegia);

        Programazioa programazioa = new Programazioa();
        programazioa.setKoadernoa(koadernoa);
        Ebaluaketa ebal = ebaluaketa(programazioa, 1L, LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 12));
        programazioa.setEbaluaketak(new ArrayList<>(List.of(ebal)));

        KoadernoOrdutegiBlokea astekoOrdutegia = new KoadernoOrdutegiBlokea();
        astekoOrdutegia.setHasieraData(LocalDate.of(2025, 9, 1));
        astekoOrdutegia.setAsteguna(Astegunak.ASTELEHENA);
        astekoOrdutegia.setIraupenaSlot(2);

        KoadernoOrdutegiBlokea tarteHutsa = new KoadernoOrdutegiBlokea();
        tarteHutsa.setHasieraData(LocalDate.of(2025, 9, 8));
        tarteHutsa.setTarteHutsa(true);
        tarteHutsa.setAsteguna(null);
        tarteHutsa.setIraupenaSlot(0);

        Map<Long, Integer> emaitza = service.ebalOrduErabilgarriakBlokeekin(
                programazioa, egutegia, List.of(astekoOrdutegia, tarteHutsa));

        assertThat(emaitza).containsEntry(1L, 2);
    }

    private Ebaluaketa ebaluaketa(Programazioa programazioa, Long id, LocalDate hasiera, LocalDate bukaera) {
        Ebaluaketa e = new Ebaluaketa();
        e.setId(id);
        e.setProgramazioa(programazioa);
        e.setHasieraData(hasiera);
        e.setBukaeraData(bukaera);
        e.setUnitateak(new ArrayList<>());
        return e;
    }
}
