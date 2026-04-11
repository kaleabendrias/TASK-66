package com.demo.app.persistence.entity;

import com.demo.app.domain.enums.Role;
import com.demo.app.domain.model.User;
import com.demo.app.infrastructure.encryption.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "email", nullable = false, length = 512)
    private String email;

    // Deterministic SHA-256 of the normalized email. Lets us enforce uniqueness
    // and look up by email without ever needing to decrypt — the encrypted
    // column alone cannot satisfy either need because AES/GCM is randomized.
    @Column(name = "email_lookup_hash", length = 64, unique = true)
    private String emailLookupHash;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "display_name", nullable = false, length = 512)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Role role;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public User toModel() {
        return User.builder()
                .id(id)
                .username(username)
                .email(email)
                .displayName(displayName)
                .role(role)
                .enabled(enabled)
                .build();
    }
}
