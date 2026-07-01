// [S3 이미지 업로드 기능 추가] PostImageRepository
// til_images 테이블에 대한 JPA 리포지토리.
// TIL 단위 이미지 조회·삭제를 지원한다.
package com.Rootin.domain.til.repository;

import com.Rootin.domain.til.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    // TIL(postId) 에 속한 모든 이미지를 순서대로 조회 (TIL 수정 화면 진입 시 사용)
    List<PostImage> findByPostIdOrderByImageOrder(Long postId);

    // TIL 삭제·이미지 전체 교체 시 해당 TIL 이미지를 일괄 삭제
    void deleteByPostId(Long postId);

    // 지정된 이미지 ID 목록만 삭제 (수정 요청에서 일부 이미지만 제거할 때 사용)
    void deleteAllByIdIn(List<Long> ids);
}
