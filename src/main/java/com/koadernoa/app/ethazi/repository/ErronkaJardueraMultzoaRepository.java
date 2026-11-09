package com.koadernoa.app.ethazi.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.ethazi.entitateak.kronograma.ErronkaJardueraMultzoa;

public interface ErronkaJardueraMultzoaRepository extends JpaRepository<ErronkaJardueraMultzoa, Long> {
    @EntityGraph(attributePaths = "erronka")
    List<ErronkaJardueraMultzoa> findByErronkaIdOrderByOrdenaAscIdAsc(Long erronkaId);

    @EntityGraph(attributePaths = "erronka")
    List<ErronkaJardueraMultzoa> findByErronkaIdInOrderByOrdenaAscIdAsc(List<Long> erronkaIds);

    boolean existsByErronkaIdAndIzenaIgnoreCase(Long erronkaId, String izena);

    boolean existsByErronkaIdAndIzenaIgnoreCaseAndIdNot(Long erronkaId, String izena, Long id);
}
