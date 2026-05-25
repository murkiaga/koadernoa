package com.koadernoa.app.objektuak.audit.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.audit.entitateak.AuditEventMota;
import com.koadernoa.app.objektuak.audit.repository.AuditEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(AuditCleanupService.class);

    private final AuditEventRepository auditEventRepository;
    private final AuditCleanupProperties props;

    @Scheduled(cron = "${audit.cleanup.cron:0 30 3 * * SUN}")
    public void runCleanup() {
        if (!props.getCleanup().isEnabled()) {
            logger.debug("Audit cleanup desgaituta dago (audit.cleanup.enabled=false)");
            return;
        }

        try {
            cleanupByRetention("PAGE_VIEW", List.of(AuditEventMota.PAGE_VIEW), props.getRetention().getPageViewDays());
            cleanupByRetention("ACTION", List.of(AuditEventMota.ACTION), props.getRetention().getActionDays());
            cleanupByRetention("LOGIN", List.of(AuditEventMota.LOGIN_OK, AuditEventMota.LOGIN_FAIL, AuditEventMota.LOGOUT), props.getRetention().getLoginDays());
            cleanupByRetention("ERROR", List.of(AuditEventMota.ERROR), props.getRetention().getErrorDays());
        } catch (Exception e) {
            logger.error("Audit cleanup exekuzio orokorrean errorea", e);
        }
    }

    private void cleanupByRetention(String label, List<AuditEventMota> motak, int retentionDays) {
        if (retentionDays < 0) {
            logger.warn("{} cleanup saltatuta: retentionDays negatiboa ({})", label, retentionDays);
            return;
        }

        int batchSize = Math.max(1, props.getCleanup().getBatchSize());
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        long totalDeleted = 0;

        while (true) {
            int deleted = deleteOneBatch(motak, cutoff, batchSize);
            if (deleted <= 0) {
                break;
            }
            totalDeleted += deleted;
            if (deleted < batchSize) {
                break;
            }
        }

        logger.info("Audit cleanup [{}] -> {} event ezabatuta (retention={} egun, cutoff={})", label, totalDeleted, retentionDays, cutoff);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected int deleteOneBatch(List<AuditEventMota> motak, LocalDateTime cutoff, int batchSize) {
        try {
            var motaNames = motak.stream().map(Enum::name).collect(Collectors.toList());
            return auditEventRepository.deleteBatchOlderThanByMota(cutoff, motaNames, batchSize);
        } catch (Exception e) {
            logger.error("Audit cleanup batch errorea. motak={}, cutoff={}, batchSize={}", motak, cutoff, batchSize, e);
            return 0;
        }
    }
}
