package com.koadernoa.app.objektuak.modulua.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.IkaskuntzaEmaitza;

public interface IkaskuntzaEmaitzaRepository extends JpaRepository<IkaskuntzaEmaitza, Long> {
    List<IkaskuntzaEmaitza> findByEeiKodeaOrderByOrdenaAsc(String eeiKodea);
    Optional<IkaskuntzaEmaitza> findByEeiKodeaAndOrdena(String eeiKodea, Integer ordena);
}
