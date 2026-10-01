package com.doctutorials.crm.common.exception;

import java.time.Instant;

/**
 * Thrown when login is attempted against an account that is currently
 * locked out after too many failed attempts. Mapped to HTTP 423 (Locked)
 * by GlobalExceptionHandler.
 */
public class AccountLockedException extends RuntimeException {

    private final Instant lockedUntil;

    public AccountLockedException(Instant lockedUntil) {
        super("Account temporarily locked due to too many failed login attempts. Try again after "
                + lockedUntil);
        this.lockedUntil = lockedUntil;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }
}
