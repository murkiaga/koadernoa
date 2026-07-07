package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.egutegia.repository.EgutegiaRepository;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoAutomatikoSorreraService;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;

class KoadernoAutomatikoSorreraServiceTest {

    private final IkasturteaRepository ikasturteaRepository = mock(IkasturteaRepository.class);
    private final EgutegiaRepository egutegiaRepository = mock(EgutegiaRepository.class);
    private final ModuloaRepository moduloaRepository = mock(ModuloaRepository.class);
    private final KoadernoaRepository koadernoaRepository = mock(KoadernoaRepository.class);
    private final KoadernoaService koadernoaService = mock(KoadernoaService.class);

    private final KoadernoAutomatikoSorreraService service = new KoadernoAutomatikoSorreraService(
            ikasturteaRepository, egutegiaRepository, moduloaRepository, koadernoaRepository, koadernoaService);

    @Test
    void sortuFaltaDirenKoadernoakCreatesOnlyMissingActiveYearNotebooksWithoutOwner() {
        Ikasturtea ikasturtea = ikasturtea(1L, true);
        Maila maila = maila(10L);
        Egutegia egutegia = egutegia(20L, maila);
        Moduloa berria = moduloa(30L);
        Moduloa existitzenDena = moduloa(31L);

        when(ikasturteaRepository.findById(1L)).thenReturn(Optional.of(ikasturtea));
        when(egutegiaRepository.findByIkasturtea_Id(1L)).thenReturn(List.of(egutegia));
        when(moduloaRepository.findByMaila_IdAndAktiboTrue(10L)).thenReturn(List.of(berria, existitzenDena));
        Koadernoa existitzenDenKoadernoa = new Koadernoa();
        existitzenDenKoadernoa.setId(99L);
        when(koadernoaRepository.findFirstByModuloa_IdAndEgutegia_IdOrderByIdAsc(30L, 20L)).thenReturn(Optional.empty());
        when(koadernoaRepository.findFirstByModuloa_IdAndEgutegia_IdOrderByIdAsc(31L, 20L))
                .thenReturn(Optional.of(existitzenDenKoadernoa));
        when(koadernoaRepository.save(any(Koadernoa.class))).thenAnswer(inv -> inv.getArgument(0));

        KoadernoAutomatikoSorreraService.Emaitza emaitza = service.sortuFaltaDirenKoadernoak(1L);

        assertThat(emaitza.sortutakoak()).isEqualTo(1);
        assertThat(emaitza.lehendikZeudenak()).isEqualTo(1);
        ArgumentCaptor<Koadernoa> captor = ArgumentCaptor.forClass(Koadernoa.class);
        verify(koadernoaRepository).save(captor.capture());
        Koadernoa gordetakoa = captor.getValue();
        assertThat(gordetakoa.getModuloa()).isSameAs(berria);
        assertThat(gordetakoa.getEgutegia()).isSameAs(egutegia);
        assertThat(gordetakoa.getJabea()).isNull();
        assertThat(gordetakoa.getIrakasleak()).isEmpty();
    }

    @Test
    void sortuFaltaDirenKoadernoakDoesNotCreateWhenEveryLogicalNotebookExists() {
        Ikasturtea ikasturtea = ikasturtea(1L, true);
        Maila maila = maila(10L);
        Egutegia egutegia = egutegia(20L, maila);
        Moduloa moduloa = moduloa(30L);

        when(ikasturteaRepository.findById(1L)).thenReturn(Optional.of(ikasturtea));
        when(egutegiaRepository.findByIkasturtea_Id(1L)).thenReturn(List.of(egutegia));
        when(moduloaRepository.findByMaila_IdAndAktiboTrue(10L)).thenReturn(List.of(moduloa));
        Koadernoa existitzenDenKoadernoa = new Koadernoa();
        existitzenDenKoadernoa.setId(99L);
        when(koadernoaRepository.findFirstByModuloa_IdAndEgutegia_IdOrderByIdAsc(30L, 20L))
                .thenReturn(Optional.of(existitzenDenKoadernoa));

        KoadernoAutomatikoSorreraService.Emaitza emaitza = service.sortuFaltaDirenKoadernoak(1L);

        assertThat(emaitza.sortutakoak()).isZero();
        assertThat(emaitza.lehendikZeudenak()).isEqualTo(1);
        verify(koadernoaRepository, never()).save(any(Koadernoa.class));
    }

    @Test
    void sortuFaltaDirenKoadernoakRejectsInactiveYearAndCreatesNothing() {
        when(ikasturteaRepository.findById(1L)).thenReturn(Optional.of(ikasturtea(1L, false)));

        assertThatThrownBy(() -> service.sortuFaltaDirenKoadernoak(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ez dago aktibo");
        verify(koadernoaRepository, never()).save(any(Koadernoa.class));
    }

    private Ikasturtea ikasturtea(Long id, boolean aktiboa) {
        Ikasturtea ikasturtea = new Ikasturtea();
        ikasturtea.setId(id);
        ikasturtea.setAktiboa(aktiboa);
        return ikasturtea;
    }

    private Egutegia egutegia(Long id, Maila maila) {
        Egutegia egutegia = new Egutegia();
        egutegia.setId(id);
        egutegia.setMaila(maila);
        return egutegia;
    }

    private Maila maila(Long id) {
        Maila maila = new Maila();
        maila.setId(id);
        return maila;
    }

    private Moduloa moduloa(Long id) {
        Moduloa moduloa = new Moduloa();
        moduloa.setId(id);
        return moduloa;
    }
}
