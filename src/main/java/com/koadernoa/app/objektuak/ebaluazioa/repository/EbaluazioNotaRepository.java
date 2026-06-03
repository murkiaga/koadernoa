package com.koadernoa.app.objektuak.ebaluazioa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioEgoera;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioMomentua;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioNota;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;

public interface EbaluazioNotaRepository extends JpaRepository<EbaluazioNota, Long> {

    Optional<EbaluazioNota> findByMatrikulaAndEbaluazioMomentua(Matrikula matrikula,
                                                                EbaluazioMomentua ebaluazioMomentua);

    @Modifying(flushAutomatically = true)
    @Query("""
        delete from EbaluazioNota n
        where n.matrikula.id = :matrikulaId
          and n.ebaluazioMomentua.id = :momentuaId
    """)
    void deleteByMatrikulaIdAndMomentuaId(@Param("matrikulaId") Long matrikulaId,
                                          @Param("momentuaId") Long momentuaId);

    // Koaderno bereko nota guztiak kargatzeko (hasierako pantailarako)
    List<EbaluazioNota> findByMatrikulaKoadernoa(Koadernoa koadernoa);
    
    boolean existsByEgoera(EbaluazioEgoera egoera);
    
    void deleteByMatrikula_Koadernoa_Id(Long koadernoId);

    @Query("""
        select n
        from EbaluazioNota n
        join fetch n.ebaluazioMomentua em
        left join fetch n.egoera eg
        where n.matrikula.id in :matrikulaIds
          and em.kodea in :momentuKodeak
    """)
    List<EbaluazioNota> findByMatrikulaIdsAndMomentuKodeak(@Param("matrikulaIds") List<Long> matrikulaIds,
                                                           @Param("momentuKodeak") List<String> momentuKodeak);
}
