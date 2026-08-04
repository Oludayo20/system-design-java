package com.systemdesign.modularmonolith.ordering.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts {@link OrderStatus} to/from the lowercase text value stored in the Postgres native
 * enum column {@code ordering.orders.status}. The JDBC URL carries {@code ?stringtype=unspecified}
 * (see application.yml) so pgjdbc lets a plain String bind be implicitly cast into the native
 * enum column -- the standard, well-documented workaround for Hibernate + Postgres native enums.
 */
@Converter
public class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {

    @Override
    public String convertToDatabaseColumn(OrderStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public OrderStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : OrderStatus.fromValue(dbData);
    }
}
