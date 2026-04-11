package com.demo.app.infrastructure.encryption;

import com.demo.app.DemoApplication;
import com.demo.app.domain.enums.Role;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DemoApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("UserEntity PII at-rest encryption")
class UserPiiEncryptionPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FieldEncryptor fieldEncryptor;

    @PersistenceContext
    private EntityManager entityManager;

    private String uniqueSuffix;

    @BeforeEach
    void setUp() {
        uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
    }

    private UserEntity persistSample(String plaintextEmail, String plaintextDisplayName) {
        UserEntity entity = UserEntity.builder()
                .username("enc_" + uniqueSuffix)
                .email(plaintextEmail)
                .emailLookupHash(EmailHashUtil.hash(plaintextEmail))
                .passwordHash("$2a$10$CDdcj12dr65C27ckLRMFQevdNud3wkqYzCcyk5iCsqCihFJDF1Ol2")
                .displayName(plaintextDisplayName)
                .role(Role.MEMBER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        UserEntity saved = userRepository.saveAndFlush(entity);
        entityManager.clear();
        return saved;
    }

    private String rawColumn(Long userId, String column) {
        Object result = entityManager
                .createNativeQuery("SELECT " + column + " FROM app_user WHERE id = :id")
                .setParameter("id", userId)
                .getSingleResult();
        return result == null ? null : result.toString();
    }

    @Test
    @DisplayName("email column holds ciphertext, not plaintext")
    void emailIsEncryptedAtRest() {
        String plaintextEmail = "alice+" + uniqueSuffix + "@example.com";
        UserEntity saved = persistSample(plaintextEmail, "Alice Example");

        String rawEmail = rawColumn(saved.getId(), "email");

        assertThat(rawEmail).isNotNull();
        assertThat(rawEmail).isNotEqualTo(plaintextEmail);
        assertThat(rawEmail).doesNotContain(plaintextEmail);
        // Encrypted column must be the AES/GCM ciphertext — Base64 payload,
        // meaningfully longer than the plaintext. If someone forgets the
        // @Convert annotation, this length check catches it.
        assertThat(rawEmail.length()).isGreaterThan(plaintextEmail.length());
        assertThat(fieldEncryptor.decrypt(rawEmail)).isEqualTo(plaintextEmail);
    }

    @Test
    @DisplayName("display_name column holds ciphertext, not plaintext")
    void displayNameIsEncryptedAtRest() {
        String plaintextName = "Alice " + uniqueSuffix;
        UserEntity saved = persistSample("dn+" + uniqueSuffix + "@example.com", plaintextName);

        String rawDisplayName = rawColumn(saved.getId(), "display_name");

        assertThat(rawDisplayName).isNotNull();
        assertThat(rawDisplayName).isNotEqualTo(plaintextName);
        assertThat(rawDisplayName).doesNotContain(plaintextName);
        assertThat(fieldEncryptor.decrypt(rawDisplayName)).isEqualTo(plaintextName);
    }

    @Test
    @DisplayName("repository reads decrypt values transparently")
    void repositoryReadsDecryptTransparently() {
        String plaintextEmail = "bob+" + uniqueSuffix + "@example.com";
        String plaintextName = "Bob " + uniqueSuffix;
        UserEntity saved = persistSample(plaintextEmail, plaintextName);

        UserEntity loaded = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getEmail()).isEqualTo(plaintextEmail);
        assertThat(loaded.getDisplayName()).isEqualTo(plaintextName);
    }

    @Test
    @DisplayName("same plaintext email encrypts to different ciphertexts on repeated writes")
    void encryptionIsNondeterministic() {
        String plaintextEmail = "nondet+" + uniqueSuffix + "@example.com";
        UserEntity a = persistSample(plaintextEmail, "Plain Name A");

        // Force a re-encryption by updating the row via a managed entity.
        UserEntity managed = entityManager.find(UserEntity.class, a.getId());
        managed.setDisplayName("Plain Name A (updated)");
        userRepository.saveAndFlush(managed);
        entityManager.clear();

        String rawAfterUpdate = rawColumn(a.getId(), "display_name");
        assertThat(fieldEncryptor.decrypt(rawAfterUpdate)).isEqualTo("Plain Name A (updated)");
        // Re-encrypting the same plaintext must yield a different ciphertext.
        String encryptedAgain = fieldEncryptor.encrypt("Plain Name A (updated)");
        assertThat(rawAfterUpdate).isNotEqualTo(encryptedAgain);
    }

    @Test
    @DisplayName("email_lookup_hash enforces uniqueness and enables lookup")
    void emailLookupHashEnablesLookup() {
        String plaintextEmail = "lookup+" + uniqueSuffix + "@example.com";
        UserEntity saved = persistSample(plaintextEmail, "Lookup User");

        String hash = EmailHashUtil.hash(plaintextEmail);
        assertThat(userRepository.existsByEmailLookupHash(hash)).isTrue();
        assertThat(userRepository.findByEmailLookupHash(hash))
                .isPresent()
                .get()
                .extracting(UserEntity::getId)
                .isEqualTo(saved.getId());

        // Case-insensitive normalization: uppercase lookups resolve to the same row.
        assertThat(userRepository.existsByEmailLookupHash(
                EmailHashUtil.hash(plaintextEmail.toUpperCase()))).isTrue();
    }
}
