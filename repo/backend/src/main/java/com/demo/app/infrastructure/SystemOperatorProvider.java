package com.demo.app.infrastructure;

import com.demo.app.domain.enums.Role;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves the user id used as the "system" actor when no human user is on the
 * call stack — currently the seeded administrator. The first lookup is cached
 * because the system operator's identity does not change at runtime, and any
 * code path that needs it (scheduled jobs, internal compensation flows) calls
 * it on every event.
 *
 * <p>The {@code inventory_movement.operator_id} column is {@code NOT NULL
 * REFERENCES app_user(id)} (see V5__warehouse_inventory.sql), so callers like
 * {@link com.demo.app.application.service.ReservationService#expireOverdueReservations()}
 * must always provide a real user id even when the action is fully automated.
 */
@Component
@RequiredArgsConstructor
public class SystemOperatorProvider {

    private final UserRepository userRepository;
    private volatile Long cachedId;

    public Long getSystemOperatorId() {
        Long id = cachedId;
        if (id != null && userRepository.existsById(id)) {
            return id;
        }
        synchronized (this) {
            if (cachedId != null && userRepository.existsById(cachedId)) {
                return cachedId;
            }
            cachedId = userRepository.findFirstByRole(Role.ADMINISTRATOR)
                    .map(UserEntity::getId)
                    .orElseThrow(() -> new IllegalStateException(
                            "No ADMINISTRATOR user available to act as system operator"));
            return cachedId;
        }
    }
}

