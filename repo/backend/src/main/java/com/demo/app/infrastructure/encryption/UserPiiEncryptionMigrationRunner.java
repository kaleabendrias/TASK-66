package com.demo.app.infrastructure.encryption;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Encrypts any plaintext values still sitting in app_user.email / display_name,
 * and backfills email_lookup_hash. Idempotent: rows whose columns already
 * decrypt cleanly are left alone.
 *
 * Runs after the bean graph is up so the EncryptedStringConverter has its
 * FieldEncryptor injected. Uses JdbcTemplate to bypass the JPA converter and
 * see the raw column values.
 */
@Component
@RequiredArgsConstructor
public class UserPiiEncryptionMigrationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserPiiEncryptionMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final FieldEncryptor fieldEncryptor;

    @EventListener(ApplicationReadyEvent.class)
    @Order(0)
    @Transactional
    public void migrate() {
        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList(
                    "SELECT id, email, display_name, email_lookup_hash FROM app_user");
        } catch (Exception e) {
            log.warn("Skipping PII encryption migration, app_user not present: {}", e.getMessage());
            return;
        }

        int migrated = 0;
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            String email = (String) row.get("email");
            String displayName = (String) row.get("display_name");
            String emailHash = (String) row.get("email_lookup_hash");

            String plaintextEmail = decryptOrNull(email);
            String plaintextDisplayName = decryptOrNull(displayName);

            boolean emailIsEncrypted = plaintextEmail != null;
            boolean displayNameIsEncrypted = plaintextDisplayName != null;

            if (emailIsEncrypted && displayNameIsEncrypted && emailHash != null) {
                continue;
            }

            String storedEmail = emailIsEncrypted
                    ? email
                    : fieldEncryptor.encrypt(email);
            String storedDisplayName = displayNameIsEncrypted
                    ? displayName
                    : fieldEncryptor.encrypt(displayName);
            String storedHash = emailHash != null
                    ? emailHash
                    : EmailHashUtil.hash(emailIsEncrypted ? plaintextEmail : email);

            jdbcTemplate.update(
                    "UPDATE app_user SET email = ?, display_name = ?, email_lookup_hash = ? WHERE id = ?",
                    storedEmail, storedDisplayName, storedHash, id);
            migrated++;
        }

        if (migrated > 0) {
            log.info("Encrypted PII for {} user row(s) at startup", migrated);
        }
    }

    private String decryptOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            return fieldEncryptor.decrypt(value);
        } catch (Exception e) {
            return null;
        }
    }
}
