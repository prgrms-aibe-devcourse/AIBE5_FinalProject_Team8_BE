package com.Rootin.domain.til.repository;

import com.Rootin.domain.til.entity.PostStatus;
import com.Rootin.domain.til.entity.Til;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TilRepository extends JpaRepository<Til, Long> {

    Page<Til> findByUserIdAndStatus(Long userId, PostStatus status, Pageable pageable);

    Page<Til> findByUserIdAndPotIdAndStatus(Long userId, Long potId, PostStatus status, Pageable pageable);
}
