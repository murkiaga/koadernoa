package com.koadernoa.app;

import static org.mockito.Mockito.inOrder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaEzabatzeService;
import com.koadernoa.app.objektuak.mezuak.repository.MezuaRepository;
import com.koadernoa.app.objektuak.ordutegiak.repository.IrakasleOrdutegiaRepository;

@ExtendWith(MockitoExtension.class)
class IrakasleaEzabatzeServiceTest {

    @Mock
    private MezuaRepository mezuaRepository;
    @Mock
    private IrakasleOrdutegiaRepository irakasleOrdutegiaRepository;
    @Mock
    private IrakasleaRepository irakasleaRepository;

    @InjectMocks
    private IrakasleaEzabatzeService service;

    @Test
    void mezuakEtaOrdutegiaIrakasleaBainoLehenEzabatzenDitu() {
        Irakaslea irakaslea = new Irakaslea();
        irakaslea.setId(42L);

        service.ezabatu(irakaslea);

        InOrder ordena = inOrder(mezuaRepository, irakasleOrdutegiaRepository, irakasleaRepository);
        ordena.verify(mezuaRepository).deleteByIrakasleaId(42L);
        ordena.verify(irakasleOrdutegiaRepository).deleteByIrakasleaId(42L);
        ordena.verify(irakasleaRepository).delete(irakaslea);
        ordena.verify(irakasleaRepository).flush();
    }
}
