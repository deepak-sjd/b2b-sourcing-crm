package com.doctutorials.crm.auth.token;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Both token tables grow forever otherwise - every login issues a refresh
 * token, every rotation revokes-and-reissues one. This runs on the same
 * fixedRate style as TaskService.markOverdueTasks, once every 6 hours,
 * and deletes anything already expired or already revoked/used - there's
 * nothing worth keeping past that point (no audit requirement here; if
 * one shows up later, delete should become soft-delete instead).
 */
@Component
@RequiredArgsConstructor
public class TokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupJob.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Scheduled(fixedRate = 6 * 60 * 60 * 1000)
    @Transactional
    public void cleanup() {
        Instant now = Instant.now();
        int refreshDeleted = refreshTokenRepository.deleteExpiredOrRevoked(now);
        int resetDeleted = passwordResetTokenRepository.deleteExpiredOrUsed(now);
        if (refreshDeleted > 0 || resetDeleted > 0) {
            log.info("Token cleanup: removed {} refresh tokens, {} password reset tokens",
                    refreshDeleted, resetDeleted);
        }
    }
}
