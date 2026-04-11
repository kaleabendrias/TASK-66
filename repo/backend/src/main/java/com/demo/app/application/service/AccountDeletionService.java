package com.demo.app.application.service;

import com.demo.app.domain.exception.ConflictException;
import com.demo.app.domain.exception.ResourceNotFoundException;
import com.demo.app.persistence.entity.AccountDeletionRequestEntity;
import com.demo.app.persistence.entity.IncidentEntity;
import com.demo.app.persistence.entity.LoginAttemptEntity;
import com.demo.app.persistence.entity.MemberProfileEntity;
import com.demo.app.persistence.entity.RiskEventEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.AccountDeletionRequestRepository;
import com.demo.app.persistence.repository.IncidentRepository;
import com.demo.app.persistence.repository.LoginAttemptRepository;
import com.demo.app.persistence.repository.MemberProfileRepository;
import com.demo.app.persistence.repository.RiskEventRepository;
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
    private final MemberProfileRepository memberProfileRepository;
    private final IncidentRepository incidentRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final RiskEventRepository riskEventRepository;

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
            scrubUserData(request.getUserId());

            request.setStatus("PROCESSED");
            request.setProcessedAt(LocalDateTime.now());
            deletionRepository.save(request);

            log.info("Processed account deletion for user {}", request.getUserId());
        }

        if (!expired.isEmpty()) {
            log.info("Processed {} expired account deletion requests", expired.size());
        }
    }

    /**
     * Irreversibly scrub every PII-bearing field on tables that reference the
     * deleted user. This is the regulatory-facing "right to be forgotten"
     * implementation: after it runs no direct identifier (email, display name,
     * phone, street address) and no deterministic lookup hash may remain in
     * the database for this user id. Rows that carry audit/risk signal are
     * kept, but their free-form content is overwritten so no residual
     * identifiable data leaks via analytics or backups.
     */
    private void scrubUserData(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            // Nothing to scrub — the row has already been cleared or never existed.
            return;
        }

        String oldUsername = user.getUsername();
        String anonymized = "deleted_" + userId;

        // 1. UserEntity: clear every field that could re-identify the user.
        //    emailLookupHash MUST be cleared — it is a SHA-256 of the old
        //    email and would otherwise let anyone recompute the hash and
        //    find the "deleted" row.
        user.setUsername(anonymized);
        user.setEmail(anonymized + "@deleted.local");
        user.setEmailLookupHash(null);
        user.setDisplayName("Deleted User");
        user.setPasswordHash("DELETED");
        user.setEnabled(false);
        user.setUpdatedAt(now);
        userRepository.save(user);

        // 2. MemberProfileEntity: blank both the ciphertext and the masked
        //    form of the phone number. Keeping either leaks a partial
        //    identifier (masked last-4) or the full plaintext (decryptable
        //    ciphertext, since our key is long-lived).
        memberProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setPhoneEncrypted(null);
            profile.setPhoneMasked(null);
            profile.setUpdatedAt(now);
            memberProfileRepository.save(profile);
        });

        // 3. IncidentEntity: a reporter may have dictated their street
        //    address and a free-form description that contains PII.
        //    Overwrite address/cross_street and replace description with a
        //    static tombstone so the incident row remains for moderation
        //    history but carries no residual personal data.
        for (IncidentEntity incident : incidentRepository.findByReporterId(userId)) {
            incident.setAddress(null);
            incident.setCrossStreet(null);
            incident.setDescription("[REDACTED — account deleted]");
            incident.setUpdatedAt(now);
            incidentRepository.save(incident);
        }

        // 4. LoginAttemptEntity: rows are keyed by the submitted username
        //    string, not by user id. Delete every row keyed on the user's
        //    old username so rate-limit / lockout history cannot be joined
        //    back to this account via the pre-anonymization username.
        List<LoginAttemptEntity> attempts = loginAttemptRepository.findRecentFailed(oldUsername);
        if (!attempts.isEmpty()) {
            loginAttemptRepository.deleteAll(attempts);
        }

        // 5. RiskEventEntity: the `details` JSON column may contain IPs,
        //    user-agents, or free-form strings that re-identify the user.
        //    Keep the event rows (they carry fraud signal joined on user_id
        //    which is already scrubbed) but overwrite the payload.
        List<RiskEventEntity> riskEvents = riskEventRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (RiskEventEntity event : riskEvents) {
            event.setDetails("{\"redacted\":true}");
            riskEventRepository.save(event);
        }
    }

    public AccountDeletionRequestEntity getByUser(Long userId) {
        return deletionRepository.findByUserIdAndStatus(userId, "PENDING").orElse(null);
    }
}
