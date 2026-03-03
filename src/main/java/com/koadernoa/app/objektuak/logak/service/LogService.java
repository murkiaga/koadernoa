package com.koadernoa.app.objektuak.logak.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.logak.entitateak.LogMota;
import com.koadernoa.app.objektuak.logak.entitateak.LogSarrera;
import com.koadernoa.app.objektuak.logak.repository.LogSarreraRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogService {

    private final LogSarreraRepository logSarreraRepository;

    public void gorde(LogMota mota,
                      Irakaslea eragilea,
                      String entitateMota,
                      Long entitateId,
                      String deskribapena) {
        LogSarrera sarrera = new LogSarrera();
        sarrera.setMota(mota);
        sarrera.setData(LocalDateTime.now());
        if (eragilea != null) {
            sarrera.setEragileaId(eragilea.getId());
            sarrera.setEragileaIzena(eragilea.getIzena());
            sarrera.setEragileaEmaila(eragilea.getEmaila());
        }
        sarrera.setEntitateMota(entitateMota);
        sarrera.setEntitateId(entitateId);
        sarrera.setDeskribapena(deskribapena);

        logSarreraRepository.save(sarrera);
    }

    public List<LogSarrera> findAllOrderByDataDesc() {
        return logSarreraRepository.findAll().stream()
                .sorted(Comparator.comparing(LogSarrera::getData,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }
}
