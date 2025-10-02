package com.koadernoa.app.modulua.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.modulua.entitateak.Matrikula;
import com.koadernoa.app.modulua.entitateak.MatrikulaEgoera;

public interface MatrikulaRepository extends JpaRepository<Matrikula, Long> {
	boolean existsByIkasleaIdAndKoadernoaId(Long ikasleaId, Long koadernoaId);
	
	@Query("""
			  select m from Matrikula m
			  join fetch m.ikaslea i
			  where m.koadernoa.id = :koadernoId
			  order by i.abizena1 asc, i.abizena2 asc, i.izena asc
			""")
	List<Matrikula> findAllByKoadernoaFetchIkasleaOrderByIzena(Long koadernoId);
}
