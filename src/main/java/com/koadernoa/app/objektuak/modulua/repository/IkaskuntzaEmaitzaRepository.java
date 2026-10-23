package com.koadernoa.app.objektuak.modulua.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.koadernoa.app.objektuak.modulua.entitateak.IkaskuntzaEmaitza;

public interface IkaskuntzaEmaitzaRepository extends JpaRepository<IkaskuntzaEmaitza, Long> {
    List<IkaskuntzaEmaitza> findByEeiKodeaOrderByOrdenaAsc(String eeiKodea);
    Optional<IkaskuntzaEmaitza> findByEeiKodeaAndOrdena(String eeiKodea, Integer ordena);

    @Query("""
            select ie.eeiKodea, count(ie)
            from IkaskuntzaEmaitza ie
            where ie.eeiKodea in :eeiKodeak
            group by ie.eeiKodea
            """)
    List<Object[]> countByEeiKodeaIn(@Param("eeiKodeak") Collection<String> eeiKodeak);
}
