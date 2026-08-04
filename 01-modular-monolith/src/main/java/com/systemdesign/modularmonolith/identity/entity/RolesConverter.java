package com.systemdesign.modularmonolith.identity.entity;

import com.systemdesign.modularmonolith.identity.UserRole;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps {@code List<UserRole>} to a comma-separated text column, matching TypeORM's
 * {@code @Column({ type: 'simple-array' })} used on {@code user.entity.ts#roles}.
 */
@Converter
public class RolesConverter implements AttributeConverter<List<UserRole>, String> {

    @Override
    public String convertToDatabaseColumn(List<UserRole> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return UserRole.CUSTOMER.getValue();
        }
        return attribute.stream().map(UserRole::getValue).collect(Collectors.joining(","));
    }

    @Override
    public List<UserRole> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of(UserRole.CUSTOMER);
        }
        return Arrays.stream(dbData.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(UserRole::fromValue)
                .collect(Collectors.toList());
    }
}
