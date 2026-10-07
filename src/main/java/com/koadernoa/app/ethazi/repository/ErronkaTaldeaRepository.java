package com.koadernoa.app.ethazi.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.koadernoa.app.ethazi.entitateak.ErronkaTaldea;

public interface ErronkaTaldeaRepository extends JpaRepository<ErronkaTaldea, Long> {
    List<ErronkaTaldea> findByErronkaIdOrderByOrdenaAsc(Long erronkaId);
    void deleteByErronkaId(Long erronkaId);
}
