package com.koadernoa.app.objektuak.modulua.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.entitateak.MatrikulaEgoera;

public interface MatrikulaRepository extends JpaRepository<Matrikula, Long> {
	boolean existsByIkasleaIdAndKoadernoaId(Long ikasleaId, Long koadernoaId);
	
	@Query("""
			  select m from Matrikula m
			  join fetch m.ikaslea i
			  where m.koadernoa.id = :koadernoId
			  order by i.abizena1 asc, i.abizena2 asc, i.izena asc
			""")
	List<Matrikula> findAllByKoadernoaFetchIkasleaOrderByIzena(Long koadernoId);
	
	
	List<Matrikula> findByKoadernoaIdAndEgoera(Long koadernoaId, MatrikulaEgoera egoera);
	
	default List<Matrikula> findByKoadernoaIdAndEgoeraMatrikulatuta(Long koadernoaId){
        return findByKoadernoaIdAndEgoera(koadernoaId, MatrikulaEgoera.MATRIKULATUA);
    }
	
	@Query("""
	    select m.id
	    from Matrikula m
	    where m.koadernoa.id in :koadernoIds
	      and (:keepAll = true or m.ikaslea.hna not in :hnasExcel)
	  """)
	  List<Long> findIdsToDelete(List<Long> koadernoIds, boolean keepAll, List<String> hnasExcel);

	  
	// Taldea excel bidez inportatzean, baldin eta ikasle bat kendu bada
	@Query("""
	        select m from Matrikula m
	        where m.koadernoa.id = :koadernoaId
	          and m.ikaslea.hna is not null
	          and m.ikaslea.hna not in :hnasExcel
	    """)
	    List<Matrikula> findToRemoveByKoadernoAndNotInHnas(@Param("koadernoaId") Long koadernoaId,
	                                                       @Param("hnasExcel") List<String> hnasExcel);
    
}
