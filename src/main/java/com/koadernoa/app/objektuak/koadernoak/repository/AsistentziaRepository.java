package com.koadernoa.app.objektuak.koadernoak.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Asistentzia;


public interface AsistentziaRepository extends JpaRepository<Asistentzia, Long> {
	  List<Asistentzia> findBySaioaId(Long saioaId);
	  Optional<Asistentzia> findBySaioaIdAndMatrikulaId(Long saioaId, Long matrikulaId);
	  void deleteBySaioaIdAndMatrikulaId(Long saioaId, Long matrikulaId);
}
