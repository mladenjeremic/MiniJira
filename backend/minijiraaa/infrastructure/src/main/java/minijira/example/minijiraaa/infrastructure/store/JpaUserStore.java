package minijira.example.minijiraaa.infrastructure.store;

import lombok.RequiredArgsConstructor;
import minijira.example.minijiraaa.domain.model.User;
import minijira.example.minijiraaa.domain.store.UserStore;
import minijira.example.minijiraaa.infrastructure.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaUserStore implements UserStore {

    private final UserRepository userRepository;

    @Override
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll(Sort.by("name"));
    }

    @Override
    public Optional<User> findByEmailIgnoreCase(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    @Override
    public boolean existsByEmailIgnoreCase(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }
}