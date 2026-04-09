package com.demo.app.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberProfile {

    private Long id;
    private Long userId;
    private Long tierId;
    private int totalSpend;
    private String phoneMasked;
    private LocalDateTime joinedAt;
}
