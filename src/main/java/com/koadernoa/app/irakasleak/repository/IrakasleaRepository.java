package com.koadernoa.app.irakasleak.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.irakasleak.entitateak.Irakaslea;

public interface IrakasleaRepository extends JpaRepository<Irakaslea, Long> {
	
	Optional<Irakaslea> findByIzena(String izena);
	
	Optional<Irakaslea> findByEmaila(String emaila);
	
}
