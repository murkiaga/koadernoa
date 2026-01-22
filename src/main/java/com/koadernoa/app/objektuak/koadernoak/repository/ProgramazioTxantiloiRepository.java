package com.koadernoa.app.objektuak.koadernoak.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.koadernoak.entitateak.ProgramazioTxantiloi;

public interface ProgramazioTxantiloiRepository extends JpaRepository<ProgramazioTxantiloi, Long> {
    List<ProgramazioTxantiloi> findByIrakasleaIdAndModuloaIdOrderBySortzeDataDesc(Long irakasleaId, Long moduloaId);

    Optional<ProgramazioTxantiloi> findByIdAndIrakasleaId(Long id, Long irakasleaId);
}
