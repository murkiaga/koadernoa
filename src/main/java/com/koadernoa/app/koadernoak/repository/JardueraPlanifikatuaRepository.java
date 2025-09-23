package com.koadernoa.app.koadernoak.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.koadernoak.entitateak.JardueraPlanifikatua;

public interface JardueraPlanifikatuaRepository extends JpaRepository<JardueraPlanifikatua, Long> {
    List<JardueraPlanifikatua> findByUnitateaId(Long unitateaId);
    boolean existsByIdAndUnitatea_Programazioa_Koadernoa_Id(Long jpId, Long koadernoId);
}
