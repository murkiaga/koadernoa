package com.koadernoa.app.irakasleak.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.zikloak.entitateak.Familia;

public interface IrakasleaRepository extends JpaRepository<Irakaslea, Long> {
	
	Optional<Irakaslea> findByIzena(String izena);
	
	Optional<Irakaslea> findByEmaila(String emaila);
	
	List<Irakaslea> findByMintegia(Familia familia);
}
