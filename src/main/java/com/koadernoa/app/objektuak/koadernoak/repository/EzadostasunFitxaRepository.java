package com.koadernoa.app.objektuak.koadernoak.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.koadernoak.entitateak.EzadostasunFitxa;

public interface EzadostasunFitxaRepository extends JpaRepository<EzadostasunFitxa, Long> {
    Optional<EzadostasunFitxa> findByEstatistikaId(Long estatistikaId);
}
