package com.doctutorials.crm.auth.token;

import com.doctutorials.crm.common.BaseEntity;
import com.doctutorials.crm.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens", indexes = {
        @Index(name = "idx_password_reset_tokens_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_password_reset_tokens_user_id", columnList = "user_id")
})
@Getter
@Setter
public class PasswordResetToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;
}
