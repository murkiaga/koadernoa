package com.koadernoa.app.objektuak.jokabidea.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.koadernoa.app.objektuak.jokabidea.entitateak.NeurriZuzentzailea;
public interface NeurriZuzentzaileaRepository extends JpaRepository<NeurriZuzentzailea, Long> {
    List<NeurriZuzentzailea> findAllByOrderByOrdenaAscIdAsc();
    List<NeurriZuzentzailea> findByAktiboTrueOrderByOrdenaAscIdAsc();
    Optional<NeurriZuzentzailea> findFirstByDefektuzkoaTrueAndAktiboTrueOrderByOrdenaAscIdAsc();
}
