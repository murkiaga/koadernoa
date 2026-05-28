package com.koadernoa.app.objektuak.koadernoak.entitateak;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EstatistikaEbaluazioanTest {

    @Test
    void getBertaratzePortzentaiaErabiltzenDuBertaratzeOinarriOrduak() {
        EstatistikaEbaluazioan estatistika = new EstatistikaEbaluazioan();
        estatistika.setBertaratzeOinarriOrduak(24 * 77);
        estatistika.setHutsegiteOrduak(423);

        assertEquals(77.11, estatistika.getBertaratzePortzentaia());
    }
}
