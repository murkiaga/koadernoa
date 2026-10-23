package com.koadernoa.app.ethazi.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.koadernoa.app.ethazi.entitateak.errubrikak.ZeharkakoErrubrika;

public interface ZeharkakoErrubrikaRepository extends JpaRepository<ZeharkakoErrubrika, Long> {
    Optional<ZeharkakoErrubrika> findByErronkaId(Long erronkaId);
}
