package com.Rootin.domain.gamification.repository;

import com.Rootin.domain.gamification.entity.PointLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointLogRepository extends JpaRepository<PointLog, Long> {
}
