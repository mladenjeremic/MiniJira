package minijira.example.minijiraaa.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "Comment text is required")
        @Size(max = 2000, message = "Comment is too long") String text) {
}