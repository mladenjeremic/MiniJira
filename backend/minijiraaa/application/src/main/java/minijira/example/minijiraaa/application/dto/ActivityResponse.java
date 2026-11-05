package minijira.example.minijiraaa.application.dto;

import minijira.example.minijiraaa.domain.enums.ActivityType;

import java.time.Instant;

public record ActivityResponse(
        Long id,
        ActivityType type,
        String message,
        String taskKey,
        String field,
        String oldValue,
        String newValue,
        Instant createdAt,
        UserResponse actor
) {
}