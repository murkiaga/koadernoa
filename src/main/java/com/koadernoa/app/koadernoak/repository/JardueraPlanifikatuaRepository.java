package com.koadernoa.app.koadernoak.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.koadernoak.entitateak.JardueraPlanifikatua;

public interface JardueraPlanifikatuaRepository extends JpaRepository<JardueraPlanifikatua, Long> {
    List<JardueraPlanifikatua> findByUnitateaId(Long unitateaId);
    boolean existsByIdAndUnitatea_Programazioa_Koadernoa_Id(Long jpId, Long koadernoId);
    
    long countByUnitatea_Id(Long udId);

    @Query("select coalesce(sum(j.orduak), 0) from JardueraPlanifikatua j where j.unitatea.id = :udId")
    Integer sumOrduakByUdId(@Param("udId") Long udId);
}
