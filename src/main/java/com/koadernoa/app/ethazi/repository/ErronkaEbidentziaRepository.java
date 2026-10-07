package com.koadernoa.app.ethazi.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.koadernoa.app.ethazi.entitateak.errubrikak.ErronkaEbidentzia;

public interface ErronkaEbidentziaRepository extends JpaRepository<ErronkaEbidentzia, Long> {
    List<ErronkaEbidentzia> findByLorpenAdierazleakId(Long adierazleaId);
}
