package net.devstudy.resume.profile.api.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

public enum ProfileConnectionStatus {

    PENDING,
    ACCEPTED;

    public String getDbValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Converter
    public static class PersistJPAConverter implements AttributeConverter<ProfileConnectionStatus, String> {
        @Override
        public String convertToDatabaseColumn(ProfileConnectionStatus attribute) {
            return attribute == null ? null : attribute.getDbValue();
        }

        @Override
        public ProfileConnectionStatus convertToEntityAttribute(String dbValue) {
            return dbValue == null ? null : ProfileConnectionStatus.valueOf(dbValue.toUpperCase(Locale.ROOT));
        }
    }
}
