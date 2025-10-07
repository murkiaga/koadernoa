package com.koadernoa.app.objektuak.modulua.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;

public interface ModuloaRepository extends JpaRepository<Moduloa, Long>{

	List<Moduloa> findByTaldeaId(Long taldeaId);
	List<Moduloa> findByMaila_Id(Long mailaId);
	List<Moduloa> findByTaldea_Zikloa_Familia(Familia familia);
}
