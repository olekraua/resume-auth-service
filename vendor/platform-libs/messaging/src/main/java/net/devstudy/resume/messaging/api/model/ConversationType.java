package net.devstudy.resume.messaging.api.model;

import java.util.Locale;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum ConversationType {

    DIRECT,
    GROUP;

    public String getDbValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Converter
    public static class PersistJPAConverter implements AttributeConverter<ConversationType, String> {
        @Override
        public String convertToDatabaseColumn(ConversationType attribute) {
            return attribute == null ? null : attribute.getDbValue();
        }

        @Override
        public ConversationType convertToEntityAttribute(String dbValue) {
            return dbValue == null ? null : ConversationType.valueOf(dbValue.toUpperCase(Locale.ROOT));
        }
    }
}
