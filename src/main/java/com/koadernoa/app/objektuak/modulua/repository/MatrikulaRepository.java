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
	
	// Estatistiketarako: koaderno honetako MATRIKULATUA dauden matrikulak lortzeko
	List<Matrikula> findByKoadernoa_IdAndEgoera(Long koadernoaId, MatrikulaEgoera egoera);
	
	// Estatistiketarako: KOADERNO HONETAKO matrikulatuak bakarrik
	long countByKoadernoa_IdAndEgoera(Long koadernoaId, MatrikulaEgoera egoera);
	
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
    
	List<Matrikula> findByKoadernoa_Id(Long koadernoId);


	List<Matrikula> findByIkasleaIdAndKoadernoaIdIn(Long ikasleaId, List<Long> koadernoIds);

	@Query("""
	    select m from Matrikula m
	    where m.ikaslea.id = :ikasleaId
	      and m.koadernoa.egutegia.ikasturtea.id = :ikasturteaId
	      and m.koadernoa.moduloa.eeiKodea = :eeiKodea
	      and m.koadernoa.id <> :koadernoId
	""")
	List<Matrikula> findByIkasleaAndIkasturteaAndEeiKodeDifferentKoaderno(@Param("ikasleaId") Long ikasleaId,
	                                                                     @Param("ikasturteaId") Long ikasturteaId,
	                                                                     @Param("eeiKodea") String eeiKodea,
	                                                                     @Param("koadernoId") Long koadernoId);



	@Query("""
	    select distinct m.koadernoa.moduloa.izena
	    from Matrikula m
	    where m.ikaslea.id = :ikasleaId
	      and m.koadernoa.egutegia.ikasturtea.id = :ikasturteaId
	      and m.koadernoa.moduloa.eeiKodea = :eeiKodea
	      and m.koadernoa.id <> :koadernoId
	""")
	List<String> findConflictModuloIzenakByIkasleaAndIkasturteaAndEeiKodeDifferentKoaderno(@Param("ikasleaId") Long ikasleaId,
	                                                                                         @Param("ikasturteaId") Long ikasturteaId,
	                                                                                         @Param("eeiKodea") String eeiKodea,
	                                                                                         @Param("koadernoId") Long koadernoId);
	@Query("""
	    select i from Ikaslea i
	    where lower(concat(coalesce(i.abizena1, ''), ' ', coalesce(i.abizena2, ''), ' ', coalesce(i.izena, ''))) like lower(concat('%', :term, '%'))
	      and i.id not in (
	          select m.ikaslea.id from Matrikula m
	          where m.koadernoa.id = :koadernoId
	      )
	    order by i.abizena1 asc, i.abizena2 asc, i.izena asc
	""")
	List<com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea> bilatuIkasleMatrikulatuGabeakKoadernoan(@Param("koadernoId") Long koadernoId,
	                                                                                                      @Param("term") String term);
	

    @Query("""
        select m from Matrikula m
        join fetch m.ikaslea i
        join fetch m.koadernoa k
        join fetch k.moduloa mo
        join fetch k.egutegia e
        join fetch e.ikasturtea ik
        where i.id = :ikasleaId
          and (:ikasturteaId is null or ik.id = :ikasturteaId)
        order by ik.izena desc, mo.izena asc
    """)
    List<Matrikula> findIkaslearenMatrikulakByIkasturtea(@Param("ikasleaId") Long ikasleaId,
                                                         @Param("ikasturteaId") Long ikasturteaId);

    @Query("""
        select distinct ik from Matrikula m
        join m.koadernoa k
        join k.egutegia e
        join e.ikasturtea ik
        where m.ikaslea.id = :ikasleaId
        order by ik.izena desc
    """)
    List<com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea> findIkasturteakByIkaslea(@Param("ikasleaId") Long ikasleaId);

	void deleteByKoadernoa_Id(Long koadernoId);
}
