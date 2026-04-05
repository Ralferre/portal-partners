package com.example.portalpartners.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AttributeConverter JPA que cifra/decifra campos sensiveis transparentemente.
 *
 * Padrao de injecao via campo estatico: necessario porque JPA instancia
 * converters fora do contexto Spring. O @Autowired no setter configura o
 * servico estatico apos o contexto ser inicializado.
 *
 * Uso: @Convert(converter = EncryptedStringConverter.class)
 * no campo da entidade.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static FieldEncryptionService encryptionService;

    @Autowired
    public void setFieldEncryptionService(FieldEncryptionService service) {
        EncryptedStringConverter.encryptionService = service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || encryptionService == null) return attribute;
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || encryptionService == null) return dbData;
        return encryptionService.decrypt(dbData);
    }
}
