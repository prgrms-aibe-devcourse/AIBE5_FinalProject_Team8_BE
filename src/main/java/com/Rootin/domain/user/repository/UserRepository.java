package com.Rootin.domain.user.repository;

import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.entity.ENUM.Provider;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailWithLock(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.provider = :provider AND u.providerId = :providerId")
    Optional<User> findByProviderAndProviderIdWithLock(@Param("provider") Provider provider, @Param("providerId") String providerId);

    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);

    List<User> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime cutoff);
}
