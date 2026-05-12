package com.koadernoa.app.objektuak.modulua.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.koadernoa.app.objektuak.modulua.entitateak.MintegiModuluBaimena;

public interface MintegiModuluBaimenaRepository extends JpaRepository<MintegiModuluBaimena, Long> {
    List<MintegiModuluBaimena> findByFamilia_IdAndAktiboTrue(Long familiaId);
    boolean existsByFamilia_IdAndEeiKodeaIgnoreCaseAndAktiboTrue(Long familiaId, String eeiKodea);
    boolean existsByFamilia_IdAndEeiKodeaIgnoreCase(Long familiaId, String eeiKodea);
    Optional<MintegiModuluBaimena> findByFamilia_IdAndEeiKodeaIgnoreCase(Long familiaId, String eeiKodea);

    @Query("""
        select b from MintegiModuluBaimena b
        join b.familia f
        order by f.izena asc, b.eeiKodea asc, b.id asc
    """)
    List<MintegiModuluBaimena> findAllOrdenatuta();
}
