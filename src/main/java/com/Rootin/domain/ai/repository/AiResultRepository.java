package com.Rootin.domain.ai.repository;

import com.Rootin.domain.ai.entity.AiResult;
import com.Rootin.domain.til.entity.Post;
import com.Rootin.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiResultRepository extends JpaRepository<AiResult, Long> {

    // 본인 전체 결과 조회
    List<AiResult> findAllByUser(User user);

    // 본인 + 특정 TIL 결과 조회
    List<AiResult> findAllByUserAndPost(User user, Post post);
}
