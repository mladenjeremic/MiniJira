package minijira.example.minijiraaa.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import minijira.example.minijiraaa.domain.enums.Priority;
import minijira.example.minijiraaa.domain.enums.Status;
import minijira.example.minijiraaa.domain.enums.Type;

public record TaskRequest(
        @NotBlank(message = "Title is required") String title,
        String description,
        @NotNull(message = "Type is required") Type type,
        @NotNull(message = "Priority is required") Priority priority,
        @NotNull(message = "Status is required") Status status,
        Long assigneeId) {
}