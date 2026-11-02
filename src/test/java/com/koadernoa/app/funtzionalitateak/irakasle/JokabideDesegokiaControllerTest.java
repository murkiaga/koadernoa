package com.koadernoa.app.funtzionalitateak.irakasle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.koadernoa.app.objektuak.jokabidea.entitateak.JokabideDesegokia;
import com.koadernoa.app.objektuak.jokabidea.repository.JokabideDesegokiaRepository;
import com.koadernoa.app.objektuak.jokabidea.repository.NeurriZuzentzaileaRepository;
import com.koadernoa.app.objektuak.jokabidea.repository.PortaeraArrazoiaRepository;
import com.koadernoa.app.objektuak.jokabidea.service.IkasleEgunJardueraService;
import com.koadernoa.app.objektuak.jokabidea.service.JokabideDesegokiaPdfService;
import com.koadernoa.app.objektuak.jokabidea.service.JokabideDesegokiaService;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;

class JokabideDesegokiaControllerTest {
    private final JokabideDesegokiaRepository repository = mock(JokabideDesegokiaRepository.class);
    private final IkasleEgunJardueraService testuinguruService = mock(IkasleEgunJardueraService.class);
    private final JokabideDesegokiaPdfService pdfService = mock(JokabideDesegokiaPdfService.class);
    private final IrakasleaService irakasleaService = mock(IrakasleaService.class);
    private final Authentication auth = mock(Authentication.class);
    private final JokabideDesegokiaController controller = new JokabideDesegokiaController(
        mock(PortaeraArrazoiaRepository.class), mock(NeurriZuzentzaileaRepository.class),
        repository, testuinguruService, pdfService, mock(JokabideDesegokiaService.class),
        irakasleaService);

    @BeforeEach
    void hasiTransakzioSinkronizazioa() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void garbituTransakzioSinkronizazioa() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void jasoGabekoJokabideaEzabatzenDuEtaPdfaCommitOndorenKentzenDu() {
        Koadernoa koadernoa = koadernoa(4L);
        JokabideDesegokia jokabidea = jokabidea(koadernoa, false);
        when(repository.findById(7L)).thenReturn(Optional.of(jokabidea));

        var response = controller.ezabatu(7L, koadernoa, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(repository).delete(jokabidea);
        verify(repository).flush();
        verify(pdfService, never()).ezabatuIsilean("/tmp/jokabidea.pdf");
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
        verify(pdfService).ezabatuIsilean("/tmp/jokabidea.pdf");
    }

    @Test
    void kudeatzaileakJasotakoJokabideaEzDuEzabatzen() {
        Koadernoa koadernoa = koadernoa(4L);
        JokabideDesegokia jokabidea = jokabidea(koadernoa, true);
        when(repository.findById(7L)).thenReturn(Optional.of(jokabidea));

        var response = controller.ezabatu(7L, koadernoa, auth);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(((Map<?, ?>) response.getBody()).get("errorea").toString().contains("ezin da ezabatu"));
        verify(repository, never()).delete(jokabidea);
        assertFalse(TransactionSynchronizationManager.getSynchronizations().iterator().hasNext());
    }

    @Test
    void izeneanDagoenIrakasleakPdfaDeskargaDezake(@TempDir Path tempDir) throws IOException {
        Irakaslea irakaslea = irakaslea(10L);
        JokabideDesegokia jokabidea = new JokabideDesegokia();
        jokabidea.setId(7L);
        jokabidea.setIrakaslea(irakaslea);
        Path pdfa = tempDir.resolve("jokabidea.pdf");
        Files.write(pdfa, new byte[] {1, 2, 3});
        jokabidea.setPdfPath(pdfa.toString());
        jokabidea.setPdfFilename("jokabidea.pdf");
        when(repository.findById(7L)).thenReturn(Optional.of(jokabidea));
        when(irakasleaService.getLogeatutaDagoenIrakaslea(auth)).thenReturn(irakaslea);

        var response = controller.nirePdf(7L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("attachment; filename=\"jokabidea.pdf\"",
                response.getHeaders().getFirst("Content-Disposition"));
    }

    @Test
    void besteIrakasleBatekEzinDuPdfaDeskargatu() throws IOException {
        JokabideDesegokia jokabidea = new JokabideDesegokia();
        jokabidea.setId(7L);
        jokabidea.setIrakaslea(irakaslea(10L));
        when(repository.findById(7L)).thenReturn(Optional.of(jokabidea));
        when(irakasleaService.getLogeatutaDagoenIrakaslea(auth)).thenReturn(irakaslea(11L));

        var response = controller.nirePdf(7L, auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    private Koadernoa koadernoa(Long id) {
        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setId(id);
        return koadernoa;
    }

    private JokabideDesegokia jokabidea(Koadernoa koadernoa, boolean jasota) {
        Ikaslea ikaslea = new Ikaslea();
        ikaslea.setId(9L);
        JokabideDesegokia jokabidea = new JokabideDesegokia();
        jokabidea.setId(7L);
        jokabidea.setKoadernoa(koadernoa);
        jokabidea.setIkaslea(ikaslea);
        jokabidea.setJasota(jasota);
        jokabidea.setPdfPath("/tmp/jokabidea.pdf");
        return jokabidea;
    }

    private Irakaslea irakaslea(Long id) {
        Irakaslea irakaslea = new Irakaslea();
        irakaslea.setId(id);
        return irakaslea;
    }
}
