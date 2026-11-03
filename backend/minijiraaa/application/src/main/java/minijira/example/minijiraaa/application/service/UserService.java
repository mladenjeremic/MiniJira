package minijira.example.minijiraaa.application.service;

import lombok.RequiredArgsConstructor;
import minijira.example.minijiraaa.application.access.UserAccess;
import minijira.example.minijiraaa.application.dto.UserResponse;
import minijira.example.minijiraaa.domain.exception.NotFoundException;
import minijira.example.minijiraaa.domain.model.User;
import minijira.example.minijiraaa.domain.store.UserStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements UserAccess {

    private final UserStore userStore;

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long userId) {
        return userStore.findById(userId);
    }

    @Transactional(readOnly = true)
    public User getEntityById(Long id) {
        return userStore.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "User with id " + id + " not found."
                        )
                );
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userStore.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }
}