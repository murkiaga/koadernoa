package com.koadernoa.app.ethazi.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.koadernoa.app.ethazi.entitateak.errubrikak.ZeharkakoEbaluazioa;

public interface ZeharkakoEbaluazioaRepository extends JpaRepository<ZeharkakoEbaluazioa, Long> {
    List<ZeharkakoEbaluazioa> findByErronkaId(Long erronkaId);
}
