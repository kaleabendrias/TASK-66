package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.MemberProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberProfileRepository extends JpaRepository<MemberProfileEntity, Long> {
    Optional<MemberProfileEntity> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
