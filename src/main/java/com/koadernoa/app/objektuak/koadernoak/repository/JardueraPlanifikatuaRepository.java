package com.koadernoa.app.objektuak.koadernoak.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.objektuak.koadernoak.entitateak.JardueraPlanifikatua;

public interface JardueraPlanifikatuaRepository extends JpaRepository<JardueraPlanifikatua, Long> {
	
    List<JardueraPlanifikatua> findByUnitateaId(Long unitateaId);

    //JP hori koaderno horren barruan dagoen egiaztatzeko (JP -> UD -> EBAL -> PROG -> KOAD)
    boolean existsByIdAndUnitatea_Ebaluaketa_Programazioa_Koadernoa_Id(Long jpId, Long koadernoId);

    //UD bateko JP kopurua
    long countByUnitatea_Id(Long udId);

    //UD bateko JP orduen batura (SUM)
    @Query("select coalesce(sum(j.orduak), 0) from JardueraPlanifikatua j where j.unitatea.id = :udId")
    Long sumOrduakByUdId(@Param("udId") Long udId);

    //UD anitzeko JP orduen batura (id → sum)
    @Query("""
           select j.unitatea.id, coalesce(sum(j.orduak), 0)
           from JardueraPlanifikatua j
           where j.unitatea.id in :udIds
           group by j.unitatea.id
           """)
    List<Object[]> sumOrduakByUdIds(@Param("udIds") List<Long> udIds);
}
