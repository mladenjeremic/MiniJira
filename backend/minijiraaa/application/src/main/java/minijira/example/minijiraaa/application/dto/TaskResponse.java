package minijira.example.minijiraaa.application.dto;

import minijira.example.minijiraaa.domain.enums.Priority;
import minijira.example.minijiraaa.domain.enums.Status;
import minijira.example.minijiraaa.domain.enums.Type;

import java.time.Instant;

public record TaskResponse(
        Long id,
        String key,
        int number,
        String title,
        String description,
        Status status,
        Priority priority,
        Type type,
        Long projectId,
        UserResponse assignee,
        Instant createdAt) {
}