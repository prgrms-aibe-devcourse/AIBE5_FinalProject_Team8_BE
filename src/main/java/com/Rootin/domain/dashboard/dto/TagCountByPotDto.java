package com.Rootin.domain.dashboard.dto;

// TilTagRepository JPQL 집계 쿼리 결과를 받기 위한 인터페이스 프로젝션
public interface TagCountByPotDto {
    Long getPotId();
    String getTagName();
    Long getTagCount();
}
