package com.Rootin.domain.auth.entity;

import com.Rootin.domain.user.entity.User;
import com.Rootin.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "token", nullable = false)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "rotated_at")
    private LocalDateTime rotatedAt;

    @Column(name = "grace_expires_at")
    private LocalDateTime graceExpiresAt;

    @Column(name = "replacement_token", length = 512)
    private String replacementToken;

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

    public boolean isRotated() {
        return rotatedAt != null;
    }

    public boolean isWithinGracePeriod(LocalDateTime now) {
        return graceExpiresAt != null && !graceExpiresAt.isBefore(now);
    }

    public void rotateTo(String replacementToken, LocalDateTime rotatedAt, LocalDateTime graceExpiresAt) {
        this.replacementToken = replacementToken;
        this.rotatedAt = rotatedAt;
        this.graceExpiresAt = graceExpiresAt;
    }
}
