package com.koadernoa.app.objektuak.koadernoak.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Ebaluaketa;

public interface EbaluaketaRepository extends JpaRepository<Ebaluaketa, Long>{

	@Query("""
	      select distinct e
	      from Ebaluaketa e
	        left join fetch e.unitateak u
	      where e.programazioa.id = :programazioId
	      order by e.ordena asc, e.id asc
	  """)
	  List<Ebaluaketa> findAllWithUdByProgramazioaId(@Param("programazioId") Long programazioId);

	  boolean existsByIdAndProgramazioa_Koadernoa_Id(Long id, Long koadernoId);
}
