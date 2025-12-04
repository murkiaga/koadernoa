package com.koadernoa.app.objektuak.koadernoak.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa.SaioEgoera;

public interface SaioaRepository extends JpaRepository<Saioa, Long>{

	List<Saioa> findByKoadernoaIdAndData(Long koadernoaId, LocalDate data);
	boolean existsByKoadernoaIdAndDataAndHasieraSlot(Long koadernoaId, LocalDate data, int hasieraSlot);
	
	// Estatistiketarako erabiliko duguna:
    List<Saioa> findByKoadernoaIdAndDataBetweenOrderByDataAscHasieraSlotAsc(
            Long koadernoaId,
            LocalDate from,
            LocalDate to
    );
    
    // Estatistiketarako: koaderno + tarte jakin bateko saio aktiboak
    List<Saioa> findByKoadernoa_IdAndDataBetweenAndEgoera(
            Long koadernoaId,
            LocalDate from,
            LocalDate to,
            SaioEgoera egoera
    );
}
