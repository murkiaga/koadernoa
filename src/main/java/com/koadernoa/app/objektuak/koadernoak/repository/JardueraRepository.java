package com.koadernoa.app.objektuak.koadernoak.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Jarduera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;

public interface JardueraRepository extends JpaRepository<Jarduera, Long>{
	
	List<Jarduera> findByKoadernoaIdAndDataBetweenOrderByDataAscIdAsc(
	        Long koadernoaId, LocalDate start, LocalDate end);

    // Editatzeko erabiliko duguna
	Jarduera findByIdAndKoadernoaId(Long id, Long koadernoaId);
	
	long deleteByIdAndKoadernoaId(Long id, Long koadernoId);
}
