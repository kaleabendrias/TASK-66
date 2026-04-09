package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.LoginAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttemptEntity, Long> {

    @Query("SELECT COUNT(a) FROM LoginAttemptEntity a WHERE a.username = :username AND a.success = false AND a.attemptedAt > :since")
    long countRecentFailed(@Param("username") String username, @Param("since") LocalDateTime since);

    @Query("SELECT a FROM LoginAttemptEntity a WHERE a.username = :username AND a.success = false ORDER BY a.attemptedAt DESC")
    List<LoginAttemptEntity> findRecentFailed(@Param("username") String username);
}
