package com.demo.app.infrastructure.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static FieldEncryptor fieldEncryptor;

    public static void setFieldEncryptor(FieldEncryptor encryptor) {
        fieldEncryptor = encryptor;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return fieldEncryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return fieldEncryptor.decrypt(dbData);
    }
}
