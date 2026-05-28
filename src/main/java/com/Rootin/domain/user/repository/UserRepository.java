package com.Rootin.domain.user.repository;

import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.entity.ENUM.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);
}