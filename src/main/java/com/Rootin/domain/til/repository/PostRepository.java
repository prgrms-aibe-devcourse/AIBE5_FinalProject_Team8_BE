package com.Rootin.domain.til.repository;

import com.Rootin.domain.til.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
