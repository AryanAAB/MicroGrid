package com.example.backend.dto;

public record UserProfileDto(
        String houseName,
        String name,
        String phone,
        Double latitude,
        Double longitude
) {}
