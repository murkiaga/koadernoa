package com.koadernoa.app.objektuak.zikloak.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;

public interface TaldeaRepository extends JpaRepository<Taldea, Long> {
	Optional<Taldea> findByTutorea(Irakaslea tutorea);
    Optional<Taldea> findByTutorea_Id(Long irakasleId);
    boolean existsByTutorea_Id(Long irakasleId);
    
    //zikloaren arabera taldeak
    List<Taldea> findByZikloa_Id(Long zikloaId);
    List<Taldea> findByZikloa_IdOrderByIzenaAsc(Long zikloaId);
    
    Optional<Taldea> findByIzenaIgnoreCaseAndZikloa(String izena, Zikloa zikloa);
    
    List<Taldea> findAllByOrderByIzenaAsc();

    List<Taldea> findByZikloa_Familia_IdOrderByIzenaAsc(Long familiaId);

    @Query("""
            select t
            from Taldea t
            left join t.zikloa z
            where lower(coalesce(t.izena, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(z.izena, '')) like lower(concat('%', :q, '%'))
               or lower(cast(z.maila as string)) like lower(concat('%', :q, '%'))
            order by t.izena asc
            """)
    List<Taldea> bilatuAutocomplete(@Param("q") String q, Pageable pageable);
}
