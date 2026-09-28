package com.koadernoa.app.objektuak.mezuak.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.objektuak.mezuak.entitateak.Mezua;

public interface MezuaRepository extends JpaRepository<Mezua, Long> {

    List<Mezua> findByHartzaileaIdOrderByBidalketaDataDesc(Long hartzaileaId);

    List<Mezua> findByBidaltzaileaIdOrHartzaileaIdOrderByBidalketaDataDesc(Long bidaltzaileaId, Long hartzaileaId);

    @Query("""
            select m from Mezua m
            where (m.bidaltzailea.id = :irakasleId or m.hartzailea.id = :irakasleId)
              and (:hasiera is null or m.bidalketaData >= :hasiera)
              and (:amaiera is null or m.bidalketaData < :amaiera)
            order by m.bidalketaData desc
            """)
    List<Mezua> findBidaliEtaJasotakoakDatenArtean(
            @Param("irakasleId") Long irakasleId,
            @Param("hasiera") LocalDateTime hasiera,
            @Param("amaiera") LocalDateTime amaiera);

    long countByHartzaileaIdAndIrakurritaFalse(Long hartzaileaId);

    @Modifying
    @Query("delete from Mezua m where m.bidaltzailea.id = :irakasleId or m.hartzailea.id = :irakasleId")
    int deleteByIrakasleaId(@Param("irakasleId") Long irakasleId);

    @Modifying
    @Query("""
            delete from Mezua m
            where m.id in :ids
              and (m.bidaltzailea.id = :irakasleId or m.hartzailea.id = :irakasleId)
            """)
    int deleteHautatuak(@Param("irakasleId") Long irakasleId, @Param("ids") Collection<Long> ids);
}
