package minijira.example.minijiraaa.application.dto;

import minijira.example.minijiraaa.domain.model.User;

public record ProjectMemberResponse(
        Long id,
        String name,
        String email
) {
    public static ProjectMemberResponse from(User user) {
        return new ProjectMemberResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}