package com.koadernoa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.IrakasleOrdutegia;
import com.koadernoa.app.objektuak.ordutegiak.repository.IrakasleOrdutegiaRepository;
import com.koadernoa.app.objektuak.ordutegiak.service.KronowinOrdutegiImportService;
import com.koadernoa.app.objektuak.ordutegiak.service.KronowinOrdutegiXmlParser;
import com.koadernoa.app.objektuak.ordutegiak.service.OrdutegiImportResult;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;

class KronowinOrdutegiImportServiceTest {
    private final IkasturteaRepository ikasturteaRepository = mock(IkasturteaRepository.class);
    private final IrakasleaRepository irakasleaRepository = mock(IrakasleaRepository.class);
    private final TaldeaRepository taldeaRepository = mock(TaldeaRepository.class);
    private final IrakasleOrdutegiaRepository irakasleOrdutegiaRepository = mock(IrakasleOrdutegiaRepository.class);

    private final KronowinOrdutegiImportService service = new KronowinOrdutegiImportService(
            new KronowinOrdutegiXmlParser(), ikasturteaRepository, irakasleaRepository,
            taldeaRepository, irakasleOrdutegiaRepository);

    @Test
    void dia3EtaHora4SolucfBatekAsteazkenaEtaLaugarrenOrduaSortzenDitu() {
        when(ikasturteaRepository.findById(1L)).thenReturn(Optional.of(ikasturtea()));
        Irakaslea irakaslea = new Irakaslea();
        irakaslea.setId(10L);
        irakaslea.setEmaila("urki@example.test");
        when(irakasleaRepository.findByEmailaIgnoreCase("urki@example.test")).thenReturn(Optional.of(irakaslea));

        OrdutegiImportResult result = service.inportatu(xmlFile(solucf("URKI", "3", "4")), 1L);

        assertThat(result.getSortutakoLerroKopurua()).isEqualTo(1);
        ArgumentCaptor<Iterable<IrakasleOrdutegia>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(irakasleOrdutegiaRepository).saveAll(captor.capture());
        ArrayList<IrakasleOrdutegia> gordetakoak = new ArrayList<>();
        captor.getValue().forEach(gordetakoak::add);
        assertThat(gordetakoak).hasSize(1);
        assertThat(gordetakoak.get(0).getLerroak()).hasSize(1);
        assertThat(gordetakoak.get(0).getLerroak().get(0).getAsteguna()).isEqualTo(Astegunak.ASTEAZKENA);
        assertThat(gordetakoak.get(0).getLerroak().get(0).getOrduZenbakia()).isEqualTo(4);
    }

    @Test
    void dia0EtaHora0SolucfBatekEzDuOrdutegiLerrorikSortzen() {
        when(ikasturteaRepository.findById(1L)).thenReturn(Optional.of(ikasturtea()));
        when(irakasleaRepository.findByEmailaIgnoreCase("urki@example.test")).thenReturn(Optional.of(new Irakaslea()));

        OrdutegiImportResult result = service.inportatu(xmlFile(solucf("URKI", "0", "0")), 1L);

        assertThat(result.getSortutakoLerroKopurua()).isZero();
        assertThat(result.getSaltatutakoKopurua()).isEqualTo(1);
        assertThat(result.getSaltatutakoLerroak().get(0).arrazoia()).contains("DIA edo HORA 0 da");
        verify(irakasleOrdutegiaRepository).saveAll(org.mockito.ArgumentMatchers.argThat(iterable -> !iterable.iterator().hasNext()));
    }

    @Test
    void xmlEmailaLotutaEzBadagoEzDaIrakasleBerririkSortzen() {
        when(ikasturteaRepository.findById(1L)).thenReturn(Optional.of(ikasturtea()));
        when(irakasleaRepository.findByEmailaIgnoreCase("urki@example.test")).thenReturn(Optional.empty());

        service.inportatu(xmlFile(solucf("URKI", "3", "4")), 1L);

        verify(irakasleaRepository, never()).save(org.mockito.ArgumentMatchers.any(Irakaslea.class));
        verify(irakasleOrdutegiaRepository).saveAll(org.mockito.ArgumentMatchers.argThat(iterable -> !iterable.iterator().hasNext()));
    }

    @Test
    void lotuGabekoIrakasleaImportResultEanAgertzenDa() {
        when(ikasturteaRepository.findById(1L)).thenReturn(Optional.of(ikasturtea()));
        when(irakasleaRepository.findByEmailaIgnoreCase("urki@example.test")).thenReturn(Optional.empty());

        OrdutegiImportResult result = service.inportatu(xmlFile(solucf("URKI", "3", "4")), 1L);

        assertThat(result.getLotuGabekoIrakasleak())
                .containsExactly("URKI - Urkiaga, Mikel (urki@example.test) ez dago aplikazioko irakasle batekin lotuta");
        assertThat(result.getSortutakoLerroKopurua()).isZero();
        verify(irakasleOrdutegiaRepository).saveAll(org.mockito.ArgumentMatchers.argThat(iterable -> !iterable.iterator().hasNext()));
    }

    private Ikasturtea ikasturtea() {
        Ikasturtea ikasturtea = new Ikasturtea();
        ikasturtea.setId(1L);
        ikasturtea.setIzena("2025-2026");
        return ikasturtea;
    }

    private MockMultipartFile xmlFile(String solucf) {
        String xml = """
                <SERVICIO>
                  <PROFT><PROFF ABREV="URKI" NOMBRE="Urkiaga, Mikel" DEPART="INF" EMAIL="urki@example.test"/></PROFT>
                  <NOMASIGT><NOMASIGF ABREV="RELO" NOMBRE="Sare lokalak"/></NOMASIGT>
                  <AULAT><AULAF ABREV="TSMA" NOMBRE="Tailerra"/></AULAT>
                  <SOLUCT>%s</SOLUCT>
                </SERVICIO>
                """.formatted(solucf);
        return new MockMultipartFile("fitxategia", "PROBA.xml", "application/xml", xml.getBytes(StandardCharsets.UTF_8));
    }

    private String solucf(String prof, String dia, String hora) {
        return "<SOLUCF ASIG=\"RELO\" AULA=\"TSMA\" CODGRUPO=\"1SM2A\" CURSO=\"1\" DIA=\"" + dia
                + "\" GRUPO=\"A\" HORA=\"" + hora
                + "\" NIVEL=\"EM\" PROF=\"" + prof
                + "\" SESIONES=\"1\" TAREA=\"\" TURNO=\"G\" MARCO=\"0\"/>";
    }
}
