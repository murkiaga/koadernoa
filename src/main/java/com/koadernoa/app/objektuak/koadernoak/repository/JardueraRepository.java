package com.koadernoa.app.objektuak.koadernoak.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Jarduera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;

public interface JardueraRepository extends JpaRepository<Jarduera, Long>{
	
	List<Jarduera> findByKoadernoaIdAndDataBetweenOrderByDataAscIdAsc(
	        Long koadernoaId, LocalDate start, LocalDate end);

    List<Jarduera> findByKoadernoaIdOrderByDataAscIdAsc(Long koadernoaId);

    // Editatzeko erabiliko duguna
	Jarduera findByIdAndKoadernoaId(Long id, Long koadernoaId);
	
	long deleteByIdAndKoadernoaId(Long id, Long koadernoId);
	
    @Modifying
    @Query("update Jarduera j set j.unitatea = null where j.unitatea.id = :unitateaId")
    int clearUnitateaByUnitateaId(@Param("unitateaId") Long unitateaId);

	@Modifying // DELETE da; @Transactional zerbitzuan baduzu, nahikoa da
	void deleteByKoadernoaAndDataBetweenAndMota(Koadernoa koadernoa, LocalDate from, LocalDate to, String mota);

	// (aukerakoa, erabilgarria izan daiteke aurreikuspenetarako)
	List<Jarduera> findByKoadernoaAndMotaAndDataBetween(Koadernoa koadernoa, String mota, LocalDate from, LocalDate to);
	
    void deleteByKoadernoa_Id(Long koadernoId);
    
    
}
