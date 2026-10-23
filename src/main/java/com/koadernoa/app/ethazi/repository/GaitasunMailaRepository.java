package com.koadernoa.app.ethazi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.*;

public interface GaitasunMailaRepository extends JpaRepository<GaitasunMaila, Long> {
    boolean existsByMailaId(Long mailaId);
    java.util.List<GaitasunMaila> findByMailaId(Long mailaId);
}
