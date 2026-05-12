package com.koadernoa.app.objektuak.modulua.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.modulua.entitateak.MintegiModuluBaimena;

public interface MintegiModuluBaimenaRepository extends JpaRepository<MintegiModuluBaimena, Long> {
    List<MintegiModuluBaimena> findByFamilia_IdAndAktiboTrue(Long familiaId);
    boolean existsByFamilia_IdAndEeiKodeaIgnoreCaseAndAktiboTrue(Long familiaId, String eeiKodea);
}
