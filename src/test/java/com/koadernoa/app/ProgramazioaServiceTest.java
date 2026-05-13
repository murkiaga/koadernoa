package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Ebaluaketa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoOrdutegiBlokea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Programazioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.UnitateDidaktikoa;
import com.koadernoa.app.objektuak.koadernoak.repository.EbaluaketaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.JardueraPlanifikatuaRepository;
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
