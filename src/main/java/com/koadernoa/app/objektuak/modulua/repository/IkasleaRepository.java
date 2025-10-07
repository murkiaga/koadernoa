package com.koadernoa.app.objektuak.modulua.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

public interface IkasleaRepository extends JpaRepository<Ikaslea, Long> {
    Optional<Ikaslea> findByHna(String hna);
    boolean existsByHna(String hna);
    Optional<Ikaslea> findByNan(String nan);
    long countByTaldea_Id(Long taldeaId);
    
    interface TaldeKop {
        Long getTaldeaId();
        Long getKop();
    }

    @Query("""
        select i.taldea.id as taldeaId, count(i) as kop
        from Ikaslea i
        where i.taldea.id in :ids
        group by i.taldea.id
    """)
    List<TaldeKop> countByTaldeaIds(@Param("ids") List<Long> taldeaIds);
    
    // Ikasleak talde baten arabera, izenez ordenatuta:
    List<Ikaslea> findByTaldea_IdOrderByAbizena1AscAbizena2AscIzenaAsc(Long taldeaId);
    
    @Query("select distinct i.taldea from Ikaslea i where i.taldea is not null")
    List<Taldea> findDistinctTaldeakWithStudents();
}
