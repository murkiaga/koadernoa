package com.koadernoa.app.objektuak.jokabidea.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.koadernoa.app.objektuak.jokabidea.entitateak.PortaeraArrazoia;
public interface PortaeraArrazoiaRepository extends JpaRepository<PortaeraArrazoia, Long> {
    List<PortaeraArrazoia> findAllByOrderByOrdenaAscIdAsc();
    List<PortaeraArrazoia> findByAktiboTrueOrderByOrdenaAscIdAsc();
    Optional<PortaeraArrazoia> findFirstByDefektuzkoaTrueAndAktiboTrueOrderByOrdenaAscIdAsc();
}
