package com.koadernoa.app.ethazi.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.ethazi.entitateak.kronograma.ErronkaKronogramaJarduera;

public interface ErronkaKronogramaJardueraRepository extends JpaRepository<ErronkaKronogramaJarduera, Long> {
    @EntityGraph(attributePaths = {"moduloa", "aurreikusitakoModuloa"})
    List<ErronkaKronogramaJarduera> findByErronkaIdOrderByHasieraDataAscBukaeraDataAscIdAsc(Long erronkaId);
}
