package com.Rootin.domain.garden.repository;

import com.Rootin.domain.garden.entity.Pot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PotRepository extends JpaRepository<Pot, Long> {
    
    // 특정 사용자가 생성한 화분 목록을 조회하기 위한 메소드
    List<Pot> findByUserId(Long userId);
}
