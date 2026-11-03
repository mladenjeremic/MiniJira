package minijira.example.minijiraaa.domain.store;

import minijira.example.minijiraaa.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserStore {

    Optional<User> findById(Long userId);

    List<User> findAll();

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    User save(User user);
}