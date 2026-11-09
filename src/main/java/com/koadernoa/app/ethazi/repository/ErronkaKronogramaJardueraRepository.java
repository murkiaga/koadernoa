package com.koadernoa.app.ethazi.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.ethazi.entitateak.kronograma.ErronkaKronogramaJarduera;

public interface ErronkaKronogramaJardueraRepository extends JpaRepository<ErronkaKronogramaJarduera, Long> {
    @EntityGraph(attributePaths = {"moduloa", "aurreikusitakoModuloa", "multzoa"})
    List<ErronkaKronogramaJarduera> findByErronkaIdOrderByHasieraDataAscBukaeraDataAscIdAsc(Long erronkaId);

    @EntityGraph(attributePaths = {"erronka", "moduloa", "aurreikusitakoModuloa", "multzoa"})
    List<ErronkaKronogramaJarduera> findByErronkaIdInOrderByHasieraDataAscBukaeraDataAscIdAsc(
            List<Long> erronkaIds);

    List<ErronkaKronogramaJarduera> findByMultzoaId(Long multzoaId);
}
