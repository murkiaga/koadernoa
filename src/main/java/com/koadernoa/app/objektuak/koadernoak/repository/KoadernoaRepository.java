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
    
    @Query("select k.id from Koadernoa k where k.moduloa.taldea.id = :taldeaId")
    List<Long> findKoadernoIdsByTaldeaId(@Param("taldeaId") Long taldeaId);
    
    //Programazioa inportatzerako
    List<Koadernoa> findByModuloa_EeiKodeaAndIdNot(String eeiKodea, Long excludeId);
}
