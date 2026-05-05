package com.koadernoa.app.objektuak.modulua.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;

public interface ModuloaRepository extends JpaRepository<Moduloa, Long>{

	List<Moduloa> findByTaldeaId(Long taldeaId);
	Page<Moduloa> findByTaldeaId(Long taldeaId, Pageable pageable);
	Page<Moduloa> findAll(Pageable pageable);
	List<Moduloa> findByMaila_Id(Long mailaId);
	List<Moduloa> findByTaldea_Zikloa_Familia(Familia familia);
	
	List<Moduloa> findAllByTaldea_Zikloa_Familia_Id(Long familiaId);

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
