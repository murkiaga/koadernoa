package com.koadernoa.app.modulua.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.modulua.entitateak.Moduloa;

public interface ModuloaRepository extends JpaRepository<Moduloa, Long>{

	List<Moduloa> findByTaldeaId(Long taldeaId);
}
