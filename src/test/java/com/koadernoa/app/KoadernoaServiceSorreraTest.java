package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioMomentuaRepository;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioNotaRepository;
import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.egutegia.repository.EgutegiaRepository;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoaSortuDto;
import com.koadernoa.app.objektuak.koadernoak.repository.AsistentziaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.EstatistikaEbaluazioanRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.JardueraRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoOrdutegiBlokeaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.NotaFitxategiaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.ProgramazioaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.SaioaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.UnitateDidaktikoaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoSorreraEmaitza;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoaService;
import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;
import com.koadernoa.app.objektuak.modulua.repository.MintegiModuluBaimenaRepository;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;

class KoadernoaServiceSorreraTest {

    private final ModuloaRepository moduloaRepository = mock(ModuloaRepository.class);
    private final EgutegiaRepository egutegiaRepository = mock(EgutegiaRepository.class);
    private final IrakasleaRepository irakasleaRepository = mock(IrakasleaRepository.class);
    private final KoadernoaRepository koadernoaRepository = mock(KoadernoaRepository.class);
    private final EbaluazioMomentuaRepository ebaluazioMomentuaRepository = mock(EbaluazioMomentuaRepository.class);
    private final AplikazioAukeraService aplikazioAukeraService = mock(AplikazioAukeraService.class);
    private final MintegiModuluBaimenaRepository mintegiModuluBaimenaRepository = mock(MintegiModuluBaimenaRepository.class);

    private final KoadernoaService service = new KoadernoaService(
            moduloaRepository,
            egutegiaRepository,
            irakasleaRepository,
            koadernoaRepository,
            mock(JardueraRepository.class),
            ebaluazioMomentuaRepository,
            mock(ProgramazioaRepository.class),
            mock(UnitateDidaktikoaRepository.class),
            mock(EstatistikaEbaluazioanRepository.class),
            mock(SaioaRepository.class),
            mock(MatrikulaRepository.class),
            mock(NotaFitxategiaRepository.class),
            mock(AsistentziaRepository.class),
            mock(EbaluazioNotaRepository.class),
            mock(KoadernoOrdutegiBlokeaRepository.class),
            aplikazioAukeraService,
            mintegiModuluBaimenaRepository);

    @Test
    void sortuEdoEsleituKoadernoaAssignsExistingOwnerlessNotebook() {
        TestDatuak datuak = prestatuDatuak();
        Koadernoa existitzenDena = new Koadernoa();
        existitzenDena.setId(99L);
        existitzenDena.setModuloa(datuak.moduloa());
        existitzenDena.setEgutegia(datuak.egutegia());
        existitzenDena.setIrakasleak(new ArrayList<>());
        when(koadernoaRepository.findFirstByModuloa_IdAndEgutegia_IdOrderByIdAsc(30L, 20L))
                .thenReturn(Optional.of(existitzenDena));
        when(koadernoaRepository.save(any(Koadernoa.class))).thenAnswer(inv -> inv.getArgument(0));

        KoadernoSorreraEmaitza emaitza = service.sortuEdoEsleituKoadernoa(dto(30L), datuak.irakaslea(), List.of());

        assertThat(emaitza.egoera()).isEqualTo(KoadernoSorreraEmaitza.Egoera.ESLEITUA_JABE_GABEA);
        assertThat(emaitza.mezua()).isEqualTo("Koadernoa zure izenean esleitu da.");
        assertThat(emaitza.koadernoa().getJabea()).isSameAs(datuak.irakaslea());
        assertThat(emaitza.koadernoa().getIrakasleak()).containsExactly(datuak.irakaslea());
    }

    @Test
    void sortuEdoEsleituKoadernoaAssignsScheduleBlocksWhenClaimingOwnerlessNotebook() {
        TestDatuak datuak = prestatuDatuak();
        Koadernoa existitzenDena = new Koadernoa();
        existitzenDena.setId(99L);
        existitzenDena.setModuloa(datuak.moduloa());
        existitzenDena.setEgutegia(datuak.egutegia());
        existitzenDena.setIrakasleak(new ArrayList<>());
        existitzenDena.setOrdutegiak(new ArrayList<>());
        when(koadernoaRepository.findFirstByModuloa_IdAndEgutegia_IdOrderByIdAsc(30L, 20L))
                .thenReturn(Optional.of(existitzenDena));
        when(koadernoaRepository.save(any(Koadernoa.class))).thenAnswer(inv -> inv.getArgument(0));

        KoadernoSorreraEmaitza emaitza = service.sortuEdoEsleituKoadernoa(
                dto(30L), datuak.irakaslea(), List.of("1-1", "1-2", "3-4"));

        assertThat(emaitza.egoera()).isEqualTo(KoadernoSorreraEmaitza.Egoera.ESLEITUA_JABE_GABEA);
        assertThat(emaitza.koadernoa().getOrdutegiak()).hasSize(2);
        assertThat(emaitza.koadernoa().getOrdutegiak()).allMatch(b -> b.getKoadernoa() == existitzenDena);
        assertThat(emaitza.koadernoa().getOrdutegiak()).allMatch(b -> LocalDate.of(2025, 9, 1).equals(b.getHasieraData()));
        assertThat(emaitza.koadernoa().getOrdutegiak()).anySatisfy(b -> {
            assertThat(b.getAsteguna()).isEqualTo(Astegunak.ASTELEHENA);
            assertThat(b.getHasieraSlot()).isEqualTo(1);
            assertThat(b.getIraupenaSlot()).isEqualTo(2);
        });
        assertThat(emaitza.koadernoa().getOrdutegiak()).anySatisfy(b -> {
            assertThat(b.getAsteguna()).isEqualTo(Astegunak.ASTEAZKENA);
            assertThat(b.getHasieraSlot()).isEqualTo(4);
            assertThat(b.getIraupenaSlot()).isEqualTo(1);
        });
    }

