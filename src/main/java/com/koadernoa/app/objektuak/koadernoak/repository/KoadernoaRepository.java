package com.koadernoa.app.objektuak.koadernoak.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;

public interface KoadernoaRepository extends JpaRepository<Koadernoa, Long>{

	List<Koadernoa> findByIrakasleakContaining(Irakaslea irakaslea);
	
	//Tutore entitatearen arabera. Tutorearen taldeko koadernoak:
    List<Koadernoa> findByModuloa_Taldea_Tutorea(Irakaslea tutorea);
    //Tutorearen IDaren arabera
    List<Koadernoa> findByModuloa_Taldea_Tutorea_Id(Long tutoreId);
    
    boolean existsByIdAndIrakasleak_Id(Long koadernoId, Long irakasleId);
    
    List<Koadernoa> findAllByIrakasleak_Id(Long irakasleId);
    
    List<Koadernoa> findByModuloa_Taldea_Id(Long taldeaId);
    
    // Talde bateko koadernoak ikasturte aktiboan (Egutegia → Ikasturtea.aktibo = true)
    @Query("""
	      select k.id
	      from Koadernoa k
	      where k.moduloa.taldea.id = :taldeaId
	        and k.egutegia.ikasturtea.aktiboa = true
	    """)
	    List<Long> findActiveYearKoadernoIdsByTaldea(@Param("taldeaId") Long taldeaId);
    
    @EntityGraph(attributePaths = {
            "ordutegiak",
            "egutegia",
            "egutegia.ikasturtea",
            "moduloa"
        })
        Optional<Koadernoa> findWithOrdutegiaById(Long id);
    
	@Query("""
		  select k from Koadernoa k
		    left join fetch k.ordutegiak
		    left join fetch k.egutegia eg
		  where k.id = :id
		""")
		Optional<Koadernoa> findByIdWithOrdutegiaAndEgutegia(@Param("id") Long id);

	@Query("""
		  select distinct k from Koadernoa k
		    left join fetch k.egutegia eg
		    left join fetch eg.egunBereziak
		    left join fetch eg.ikasturtea
		  where k.id = :id
		""")
		Optional<Koadernoa> findByIdWithEgutegiaAndEgunBereziak(@Param("id") Long id);
    
    @Query("select k.id from Koadernoa k where k.moduloa.taldea.id = :taldeaId")
    List<Long> findKoadernoIdsByTaldeaId(@Param("taldeaId") Long taldeaId);

    @Query("""
          select distinct k.moduloa.id
          from Koadernoa k
          where k.egutegia.ikasturtea.aktiboa = true
        """)
    List<Long> findModuloIdsInAktiboIkasturtea();
    
    //Programazioa inportatzerako
    List<Koadernoa> findByModuloa_EeiKodeaAndIdNot(String eeiKodea, Long excludeId);

    boolean existsByEgutegia_Id(Long egutegiaId);

    boolean existsByModuloa_Id(Long moduloaId);

    boolean existsByModuloa_Taldea_Id(Long taldeaId);

    boolean existsByModuloa_IdAndEgutegia_Id(Long moduloaId, Long egutegiaId);

    java.util.Optional<Koadernoa> findFirstByModuloa_IdAndEgutegia_IdOrderByIdAsc(Long moduloaId, Long egutegiaId);

    @Query("""
          select k from Koadernoa k
          where k.moduloa.id = :moduloaId
            and k.egutegia.ikasturtea.aktiboa = true
          order by k.id asc
        """)
    List<Koadernoa> findByModuloaIdInAktiboIkasturtea(@Param("moduloaId") Long moduloaId);
    
    
    @Query("""
          select distinct k from Koadernoa k
            left join fetch k.moduloa m
            left join fetch m.taldea t
            left join fetch k.egutegia e
            left join fetch e.ikasturtea ik
          where (:taldeaId is null or t.id = :taldeaId)
            and (:ikasturteaId is null or ik.id = :ikasturteaId)
          order by m.izena asc
        """)
    List<Koadernoa> findByTaldeaAndIkasturteaWithModuloa(@Param("taldeaId") Long taldeaId,
                                                          @Param("ikasturteaId") Long ikasturteaId);

    @Query("""
          select k from Koadernoa k
          where k.moduloa.id = :moduloaId
            and k.egutegia.ikasturtea.id = :ikasturteaId
          order by k.id asc
        """)
    List<Koadernoa> findByModuloaIdAndIkasturteaId(@Param("moduloaId") Long moduloaId,
                                                   @Param("ikasturteaId") Long ikasturteaId);


    boolean existsByJabeaIsNull();

    @Query("""
          select distinct k from Koadernoa k
            left join fetch k.moduloa m
            left join fetch m.taldea t
            left join fetch t.zikloa z
            left join fetch z.familia f
            left join fetch k.egutegia e
            left join fetch e.ikasturtea i
            left join fetch e.maila ma
            left join fetch k.irakasleak ir
          where k.jabea is null
          order by i.izena desc, f.izena, t.izena, m.izena
        """)
    List<Koadernoa> findJabeGabekoakWithRelations();

    @Query("""
          select distinct k from Koadernoa k
            left join fetch k.irakasleak ir
            left join fetch k.jabea j
          where k.id = :id
        """)
    Optional<Koadernoa> findByIdWithJabeaEtaIrakasleak(@Param("id") Long id);

    //Kudeatzaileak koadernoak kontsultatzeko
    @Query("""
    		  select distinct k
    		  from Koadernoa k
    		  left join fetch k.moduloa m
    		  left join fetch m.taldea t
    		  left join fetch t.zikloa z
    		  left join fetch z.familia f
    		  left join fetch k.egutegia e
    		  left join fetch e.ikasturtea i
    		  left join fetch k.irakasleak ir
              left join fetch k.jabea j
    		  order by f.izena, t.izena, m.izena
    		  """)
    		List<Koadernoa> findAllWithRelations();
}
