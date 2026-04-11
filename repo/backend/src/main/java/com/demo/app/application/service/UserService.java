package com.demo.app.application.service;

import com.demo.app.domain.model.User;
import com.demo.app.infrastructure.encryption.EmailHashUtil;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<User> getAll() {
        return userRepository.findAll().stream()
                .map(UserEntity::toModel)
                .toList();
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .map(UserEntity::toModel)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserEntity::toModel)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    @Transactional
    public User createUser(User user, String passwordHash) {
        UserEntity entity = UserEntity.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .emailLookupHash(EmailHashUtil.hash(user.getEmail()))
                .passwordHash(passwordHash)
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return userRepository.save(entity).toModel();
    }

    @Transactional
    public User updateUser(Long id, User user) {
        UserEntity entity = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        entity.setUsername(user.getUsername());
        entity.setEmail(user.getEmail());
        entity.setEmailLookupHash(EmailHashUtil.hash(user.getEmail()));
        entity.setDisplayName(user.getDisplayName());
        entity.setRole(user.getRole());
        entity.setEnabled(user.isEnabled());
        entity.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(entity).toModel();
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
