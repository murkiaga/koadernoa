package com.koadernoa.app.ethazi.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.koadernoa.app.ethazi.entitateak.ErronkaTaldeKidea;

public interface ErronkaTaldeKideaRepository extends JpaRepository<ErronkaTaldeKidea, Long> {
    List<ErronkaTaldeKidea> findByErronkaId(Long erronkaId);
    void deleteByErronkaId(Long erronkaId);
}
