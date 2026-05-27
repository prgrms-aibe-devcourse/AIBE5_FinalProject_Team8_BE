package com.Rootin.domain.til.repository;

import com.Rootin.domain.til.entity.TilTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TilTemplateRepository extends JpaRepository<TilTemplate, Long> {

    @Query("SELECT t FROM TilTemplate t WHERE t.isDefault = true OR t.user.id = :userId")
    List<TilTemplate> findByUserIdOrIsDefault(Long userId);
}
