package com.koadernoa.app.objektuak.mezuak.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.mezuak.entitateak.Mezua;

public interface MezuaRepository extends JpaRepository<Mezua, Long> {

    List<Mezua> findByHartzaileaIdOrderByBidalketaDataDesc(Long hartzaileaId);

    List<Mezua> findByBidaltzaileaIdOrHartzaileaIdOrderByBidalketaDataDesc(Long bidaltzaileaId, Long hartzaileaId);

    long countByHartzaileaIdAndIrakurritaFalse(Long hartzaileaId);
}
