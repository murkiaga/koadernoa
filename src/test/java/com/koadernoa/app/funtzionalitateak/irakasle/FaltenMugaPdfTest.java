package com.koadernoa.app.funtzionalitateak.irakasle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.denboralizazioa.*;
import com.koadernoa.app.objektuak.koadernoak.service.*;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.audit.service.AuditService;
import com.koadernoa.app.objektuak.modulua.entitateak.*;

@ExtendWith(MockitoExtension.class)
class FaltenMugaPdfTest {
    @Mock KoadernoaService koadernoak;
    @Mock DenboralizazioFaltaService faltakService;
    @Mock FaltenJakinarazpenPdfService pdfService;
    @Mock IrakasleaService irakasleak;
    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS) AuditService audit;
    @InjectMocks DenboralizazioaController controller;

    @ParameterizedTest
    @CsvSource({"20,20,false", "20,20.01,true", "25,21,false", "25,25,false", "25,25.01,true", "15,16,true", "4,5,true"})
    void pdfDeskargakModuluarenMailakoMugaErrespetatzenDu(int muga, double pct, boolean onartu) {
        Maila maila = new Maila();
        maila.setFaltenMugaPortzentaia(muga);
        Moduloa moduloa = new Moduloa();
        moduloa.setMaila(maila);
        Koadernoa koadernoa = new Koadernoa();
        koadernoa.setId(7L);
        koadernoa.setModuloa(moduloa);
        Matrikula matrikula = new Matrikula();
        matrikula.setId(1L);
        FaltaIkasleRow row = new FaltaIkasleRow();
        row.setMatrikula(matrikula);
        row.setFaltaPortzentaia(pct);
        FaltakBistaDTO faltak = new FaltakBistaDTO();
        faltak.setIkasleRows(List.of(row));
        when(koadernoak.findByIdWithEgutegiaAndEgunBereziak(7L)).thenReturn(Optional.of(koadernoa));
        when(faltakService.kalkulatuFaltenBista(koadernoa, 9, 2026)).thenReturn(faltak);
        if (onartu) when(pdfService.sortuPdf(any())).thenReturn(new byte[]{1});

        var response = controller.deskargatuFaltenJakinarazpenaPdf(koadernoa, 1L, 9, 2026, null);

        assertThat(response.getStatusCode().value()).isEqualTo(onartu ? 200 : 400);
        if (onartu) verify(pdfService).sortuPdf(argThat(data -> data.faltenMugaPortzentaia() == muga));
        else {
            verifyNoInteractions(pdfService);
            assertThat(response.getBody().toString()).contains("%" + muga);
        }
    }
}
