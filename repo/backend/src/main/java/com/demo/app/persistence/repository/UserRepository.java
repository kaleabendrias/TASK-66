package com.demo.app.persistence.repository;

import com.demo.app.domain.enums.Role;
import com.demo.app.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmailLookupHash(String emailLookupHash);
    Optional<UserEntity> findByEmailLookupHash(String emailLookupHash);
    Optional<UserEntity> findFirstByRole(Role role);
}
