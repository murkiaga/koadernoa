package com.koadernoa.app.ethazi.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.*;

public interface MailakatzeEreduaRepository extends JpaRepository<MailakatzeEredua, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from MailakatzeEredua e where e.id = :id")
    Optional<MailakatzeEredua> findLockedById(Long id);

    Optional<MailakatzeEredua> findByZikloaIdAndMota(Long zikloaId, GaitasunMota mota);
}
