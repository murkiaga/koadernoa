package com.koadernoa.app.objektuak.koadernoak.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.objektuak.koadernoak.entitateak.JardueraKalifikazioa;

public interface JardueraKalifikazioaRepository extends JpaRepository<JardueraKalifikazioa, Long> {
    @Query("""
            select jk from JardueraKalifikazioa jk
            join fetch jk.jarduera j
            join fetch jk.matrikula m
            where j.koadernoa.id = :koadernoId
            """)
    List<JardueraKalifikazioa> findByKoadernoId(@Param("koadernoId") Long koadernoId);

    void deleteByJarduera_IdAndMatrikula_Id(Long jardueraId, Long matrikulaId);
}
