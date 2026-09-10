package com.koadernoa.app.objektuak.mezuak.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.objektuak.mezuak.entitateak.Mezua;

public interface MezuaRepository extends JpaRepository<Mezua, Long> {

    List<Mezua> findByHartzaileaIdOrderByBidalketaDataDesc(Long hartzaileaId);

    List<Mezua> findByBidaltzaileaIdOrHartzaileaIdOrderByBidalketaDataDesc(Long bidaltzaileaId, Long hartzaileaId);

    long countByHartzaileaIdAndIrakurritaFalse(Long hartzaileaId);

    @Modifying
    @Query("delete from Mezua m where m.bidaltzailea.id = :irakasleId or m.hartzailea.id = :irakasleId")
    int deleteByIrakasleaId(@Param("irakasleId") Long irakasleId);
}
