package com.kisansetu.auth;

import com.kisansetu.domain.AuditLog;
import com.kisansetu.repository.AppUserRepository;
import com.kisansetu.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Append-only record of who did what. Never put Aadhaar numbers or other identifiers in {@code details}. */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLog;
    private final AppUserRepository users;

    @Transactional
    public void record(Long userId, String action, String entityType, Long entityId, String details) {
        AuditLog entry = new AuditLog();
        entry.setUser(userId == null ? null : users.getReferenceById(userId));
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details == null ? null : (details.length() <= 1000 ? details : details.substring(0, 1000)));
        auditLog.save(entry);
    }
}
