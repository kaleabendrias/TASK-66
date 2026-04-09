package com.demo.app.api.dto;

public record MemberTierDto(Long id, String name, int rank, int minSpend, Integer maxSpend) {
}
