package com.koadernoa.app.egutegia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.egutegia.entitateak.Maila;

public interface IkasturteaRepository extends JpaRepository<Ikasturtea, Long>{

	List<Ikasturtea> findByAktiboaTrue();
	List<Ikasturtea> findByAktiboaTrueAndMaila(Maila maila);
}
