package com.koadernoa.app.modulua.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.egutegia.entitateak.Maila;
import com.koadernoa.app.modulua.entitateak.Moduloa;
import com.koadernoa.app.zikloak.entitateak.Familia;

public interface ModuloaRepository extends JpaRepository<Moduloa, Long>{

	List<Moduloa> findByTaldeaId(Long taldeaId);
	List<Moduloa> findByMaila(Maila maila);
	List<Moduloa> findByTaldea_Zikloa_Familia(Familia familia);
}