    @Test
    void sortuEdoEsleituKoadernoaDoesNotOverwriteExistingOwnerAndAvoidsUnknownMessage() {
        TestDatuak datuak = prestatuDatuak();
        Irakaslea jabea = new Irakaslea();
        jabea.setId(2L);
        jabea.setIzena("Ane Jabea");
        Koadernoa existitzenDena = new Koadernoa();
        existitzenDena.setId(99L);
        existitzenDena.setModuloa(datuak.moduloa());
        existitzenDena.setEgutegia(datuak.egutegia());
        existitzenDena.setJabea(jabea);
        when(koadernoaRepository.findFirstByModuloa_IdAndEgutegia_IdOrderByIdAsc(30L, 20L))
                .thenReturn(Optional.of(existitzenDena));

        KoadernoSorreraEmaitza emaitza = service.sortuEdoEsleituKoadernoa(dto(30L), datuak.irakaslea(), List.of());

        assertThat(emaitza.egoera()).isEqualTo(KoadernoSorreraEmaitza.Egoera.EXISTITZEN_DA);
        assertThat(emaitza.mezua()).contains("Jabea: Ane Jabea.");
        assertThat(emaitza.mezua()).doesNotContain("ezezaguna");
    }

    @Test
    void sortuEdoEsleituKoadernoaCreatesNewNotebookWithCreatorAsOwnerAndTeacher() {
        TestDatuak datuak = prestatuDatuak();
        when(koadernoaRepository.findFirstByModuloa_IdAndEgutegia_IdOrderByIdAsc(30L, 20L)).thenReturn(Optional.empty());
        when(ebaluazioMomentuaRepository.findByMailaAndAktiboTrueOrderByOrdenaAsc(datuak.maila())).thenReturn(List.of());
        when(koadernoaRepository.save(any(Koadernoa.class))).thenAnswer(inv -> inv.getArgument(0));

        KoadernoSorreraEmaitza emaitza = service.sortuEdoEsleituKoadernoa(dto(30L), datuak.irakaslea(), List.of());

        assertThat(emaitza.egoera()).isEqualTo(KoadernoSorreraEmaitza.Egoera.SORTUA);
        ArgumentCaptor<Koadernoa> captor = ArgumentCaptor.forClass(Koadernoa.class);
        verify(koadernoaRepository).save(captor.capture());
        assertThat(captor.getValue().getJabea()).isSameAs(datuak.irakaslea());
        assertThat(captor.getValue().getIrakasleak()).containsExactly(datuak.irakaslea());
    }

    private TestDatuak prestatuDatuak() {
        Familia familia = new Familia();
        familia.setId(1L);
        Zikloa zikloa = new Zikloa();
        zikloa.setFamilia(familia);
        Taldea taldea = new Taldea();
        taldea.setZikloa(zikloa);
        Maila maila = new Maila();
        maila.setId(10L);
        Ikasturtea ikasturtea = new Ikasturtea();
        ikasturtea.setIzena("2025-2026");
        Egutegia egutegia = new Egutegia();
        egutegia.setId(20L);
        egutegia.setHasieraData(LocalDate.of(2025, 9, 1));
        egutegia.setMaila(maila);
        egutegia.setIkasturtea(ikasturtea);
        Moduloa moduloa = new Moduloa();
        moduloa.setId(30L);
        moduloa.setIzena("Datu-baseak");
        moduloa.setMaila(maila);
        moduloa.setTaldea(taldea);
        Irakaslea irakaslea = new Irakaslea();
        irakaslea.setId(1L);
        irakaslea.setMintegia(familia);
        when(moduloaRepository.findById(30L)).thenReturn(Optional.of(moduloa));
        when(egutegiaRepository.findByIkasturtea_AktiboaTrueAndMaila_Id(10L)).thenReturn(Optional.of(egutegia));
        when(aplikazioAukeraService.getBool(AplikazioAukeraService.KOADERNO_BESTE_MINTEGIA_BAIMENDU, false)).thenReturn(false);
        return new TestDatuak(maila, egutegia, moduloa, irakaslea);
    }

    private KoadernoaSortuDto dto(Long moduloaId) {
        KoadernoaSortuDto dto = new KoadernoaSortuDto();
        dto.setModuloaId(moduloaId);
        return dto;
    }

    private record TestDatuak(Maila maila, Egutegia egutegia, Moduloa moduloa, Irakaslea irakaslea) {
    }
}
