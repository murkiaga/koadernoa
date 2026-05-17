package com.koadernoa.app.objektuak.modulua.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;

public interface ModuloaRepository extends JpaRepository<Moduloa, Long>{


	Optional<Moduloa> findByKodeaIgnoreCaseAndEeiKodeaIgnoreCaseAndTaldeaAndMaila(String kodea, String eeiKodea, Taldea taldea, Maila maila);

	List<Moduloa> findByTaldeaId(Long taldeaId);
	Page<Moduloa> findByTaldeaId(Long taldeaId, Pageable pageable);
	Page<Moduloa> findAll(Pageable pageable);
	List<Moduloa> findByMaila_Id(Long mailaId);
	List<Moduloa> findByTaldea_Zikloa_Familia(Familia familia);
	
	List<Moduloa> findAllByTaldea_Zikloa_Familia_Id(Long familiaId);

	@Query("""
		select m from Moduloa m
		left join m.taldea t
		left join t.zikloa z
		where (:taldeaId is null or t.id = :taldeaId)
		  and (:zikloaId is null or z.id = :zikloaId)
		  and (:hautazkoa is null or m.hautazkoa = :hautazkoa)
	""")
	Page<Moduloa> bilatuFiltroekin(@Param("taldeaId") Long taldeaId,
	                                @Param("zikloaId") Long zikloaId,
	                                @Param("hautazkoa") Boolean hautazkoa,
	                                Pageable pageable);

	@Query("""
		select distinct z from Moduloa m
		join m.taldea t
		join t.zikloa z
		where (:familiaId is null or z.familia.id = :familiaId)
		   or upper(m.eeiKodea) in :eeiKodeak
		order by z.izena asc
	""")
	List<Zikloa> findDistinctZikloakByFamiliaOrEeiKodeak(@Param("familiaId") Long familiaId,
	                                                    @Param("eeiKodeak") List<String> eeiKodeak);

	@Query("""
		select m from Moduloa m
		where ((:familiaId is null or m.taldea.zikloa.familia.id = :familiaId)
		        or upper(m.eeiKodea) in :eeiKodeak)
		  and (:zikloaId is null or m.taldea.zikloa.id = :zikloaId)
		  and (:mailaId is null or m.maila.id = :mailaId)
		order by m.izena asc
	""")
	List<Moduloa> bilatuFamiliaEdoEeiKodeekin(@Param("familiaId") Long familiaId,
	                                         @Param("eeiKodeak") List<String> eeiKodeak,
	                                         @Param("zikloaId") Long zikloaId,
	                                         @Param("mailaId") Long mailaId);

	@Query("""
		select distinct ma from Moduloa m
		join m.maila ma
		where (:familiaId is null or m.taldea.zikloa.familia.id = :familiaId)
		  and (:zikloaId is null or m.taldea.zikloa.id = :zikloaId)
		  and ma.aktibo = true
		order by ma.ordena asc, ma.izena asc
	""")
	List<Maila> findDistinctMailakByFamiliaAndZikloa(@Param("familiaId") Long familiaId,
	                                                @Param("zikloaId") Long zikloaId);

	@Query("""
		select distinct ma from Moduloa m
		join m.maila ma
		where ((:familiaId is null or m.taldea.zikloa.familia.id = :familiaId)
		        or upper(m.eeiKodea) in :eeiKodeak)
		  and (:zikloaId is null or m.taldea.zikloa.id = :zikloaId)
		  and ma.aktibo = true
		order by ma.ordena asc, ma.izena asc
	""")
	List<Maila> findDistinctMailakByFamiliaOrEeiKodeakAndZikloa(@Param("familiaId") Long familiaId,
	                                                           @Param("eeiKodeak") List<String> eeiKodeak,
	                                                           @Param("zikloaId") Long zikloaId);

	@Query("""
		select m from Moduloa m
		where (:familiaId is null or m.taldea.zikloa.familia.id = :familiaId)
		  and (:zikloaId is null or m.taldea.zikloa.id = :zikloaId)
		  and (:mailaId is null or m.maila.id = :mailaId)
		order by m.izena asc
	""")
	List<Moduloa> bilatuFiltroekin(@Param("familiaId") Long familiaId,
	                              @Param("zikloaId") Long zikloaId,
	                              @Param("mailaId") Long mailaId);
}
