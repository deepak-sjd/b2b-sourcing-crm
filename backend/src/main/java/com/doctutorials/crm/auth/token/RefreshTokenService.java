package com.doctutorials.crm.auth.token;

import com.doctutorials.crm.common.exception.UnauthorizedException;
import com.doctutorials.crm.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Issues, rotates, and revokes refresh tokens.
 *
 * Rotation-on-use: every /auth/refresh call revokes the token it was given
 * and issues a brand new one. This means a refresh token is single-use -
 * if a stolen token is ever replayed after the legitimate client already
 * used it, the replay fails (already revoked) and, in a fuller
 * implementation, would be a signal to revoke the whole session family.
 * That extra "theft detection" step is called out below as a follow-up;
 * what's here already stops the most common attack (a leaked token being
 * reused indefinitely).
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHasher tokenHasher;

    @Value("${app.jwt.refresh-token-expiry-days}")
    private long refreshTokenExpiryDays;

    @Transactional
    public String issue(User user) {
        String rawToken = tokenHasher.generateRawToken();

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(tokenHasher.hash(rawToken));
        token.setExpiresAt(Instant.now().plus(refreshTokenExpiryDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(token);

        return rawToken;
    }

    /**
     * Validates a raw refresh token, revokes it, and issues a replacement
     * for the same user. Throws UnauthorizedException for any invalid,
     * expired, or already-used token - callers should treat that as
     * "session ended, log in again" and never leak which specific reason
     * applied.
     */
    @Transactional
    public RotatedTokens rotate(String rawToken) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHasher.hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        if (existing.isRevoked() || existing.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        String newRawToken = issue(existing.getUser());
        return new RotatedTokens(existing.getUser(), newRawToken);
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(tokenHasher.hash(rawToken))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    /**
     * Revokes every active refresh token for a user - used on password
     * reset so a stolen password doesn't leave old sessions valid, and
     * available for a future "log out of all devices" action.
     */
    @Transactional
    public void revokeAllForUser(java.util.UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId);
    }

    public record RotatedTokens(User user, String rawRefreshToken) {
    }
}
