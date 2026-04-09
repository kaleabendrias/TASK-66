package com.demo.app.persistence.entity;

import com.demo.app.domain.model.MemberProfile;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tier_id", nullable = false)
    private Long tierId;

    @Column(name = "total_spend", nullable = false)
    private int totalSpend;

    @Column(name = "phone_encrypted")
    private String phoneEncrypted;

    @Column(name = "phone_masked")
    private String phoneMasked;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public MemberProfile toModel() {
        return MemberProfile.builder()
                .id(id)
                .userId(userId)
                .tierId(tierId)
                .totalSpend(totalSpend)
                .phoneMasked(phoneMasked)
                .joinedAt(joinedAt)
                .build();
    }
}
