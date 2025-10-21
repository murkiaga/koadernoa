package com.koadernoa.app.objektuak.koadernoak.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.objektuak.koadernoak.entitateak.UnitateDidaktikoa;

public interface UnitateDidaktikoaRepository extends JpaRepository<UnitateDidaktikoa, Long> {
	List<UnitateDidaktikoa> findByEbaluaketaIdOrderByPosizioaAscIdAsc(Long ebaluaketaId);
	boolean existsByIdAndEbaluaketa_Programazioa_Koadernoa_Id(Long udId, Long koadernoId);

    
	@Query("""
		    select distinct u from UnitateDidaktikoa u
		      left join fetch u.azpiJarduerak aj
		    where u.ebaluaketa.id = :ebaluaketaId
		    order by u.posizioa asc, u.id asc
		""")
		List<UnitateDidaktikoa> findAllByEbaluaketaIdFetchAzpi(@Param("ebaluaketaId") Long ebaluaketaId);

    
    @Query("""
	    select distinct u
	    from UnitateDidaktikoa u
	    left join fetch u.azpiJarduerak jp
	    where u.id in :udIds
	    order by u.id asc
	  """)
	  List<UnitateDidaktikoa> fetchUdWithJpByIds(Collection<Long> udIds);
}
