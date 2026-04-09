package com.demo.app.infrastructure.ratelimit;

import com.demo.app.persistence.entity.LoginAttemptEntity;
import com.demo.app.persistence.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;

    @Transactional
    public void recordAttempt(String username, String ipAddress, boolean success) {
        LoginAttemptEntity attempt = LoginAttemptEntity.builder()
                .username(username)
                .ipAddress(ipAddress)
                .success(success)
                .attemptedAt(LocalDateTime.now())
                .build();
        loginAttemptRepository.save(attempt);
    }

    public boolean isLockedOut(String username) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long failedCount = loginAttemptRepository.countRecentFailed(username, oneHourAgo);
        if (failedCount < 10) {
            return false;
        }
        List<LoginAttemptEntity> recentFailed = loginAttemptRepository.findRecentFailed(username);
        if (recentFailed.isEmpty()) {
            return false;
        }
        LocalDateTime mostRecentFailure = recentFailed.get(0).getAttemptedAt();
        return mostRecentFailure.isAfter(LocalDateTime.now().minusMinutes(15));
    }

    public long getRecentFailedCount(String username) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        return loginAttemptRepository.countRecentFailed(username, oneHourAgo);
    }
}
