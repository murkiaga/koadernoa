package com.koadernoa.app.ethazi.repository;

import java.util.Optional;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.*;

public interface GaitasunaRepository extends JpaRepository<Gaitasuna, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Gaitasuna e where e.id = :id")
    Optional<Gaitasuna> findLockedById(Long id);

    List<Gaitasuna> findByZikloaIdAndMotaOrderByKodeaAsc(Long zikloaId, GaitasunMota mota);
    boolean existsByZikloaIdAndMota(Long zikloaId, GaitasunMota mota);
}
