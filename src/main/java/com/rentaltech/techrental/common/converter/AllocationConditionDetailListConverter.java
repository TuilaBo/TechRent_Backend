package com.rentaltech.techrental.common.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentaltech.techrental.device.model.AllocationConditionDetail;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Converter
public class AllocationConditionDetailListConverter implements AttributeConverter<List<AllocationConditionDetail>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<AllocationConditionDetail>> LIST_TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<AllocationConditionDetail> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Không thể chuyển điều kiện allocation sang JSON", e);
        }
    }

    @Override
    public List<AllocationConditionDetail> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, LIST_TYPE);
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể parse JSON điều kiện allocation", e);
        }
    }
}
