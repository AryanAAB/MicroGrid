package com.example.backend.dto;

public record DashboardStatsDto(
        Double energySold,
        Double energyConsumed,
        Double totalEarnings,
        Double gridSavings
) {}
