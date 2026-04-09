package com.demo.app.infrastructure.encryption;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EncryptedStringConverterInitializer {

    private final FieldEncryptor fieldEncryptor;

    @PostConstruct
    public void init() {
        EncryptedStringConverter.setFieldEncryptor(fieldEncryptor);
    }
}
