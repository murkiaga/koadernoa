package com.koadernoa.app.objektuak.audit.entitateak;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_event",
    indexes = {
        @Index(name = "idx_audit_event_data_ordua", columnList = "dataOrdua"),
        @Index(name = "idx_audit_event_mota", columnList = "mota"),
        @Index(name = "idx_audit_event_atala", columnList = "atala"),
        @Index(name = "idx_audit_event_ekintza", columnList = "ekintza"),
        @Index(name = "idx_audit_event_erabiltzaile_emaila", columnList = "erabiltzaileEmaila"),
        @Index(name = "idx_audit_event_koaderno_id", columnList = "koadernoId")
    }
)
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime dataOrdua;

    private Long erabiltzaileId;
    private String erabiltzaileEmaila;
    private String erabiltzaileIzena;
    private String rola;

    @Enumerated(EnumType.STRING)
    private AuditEventMota mota;

    @Enumerated(EnumType.STRING)
    private AuditAtala atala;

    @Enumerated(EnumType.STRING)
    private AuditEkintza ekintza;

    private String url;
    private String httpMethod;
    private String ip;
    private String userAgent;
    private Long koadernoId;
    private String entitateMota;
    private String entitateId;
    private boolean arrakastatsua;

    @Column(columnDefinition = "TEXT")
    private String xehetasunak;
}
