package com.koadernoa.app.ethazi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.*;

public interface LorpenAdierazleaRepository extends JpaRepository<LorpenAdierazlea, Long> {
    java.util.List<LorpenAdierazlea> findByErronkakId(Long id);
    java.util.List<LorpenAdierazlea> findDistinctByKinielaLoturakErronkakId(Long id);
    boolean existsByIkaskuntzaEmaitzakId(Long id);
}
