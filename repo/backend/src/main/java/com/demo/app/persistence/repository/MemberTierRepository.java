package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.MemberTierEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberTierRepository extends JpaRepository<MemberTierEntity, Long> {
    Optional<MemberTierEntity> findByName(String name);
    Optional<MemberTierEntity> findByRank(int rank);
}
