package minijira.example.minijiraaa.application.dto;

import java.time.Instant;

public record CommentResponse(
        Long id,
        String text,
        Instant createdAt,
        UserResponse author,
        boolean hasImage
) {
}