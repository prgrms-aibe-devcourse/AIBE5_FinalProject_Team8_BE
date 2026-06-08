package com.Rootin.domain.user.entity;

import com.Rootin.global.BaseEntity;
import com.Rootin.domain.user.entity.ENUM.Provider;
import com.Rootin.domain.user.entity.ENUM.Role;
import com.Rootin.domain.user.entity.ENUM.GardenTheme;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    // 닉네임 동시성 방어 및 데이터베이스 차원에서의 고유성(Unique)을 완벽하게 보장하기 위해
    // unique = true 제약 조건 설정을 명시적으로 추가합니다. 이로써 동일 닉네임 동시 가입/변경 시 예외가 안전하게 발생합니다.
    @Column(name = "nickname", nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Getter(AccessLevel.NONE)
    @Column(name = "password", length = 100)
    private String password;

    @Column(name = "bio", length = 255)
    private String bio;

    @Column(name = "point", nullable = false, columnDefinition = "INT DEFAULT 0")
    private int point;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "provider_id")
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 20)
    private Provider provider;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "garden_theme", nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'FOREST'")
    private GardenTheme gardenTheme = GardenTheme.FOREST;

    @PrePersist
    protected void applyDefaults() {
        if (nickname == null || nickname.isBlank()) {
            nickname = email != null && email.contains("@")
                    ? email.substring(0, email.indexOf("@"))
                    : "user";
        }
        if (role == null) {
            role = Role.USER;
        }
        if (provider == null) {
            provider = Provider.LOCAL;
        }
        if (gardenTheme == null) {
            gardenTheme = GardenTheme.FOREST;
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return !isDeleted;
    }

    // 비즈니스 메서드
    public void deductPoint(int amount) {
        if (this.point < amount) {
            throw new IllegalStateException(
                    "포인트가 부족합니다. 현재: " + this.point + ", 필요: " + amount
            );
        }
        this.point -= amount;
    }

    public void addPoint(int amount) {
        this.point += amount;
    }

    public void deactivate() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void updateProfile(String nickname, String bio, String profileImageUrl) {
        this.nickname = nickname;
        if (bio != null) {
            this.bio = bio;
        }
        if (profileImageUrl != null && !profileImageUrl.isBlank()) {
            this.profileImage = profileImageUrl;
        }
    }

    public void updateGardenTheme(GardenTheme theme) {
        if (theme != null) {
            this.gardenTheme = theme;
        }
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
