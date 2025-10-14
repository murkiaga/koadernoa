package com.koadernoa.app.objektuak.modulua.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    
    //Inportazio excelean orain ikasle hau ez bada agertzen, taldea=null
    @Modifying
    @Query("""
      update Ikaslea i
      set i.taldea = null
      where i.taldea.id = :taldeaId
        and (:hnakIsEmpty = true or i.hna not in :hnak)
    """)
    int removeTaldeaForNotInHnaAndTaldea(@Param("hnak") Collection<String> hnak,
                                         @Param("taldeaId") Long taldeaId,
                                         @Param("hnakIsEmpty") boolean hnakIsEmpty);
    
    //Koadernoa inportazioa egin ostean sortu bada, inportatzeko aukera
    @Query("""
	      select i
	      from Ikaslea i
	      where i.taldea.id = :taldeaId
	        and not exists (
	            select 1 from Matrikula m
	            where m.ikaslea = i and m.koadernoa.id = :koadernoaId
	        )
	      order by i.abizena1 asc, i.abizena2 asc, i.izena asc
	    """)
	    List<Ikaslea> findTeamStudentsNotEnrolledInKoaderno(@Param("taldeaId") Long taldeaId,
	                                                        @Param("koadernoaId") Long koadernoaId);
}
