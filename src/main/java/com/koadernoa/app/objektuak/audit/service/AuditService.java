package com.koadernoa.app.objektuak.audit.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.audit.entitateak.AuditAtala;
import com.koadernoa.app.objektuak.audit.entitateak.AuditEkintza;
import com.koadernoa.app.objektuak.audit.entitateak.AuditEvent;
import com.koadernoa.app.objektuak.audit.entitateak.AuditEventMota;
import com.koadernoa.app.objektuak.audit.repository.AuditEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository auditEventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginOk(AuditEvent event) {
        event.setMota(AuditEventMota.LOGIN_OK);
        event.setArrakastatsua(true);
        saveSafely(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginFail(AuditEvent event) {
        event.setMota(AuditEventMota.LOGIN_FAIL);
        event.setArrakastatsua(false);
        saveSafely(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLogout(AuditEvent event) {
        event.setMota(AuditEventMota.LOGOUT);
        event.setArrakastatsua(true);
        saveSafely(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPageView(AuditEvent event) {
        event.setMota(AuditEventMota.PAGE_VIEW);
        saveSafely(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAction(AuditEvent event) {
        event.setMota(AuditEventMota.ACTION);
        saveSafely(event);
    }

    public AuditEvent buildBaseEvent(Long erabiltzaileId,
                                     String emaila,
                                     String erabiltzaileIzena,
                                     String rola,
                                     String url,
                                     String httpMethod,
                                     String ip,
                                     String userAgent,
                                     String xehetasunak,
                                     AuditAtala atala,
                                     AuditEkintza ekintza) {
        AuditEvent event = new AuditEvent();
        event.setDataOrdua(LocalDateTime.now());
        event.setErabiltzaileId(erabiltzaileId);
        event.setErabiltzaileEmaila(emaila);
        event.setErabiltzaileIzena(erabiltzaileIzena);
        event.setRola(rola);
        event.setUrl(url);
        event.setHttpMethod(httpMethod);
        event.setIp(ip);
        event.setUserAgent(userAgent);
        event.setXehetasunak(xehetasunak);
        event.setAtala(atala);
        event.setEkintza(ekintza);
        return event;
    }

    private void saveSafely(AuditEvent event) {
        try {
            if (event.getDataOrdua() == null) {
                event.setDataOrdua(LocalDateTime.now());
            }
            auditEventRepository.save(event);
        } catch (Exception ex) {
            log.warn("Audit event ezin izan da gorde; aplikazioaren exekuzioa ez da etengo.", ex);
        }
    }
}
