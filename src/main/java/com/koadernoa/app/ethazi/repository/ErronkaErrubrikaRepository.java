package com.koadernoa.app.ethazi.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.koadernoa.app.ethazi.entitateak.errubrikak.ErronkaErrubrika;

public interface ErronkaErrubrikaRepository extends JpaRepository<ErronkaErrubrika, Long> {
    Optional<ErronkaErrubrika> findByErronkaIdAndModuloaId(Long erronkaId, Long moduloaId);
    List<ErronkaErrubrika> findByErronkaId(Long erronkaId);
}
