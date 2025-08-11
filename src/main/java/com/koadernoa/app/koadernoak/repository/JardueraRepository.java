package com.koadernoa.app.koadernoak.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.koadernoak.entitateak.Jarduera;
import com.koadernoa.app.koadernoak.entitateak.Koadernoa;

public interface JardueraRepository extends JpaRepository<Jarduera, Long>{
	
	List<Jarduera> findByKoadernoaAndDataBetweenOrderByDataAscIdAsc(
            Koadernoa koadernoa,
            LocalDate start,
            LocalDate end
    );

    // Editatzeko erabiliko duguna
    Jarduera findByIdAndKoadernoa(Long id, Koadernoa koadernoa);
}
