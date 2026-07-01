package com.Rootin.domain.user.repository;

import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.entity.ENUM.Provider;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    interface UserMeProjection {
        Long getId();
        String getEmail();
        String getNickname();
        String getProfileImageUrl();
        String getBio();
        Integer getPoint();
        String getProvider();
        LocalDateTime getCreatedAt();
        Long getTilCount();
    }

    Optional<User> findByEmail(String email);
    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailWithLock(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.provider = :provider AND u.providerId = :providerId")
    Optional<User> findByProviderAndProviderIdWithLock(@Param("provider") Provider provider, @Param("providerId") String providerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdWithLock(@Param("userId") Long userId);

    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);

    @Query(value = """
        SELECT
            u.id AS id,
            u.email AS email,
            u.nickname AS nickname,
            u.profile_image AS profileImageUrl,
            u.bio AS bio,
            u.point AS point,
            u.provider AS provider,
            u.created_at AS createdAt,
            (SELECT COUNT(*)
             FROM posts p
             JOIN til t ON t.post_id = p.id
             WHERE p.user_id = u.id
               AND p.status = :publishedStatus) AS tilCount
        FROM users u
        WHERE u.id = :userId
    """, nativeQuery = true)
    Optional<UserMeProjection> findUserMeById(
            @Param("userId") Long userId,
            @Param("publishedStatus") String publishedStatus
    );

    /** 포인트를 원자적으로 증감 — 동시 요청 시 lost update 방지 */
    @Modifying
    @Query("UPDATE User u SET u.point = u.point + :amount WHERE u.id = :userId")
    void incrementPoint(@Param("userId") Long userId, @Param("amount") int amount);

    List<User> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime cutoff);
}
