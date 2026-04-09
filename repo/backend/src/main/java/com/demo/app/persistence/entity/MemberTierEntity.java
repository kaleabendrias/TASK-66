package com.demo.app.persistence.entity;

import com.demo.app.domain.model.MemberTier;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_tier")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberTierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "rank", nullable = false)
    private int rank;

    @Column(name = "min_spend", nullable = false)
    private int minSpend;

    @Column(name = "max_spend")
    private Integer maxSpend;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public MemberTier toModel() {
        return MemberTier.builder()
                .id(id)
                .name(name)
                .rank(rank)
                .minSpend(minSpend)
                .maxSpend(maxSpend)
                .build();
    }
}
