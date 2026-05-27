package com.Rootin.domain.plant.entity.converter;

import com.Rootin.domain.plant.entity.enums.GrowthStage;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * GrowthStage ENUM과 DB 테이블의 ENUM 문자열 값 간의 변환을 지원하는 JPA Converter 클래스입니다.
 * autoApply = true 설정으로, 엔티티에서 GrowthStage 타입을 사용 시 자동으로 동작합니다.
 */
@Converter(autoApply = true)
public class GrowthStageConverter implements AttributeConverter<GrowthStage, String> {

    @Override
    public String convertToDatabaseColumn(GrowthStage attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getDbValue();
    }

    @Override
    public GrowthStage convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return GrowthStage.fromDbValue(dbData);
    }
}
