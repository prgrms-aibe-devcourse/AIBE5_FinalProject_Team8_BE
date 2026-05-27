package com.Rootin.domain.plant.entity.converter;

import com.Rootin.domain.plant.entity.enums.Grade;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Grade ENUM과 DB 테이블의 희귀 등급 문자열 값 간의 변환을 지원하는 JPA Converter 클래스입니다.
 * autoApply = true 설정으로, 엔티티에서 Grade 타입을 사용 시 별도 지정 없이도 자동으로 동작합니다.
 */
@Converter(autoApply = true)
public class GradeConverter implements AttributeConverter<Grade, String> {

    @Override
    public String convertToDatabaseColumn(Grade attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getDbValue();
    }

    @Override
    public Grade convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Grade.fromDbValue(dbData);
    }
}
