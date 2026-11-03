package minijira.example.minijiraaa.application.access;

import minijira.example.minijiraaa.domain.model.User;

import java.util.Optional;

public interface UserAccess {

    Optional<User> findById(Long userId);
}
