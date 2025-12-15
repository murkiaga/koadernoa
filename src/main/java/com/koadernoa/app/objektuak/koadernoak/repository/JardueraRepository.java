package com.koadernoa.app.objektuak.koadernoak.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Jarduera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;

public interface JardueraRepository extends JpaRepository<Jarduera, Long>{
	
	List<Jarduera> findByKoadernoaIdAndDataBetweenOrderByDataAscIdAsc(
	        Long koadernoaId, LocalDate start, LocalDate end);

    // Editatzeko erabiliko duguna
	Jarduera findByIdAndKoadernoaId(Long id, Long koadernoaId);
	
	long deleteByIdAndKoadernoaId(Long id, Long koadernoId);
	
	@Modifying // DELETE da; @Transactional zerbitzuan baduzu, nahikoa da
	void deleteByKoadernoaAndDataBetweenAndMota(Koadernoa koadernoa, LocalDate from, LocalDate to, String mota);

	// (aukerakoa, erabilgarria izan daiteke aurreikuspenetarako)
	List<Jarduera> findByKoadernoaAndMotaAndDataBetween(Koadernoa koadernoa, String mota, LocalDate from, LocalDate to);
	
    void deleteByKoadernoa_Id(Long koadernoId);
    
    
}
