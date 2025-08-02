package com.koadernoa.app.egutegia.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.egutegia.entitateak.Egutegia;
import com.koadernoa.app.egutegia.entitateak.Maila;

public interface EgutegiaRepository extends JpaRepository<Egutegia, Long>{

	Optional<Egutegia> findByIkasturtea_AktiboaTrueAndMaila(Maila maila);
}
