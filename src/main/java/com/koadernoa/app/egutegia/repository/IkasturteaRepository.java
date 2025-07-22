package com.koadernoa.app.egutegia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.egutegia.entitateak.Ikasturtea;

public interface IkasturteaRepository extends JpaRepository<Ikasturtea, Long>{

	List<Ikasturtea> findByAktiboaTrue();
	List<Ikasturtea> findAllByOrderByIzenaDesc();
}
