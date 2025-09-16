package com.koadernoa.app.egutegia.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.egutegia.entitateak.Maila;

public interface MailaRepository extends JpaRepository<Maila, Long>{
	List<Maila> findAllByAktiboTrueOrderByOrdenaAscIzenaAsc();
    Optional<Maila> findByKodea(String kodea);
  
}
