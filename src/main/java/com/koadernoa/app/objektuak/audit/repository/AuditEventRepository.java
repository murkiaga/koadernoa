package com.koadernoa.app.objektuak.audit.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.audit.entitateak.AuditEvent;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
}
