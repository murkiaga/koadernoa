package com.koadernoa.app.objektuak.koadernoak.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Programazioa;

public interface ProgramazioaRepository extends JpaRepository<Programazioa, Long> {
	Optional<Programazioa> findByKoadernoa(Koadernoa koadernoa);
    Optional<Programazioa> findByKoadernoaId(Long koadernoId);
    
    // 1️⃣ Programazioa + Ebaluaketak
    @Query("""
        select distinct p
        from Programazioa p
        left join fetch p.ebaluaketak e
        where p.koadernoa.id = :koadernoId
    """)
    Optional<Programazioa> findByKoadernoaIdFetchEbaluaketak(@Param("koadernoId") Long koadernoId);


    // 2️⃣ Programazioa + Ebaluaketak + UD-ak
    @Query("""
        select distinct p
        from Programazioa p
        left join fetch p.ebaluaketak e
        left join fetch e.unitateak u
        where p.koadernoa.id = :koadernoId
    """)
    Optional<Programazioa> findByKoadernoaIdFetchEbaluaketakUd(@Param("koadernoId") Long koadernoId);


    // 3️⃣ Programazioa + Ebaluaketak + UD-ak + Jarduerak
    // Oharra: multiple bag fetch saihesteko, egitura hau erabil daiteke soilik kasu txikietan
    // edo hibernate.default_batch_fetch_size erabiliz.
    @Query("""
	      select distinct p from Programazioa p
	        left join fetch p.ebaluaketak e
	        left join fetch e.unitateak u
	        left join fetch u.azpiJarduerak jp
	      where p.koadernoa.id = :koadernoId
	      order by e.ordena asc, u.posizioa asc, u.id asc, jp.posizioa asc, jp.id asc
	  """)
	  Optional<Programazioa> findByKoadernoaIdFetchEbaluaketakUdetajp(@Param("koadernoId") Long koadernoId);

    void deleteByKoadernoaId(Long koadernoId);
}
