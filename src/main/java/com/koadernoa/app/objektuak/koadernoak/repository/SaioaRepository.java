package com.koadernoa.app.objektuak.koadernoak.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa;

public interface SaioaRepository extends JpaRepository<Saioa, Long>{

	List<Saioa> findByKoadernoaIdAndData(Long koadernoaId, LocalDate data);
	boolean existsByKoadernoaIdAndDataAndHasieraSlot(Long koadernoaId, LocalDate data, int hasieraSlot);
}
