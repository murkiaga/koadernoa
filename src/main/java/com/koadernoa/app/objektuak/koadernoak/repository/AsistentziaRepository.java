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
	  List<Asistentzia> findBySaioa_IdIn(List<Long> saioaIds);
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

	@Query("""
	    select a
	    from Asistentzia a
	      join fetch a.saioa s
	      join fetch a.matrikula m
	      left join fetch m.ikaslea i
	    where s.koadernoa.id = :koadernoaId
	      and s.data between :hasiera and :bukaera
	      and a.egoera in :egoerak
	    order by s.data asc, s.hasieraSlot asc, i.abizena1 asc, i.abizena2 asc, i.izena asc
	""")
	List<Asistentzia> findFaltakEbaluazioan(@Param("koadernoaId") Long koadernoaId,
	                                        @Param("hasiera") LocalDate hasiera,
	                                        @Param("bukaera") LocalDate bukaera,
	                                        @Param("egoerak") List<AsistentziaEgoera> egoerak);

	@Query("""
	    select a
	    from Asistentzia a
	      join fetch a.saioa s
	      join fetch s.koadernoa k
	      join fetch k.moduloa mod
	      join fetch a.matrikula matr
	      join fetch matr.ikaslea i
	    where mod.taldea.id = :taldeaId
	      and k.egutegia.ikasturtea.id = :ikasturteaId
	      and s.data between :hasiera and :bukaera
	      and a.egoera in :egoerak
	    order by s.data asc, s.hasieraSlot asc, mod.kodea asc
	""")
	List<Asistentzia> findEthaziFaltak(@Param("taldeaId") Long taldeaId,
	                                  @Param("ikasturteaId") Long ikasturteaId,
	                                  @Param("hasiera") LocalDate hasiera,
	                                  @Param("bukaera") LocalDate bukaera,
	                                  @Param("egoerak") List<AsistentziaEgoera> egoerak);
	  
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
