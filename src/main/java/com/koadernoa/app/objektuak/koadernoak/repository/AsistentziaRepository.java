package com.koadernoa.app.objektuak.koadernoak.repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Asistentzia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Asistentzia.AsistentziaEgoera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;


public interface AsistentziaRepository extends JpaRepository<Asistentzia, Long> {
	  List<Asistentzia> findBySaioaId(Long saioaId);
	  Optional<Asistentzia> findBySaioaIdAndMatrikulaId(Long saioaId, Long matrikulaId);
	  void deleteBySaioaIdAndMatrikulaId(Long saioaId, Long matrikulaId);
	  
	@Modifying
	@Query("""
	    delete from Asistentzia a
	    where a.matrikula.id in :matrikulaIds
	  """)
	  void deleteByMatrikulaIdIn(@Param("matrikulaIds") List<Long> ids);

	@Modifying
	@Query("""
	    delete from Asistentzia a
	    where a.saioa.koadernoa.id = :koadernoaId
	      and a.saioa.data in :datak
	""")
	void deleteByKoadernoaIdAndSaioaDataIn(@Param("koadernoaId") Long koadernoaId,
	                                       @Param("datak") Set<LocalDate> datak);
	  
	  // Estatistiketarako: saio multzo bateko HUTS egoerako asistentziak
	  List<Asistentzia> findBySaioa_IdInAndEgoeraIn(
		        List<Long> saioaIds,
		        List<AsistentziaEgoera> egoerak
		);
	  
	// Hilabete jakin bateko saio + matrikulen gurutzaketa
    List<Asistentzia> findBySaioaInAndMatrikulaIn(
            List<Saioa> saioak,
            List<Matrikula> matrikulak
    );
	  
	  void deleteBySaioa_IdIn(List<Long> saioaIds);
}
