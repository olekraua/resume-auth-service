package net.devstudy.resume.shared.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

public enum LanguageType {

    ALL,
    SPOKEN,
    WRITING;

    public String getDbValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public LanguageType getReverseType() {
        return switch (this) {
            case SPOKEN -> WRITING;
            case WRITING -> SPOKEN;
            default -> throw new IllegalArgumentException(this + " does not have reverse type");
        };
    }

    @Converter
    public static class PersistJPAConverter implements AttributeConverter<LanguageType, String> {
        @Override
        public String convertToDatabaseColumn(LanguageType attribute) {
            return attribute == null ? null : attribute.getDbValue();
        }
        @Override
        public LanguageType convertToEntityAttribute(String dbValue) {
            return dbValue == null ? null : LanguageType.valueOf(dbValue.toUpperCase(Locale.ROOT));
        }
    }
}

