package minijira.example.minijiraaa.application.dto;


import minijira.example.minijiraaa.domain.model.User;

public record UserResponse(Long id, String name, String email) {
    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}