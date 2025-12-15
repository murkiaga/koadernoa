package com.koadernoa.app.objektuak.koadernoak.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.koadernoak.entitateak.EstatistikaEbaluazioan;

public interface EstatistikaEbaluazioanRepository extends JpaRepository<EstatistikaEbaluazioan, Long> {

	List<EstatistikaEbaluazioan> findByKoadernoaIdOrderByEbaluazioMomentua_OrdenaAscIdAsc(Long koadernoId);
	
	void deleteByKoadernoa_Id(Long koadernoId);
	
}
