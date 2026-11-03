package minijira.example.minijiraaa.application.dto;

import jakarta.validation.constraints.NotNull;
import minijira.example.minijiraaa.domain.enums.Status;

public record StatusRequest(@NotNull(message = "Status is required") Status status) {
}