package com.koadernoa.app.objektuak.egutegia.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;

public interface EgutegiaRepository extends JpaRepository<Egutegia, Long>{
	
	Optional<Egutegia> findByIkasturtea_AktiboaTrueAndMaila_Id(Long mailaId);
}
