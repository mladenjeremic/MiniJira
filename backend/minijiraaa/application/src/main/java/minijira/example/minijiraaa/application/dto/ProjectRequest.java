package minijira.example.minijiraaa.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProjectRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Key is required")
        @Pattern(regexp = "^[A-Za-z0-9]{2,10}$", message = "Key must be 2-10 letters or digits") String key,
        String description) {
}