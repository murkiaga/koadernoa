package com.koadernoa.app.objektuak.modulua.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.IkaskuntzaEmaitza;

public interface IkaskuntzaEmaitzaRepository extends JpaRepository<IkaskuntzaEmaitza, Long> {
    List<IkaskuntzaEmaitza> findByModuloaIdOrderByOrdenaAsc(Long moduloaId);
}
