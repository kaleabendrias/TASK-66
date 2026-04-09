package com.demo.app.application.service;

import com.demo.app.domain.exception.ConflictException;
import com.demo.app.domain.exception.ResourceNotFoundException;
import com.demo.app.persistence.entity.AccountDeletionRequestEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.AccountDeletionRequestRepository;
import com.demo.app.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);

    private final AccountDeletionRequestRepository deletionRepository;
    private final UserRepository userRepository;

    @Value("${app.account-deletion.cooling-off-days:30}")
    private int coolingOffDays;

    @Transactional
    public AccountDeletionRequestEntity requestDeletion(Long userId) {
        deletionRepository.findByUserIdAndStatus(userId, "PENDING")
                .ifPresent(existing -> {
                    throw new ConflictException("A pending deletion request already exists for this account.");
                });

        AccountDeletionRequestEntity request = AccountDeletionRequestEntity.builder()
                .userId(userId)
                .status("PENDING")
                .requestedAt(LocalDateTime.now())
                .coolingOffEndsAt(LocalDateTime.now().plusDays(coolingOffDays))
                .build();

        return deletionRepository.save(request);
    }

    @Transactional
    public AccountDeletionRequestEntity cancelDeletion(Long requestId, Long userId) {
        AccountDeletionRequestEntity request = deletionRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Deletion request", requestId));

        if (!request.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to cancel this request");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new ConflictException("Only pending requests can be cancelled");
        }

        request.setStatus("CANCELLED");
        request.setCancelledAt(LocalDateTime.now());
        return deletionRepository.save(request);
    }

    @Transactional
    public void processExpired() {
        List<AccountDeletionRequestEntity> expired = deletionRepository.findExpiredCoolingOff(LocalDateTime.now());

        for (AccountDeletionRequestEntity request : expired) {
            UserEntity user = userRepository.findById(request.getUserId()).orElse(null);
            if (user != null) {
                // Anonymize personal data
                String anonymized = "deleted_" + request.getUserId();
                user.setUsername(anonymized);
                user.setEmail(anonymized + "@deleted.local");
                user.setDisplayName("Deleted User");
                user.setPasswordHash("DELETED");
                user.setEnabled(false);
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
            }

            request.setStatus("PROCESSED");
            request.setProcessedAt(LocalDateTime.now());
            deletionRepository.save(request);

            log.info("Processed account deletion for user {}", request.getUserId());
        }

        if (!expired.isEmpty()) {
            log.info("Processed {} expired account deletion requests", expired.size());
        }
    }

    public AccountDeletionRequestEntity getByUser(Long userId) {
        return deletionRepository.findByUserIdAndStatus(userId, "PENDING").orElse(null);
    }
}
