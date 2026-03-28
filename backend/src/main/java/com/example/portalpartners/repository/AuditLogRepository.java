package com.example.portalpartners.repository;

import com.example.portalpartners.model.AuditLog;
import com.example.portalpartners.model.StatusAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>,
        JpaSpecificationExecutor<AuditLog> {

    /** Job de retencao: remove registros com mais de 365 dias. */
    @Modifying
    @Transactional
    @Query("DELETE FROM AuditLog a WHERE a.timestamp < :limite")
    void deleteByTimestampBefore(LocalDateTime limite);

    long countByUserIdAndAcaoAndStatus(Long userId, String acao, StatusAuditoria status);
}
