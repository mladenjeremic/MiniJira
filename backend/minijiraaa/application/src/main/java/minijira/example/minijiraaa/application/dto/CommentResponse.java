package minijira.example.minijiraaa.application.dto;

import minijira.example.minijiraaa.domain.model.Comment;

import java.time.Instant;

public record CommentResponse(
        Long id,
        String text,
        Instant createdAt,
        UserResponse author,
        boolean hasImage // DODATO
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getText(),
                comment.getCreatedAt(),
                UserResponse.from(comment.getAuthor()),
                comment.getImageFileName() != null // DODATO
        );
    }
}