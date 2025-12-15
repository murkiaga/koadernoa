package com.koadernoa.app.objektuak.koadernoak.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoOrdutegiBlokea;

public interface KoadernoOrdutegiBlokeaRepository extends JpaRepository<KoadernoOrdutegiBlokea, Long>{
	List<KoadernoOrdutegiBlokea> findByKoadernoaIdAndAsteguna(Long koadernoaId, Astegunak asteguna);
	
	@Query("select b.asteguna from KoadernoOrdutegiBlokea b where b.koadernoa.id = :koadernoaId")
	List<Astegunak> findAstegunakByKoadernoaId(@Param("koadernoaId") Long koadernoaId);
	
	void deleteByKoadernoa_Id(Long koadernoId);
	
	// Falten bistako kalkuluetarako:
    List<KoadernoOrdutegiBlokea> findByKoadernoa_Id(Long koadernoaId);
}
