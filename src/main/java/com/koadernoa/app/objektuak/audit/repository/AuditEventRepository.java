package com.koadernoa.app.objektuak.audit.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.objektuak.audit.entitateak.AuditAtala;
import com.koadernoa.app.objektuak.audit.entitateak.AuditEkintza;
import com.koadernoa.app.objektuak.audit.entitateak.AuditEvent;
import com.koadernoa.app.objektuak.audit.entitateak.AuditEventMota;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    @Query("""
        SELECT COUNT(a) FROM AuditEvent a
        WHERE a.dataOrdua >= :hasiera AND a.dataOrdua < :bukaera
          AND (:mota IS NULL OR a.mota = :mota)
          AND (:atala IS NULL OR a.atala = :atala)
          AND (:ekintza IS NULL OR a.ekintza = :ekintza)
          AND (:emaila IS NULL OR LOWER(a.erabiltzaileEmaila) LIKE LOWER(CONCAT('%', :emaila, '%')))
          AND a.mota = :targetMota
    """)
    long countByMotaWithFilters(@Param("hasiera") LocalDateTime hasiera,
                                @Param("bukaera") LocalDateTime bukaera,
                                @Param("mota") AuditEventMota mota,
                                @Param("atala") AuditAtala atala,
                                @Param("ekintza") AuditEkintza ekintza,
                                @Param("emaila") String emaila,
                                @Param("targetMota") AuditEventMota targetMota);

    @Query("""
        SELECT a.atala AS izena, COUNT(a) AS kopurua
        FROM AuditEvent a
        WHERE a.dataOrdua >= :hasiera AND a.dataOrdua < :bukaera
          AND (:mota IS NULL OR a.mota = :mota)
          AND (:atala IS NULL OR a.atala = :atala)
          AND (:ekintza IS NULL OR a.ekintza = :ekintza)
          AND (:emaila IS NULL OR LOWER(a.erabiltzaileEmaila) LIKE LOWER(CONCAT('%', :emaila, '%')))
          AND a.atala IS NOT NULL
        GROUP BY a.atala
        ORDER BY COUNT(a) DESC
    """)
    List<AuditCountProjection> groupByAtala(@Param("hasiera") LocalDateTime hasiera,
                                            @Param("bukaera") LocalDateTime bukaera,
                                            @Param("mota") AuditEventMota mota,
                                            @Param("atala") AuditAtala atala,
                                            @Param("ekintza") AuditEkintza ekintza,
                                            @Param("emaila") String emaila);

    @Query("""
        SELECT a.ekintza AS izena, COUNT(a) AS kopurua
        FROM AuditEvent a
        WHERE a.dataOrdua >= :hasiera AND a.dataOrdua < :bukaera
          AND (:mota IS NULL OR a.mota = :mota)
          AND (:atala IS NULL OR a.atala = :atala)
          AND (:ekintza IS NULL OR a.ekintza = :ekintza)
          AND (:emaila IS NULL OR LOWER(a.erabiltzaileEmaila) LIKE LOWER(CONCAT('%', :emaila, '%')))
          AND a.ekintza IS NOT NULL
        GROUP BY a.ekintza
        ORDER BY COUNT(a) DESC
    """)
    List<AuditCountProjection> groupByEkintza(@Param("hasiera") LocalDateTime hasiera,
                                              @Param("bukaera") LocalDateTime bukaera,
                                              @Param("mota") AuditEventMota mota,
                                              @Param("atala") AuditAtala atala,
                                              @Param("ekintza") AuditEkintza ekintza,
                                              @Param("emaila") String emaila);

    @Query("""
        SELECT COALESCE(a.erabiltzaileEmaila, '(ezezaguna)') AS izena, COUNT(a) AS kopurua
        FROM AuditEvent a
        WHERE a.dataOrdua >= :hasiera AND a.dataOrdua < :bukaera
          AND a.mota = com.koadernoa.app.objektuak.audit.entitateak.AuditEventMota.LOGIN_FAIL
          AND (:emaila IS NULL OR LOWER(a.erabiltzaileEmaila) LIKE LOWER(CONCAT('%', :emaila, '%')))
        GROUP BY a.erabiltzaileEmaila
        ORDER BY COUNT(a) DESC
    """)
    List<AuditCountProjection> topLoginFailByEmail(@Param("hasiera") LocalDateTime hasiera,
                                                    @Param("bukaera") LocalDateTime bukaera,
                                                    @Param("emaila") String emaila,
                                                    Pageable pageable);

    @Query("""
        SELECT a FROM AuditEvent a
        WHERE a.dataOrdua >= :hasiera AND a.dataOrdua < :bukaera
          AND (:mota IS NULL OR a.mota = :mota)
          AND (:atala IS NULL OR a.atala = :atala)
          AND (:ekintza IS NULL OR a.ekintza = :ekintza)
          AND (:emaila IS NULL OR LOWER(a.erabiltzaileEmaila) LIKE LOWER(CONCAT('%', :emaila, '%')))
        ORDER BY a.dataOrdua DESC
    """)
    Page<AuditEvent> findRecentWithFilters(@Param("hasiera") LocalDateTime hasiera,
                                           @Param("bukaera") LocalDateTime bukaera,
                                           @Param("mota") AuditEventMota mota,
                                           @Param("atala") AuditAtala atala,
                                           @Param("ekintza") AuditEkintza ekintza,
                                           @Param("emaila") String emaila,
                                           Pageable pageable);

    @Query("""
        SELECT COUNT(DISTINCT a.erabiltzaileEmaila) FROM AuditEvent a
        WHERE a.dataOrdua >= :hasiera AND a.dataOrdua < :bukaera
          AND (:mota IS NULL OR a.mota = :mota)
          AND (:atala IS NULL OR a.atala = :atala)
          AND (:ekintza IS NULL OR a.ekintza = :ekintza)
          AND (:emaila IS NULL OR LOWER(a.erabiltzaileEmaila) LIKE LOWER(CONCAT('%', :emaila, '%')))
          AND a.erabiltzaileEmaila IS NOT NULL
    """)
    long countDistinctEmails(@Param("hasiera") LocalDateTime hasiera,
                             @Param("bukaera") LocalDateTime bukaera,
                             @Param("mota") AuditEventMota mota,
                             @Param("atala") AuditAtala atala,
                             @Param("ekintza") AuditEkintza ekintza,
                             @Param("emaila") String emaila);
}
