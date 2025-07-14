// src/main/java/com/ronak/welcome/util/JsonListConverter.java
package com.ronak.welcome.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ronak.welcome.DTO.PriceTier;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.List;

@Converter
public class JsonListConverter implements AttributeConverter<List<PriceTier>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule()) // Register module for LocalDateTime
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Write dates as ISO-8601 strings

    @Override
    public String convertToDatabaseColumn(List<PriceTier> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            // Handle the exception, e.g., log it or throw a runtime exception
            throw new RuntimeException("Could not convert list of PriceTier to JSON string", e);
        }
    }

    @Override
    public List<PriceTier> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            // Use TypeReference for deserializing generic collections
            return objectMapper.readValue(dbData, objectMapper.getTypeFactory().constructCollectionType(List.class, PriceTier.class));
        } catch (IOException e) {
            // Handle the exception
            throw new RuntimeException("Could not convert JSON string to list of PriceTier", e);
        }
    }
}
