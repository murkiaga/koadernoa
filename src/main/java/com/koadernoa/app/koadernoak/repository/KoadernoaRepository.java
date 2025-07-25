package com.koadernoa.app.koadernoak.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.koadernoak.entitateak.Koadernoa;

public interface KoadernoaRepository extends JpaRepository<Koadernoa, Long>{

	Optional<Koadernoa> findById(Long id);

	List<Koadernoa> findByIrakasleakContaining(Irakaslea irakaslea);
}
