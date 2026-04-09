package com.demo.app.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberTier {

    private Long id;
    private String name;
    private int rank;
    private int minSpend;
    private Integer maxSpend;
}
