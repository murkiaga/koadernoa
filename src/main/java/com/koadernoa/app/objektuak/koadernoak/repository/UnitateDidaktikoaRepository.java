package com.koadernoa.app.objektuak.koadernoak.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.koadernoak.entitateak.UnitateDidaktikoa;

public interface UnitateDidaktikoaRepository extends JpaRepository<UnitateDidaktikoa, Long> {
    List<UnitateDidaktikoa> findByProgramazioaIdOrderByPosizioaAscIdAsc(Long programazioId);
    boolean existsByIdAndProgramazioa_Koadernoa_Id(Long udId, Long koadernoId);
}
