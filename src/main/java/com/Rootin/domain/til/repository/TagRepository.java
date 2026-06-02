package com.Rootin.domain.til.repository;

import com.Rootin.domain.til.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    /**
     * 태그 이름 목록에 매핑되는 모든 기존 태그 데이터를 벌크 조회합니다.
     * TIL 생성/수정 시 태그 동기화(N+1 SELECT 차단) 성능 개선을 위해 사용됩니다.
     *
     * @param names 조회할 태그 이름 목록
     * @return DB에 이미 등록되어 있는 태그 엔티티 리스트
     */
    List<Tag> findByNameIn(List<String> names);
}
