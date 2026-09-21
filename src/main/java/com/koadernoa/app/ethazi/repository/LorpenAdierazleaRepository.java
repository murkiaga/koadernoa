package com.koadernoa.app.ethazi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.*;

public interface LorpenAdierazleaRepository extends JpaRepository<LorpenAdierazlea, Long> {
    boolean existsByIkaskuntzaEmaitzakId(Long id);
}
