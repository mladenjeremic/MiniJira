package minijira.example.minijiraaa.user;

import minijira.example.minijiraaa.application.dto.UserResponse;
import minijira.example.minijiraaa.application.service.UserService;
import minijira.example.minijiraaa.domain.exception.NotFoundException;
import minijira.example.minijiraaa.domain.model.User;
import minijira.example.minijiraaa.domain.store.UserStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserStore userStore;

    @InjectMocks UserService userService;

    private static User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@mail.com");
        return user;
    }

    @Test
    void findById_returnsWhatStoreReturns() {
        User ana = user(1L, "Ana");
        when(userStore.findById(1L)).thenReturn(Optional.of(ana));

        assertThat(userService.findById(1L)).containsSame(ana);
    }

    @Test
    void findById_returnsEmptyForUnknownUser() {
        when(userStore.findById(9L)).thenReturn(Optional.empty());

        assertThat(userService.findById(9L)).isEmpty();
    }

    @Test
    void getEntityById_throwsNotFoundForUnknownUser() {
        when(userStore.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getEntityById(9L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getById_mapsUserToResponse() {
        when(userStore.findById(1L)).thenReturn(Optional.of(user(1L, "Ana")));

        UserResponse response = userService.getById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Ana");
        assertThat(response.email()).isEqualTo("ana@mail.com");
    }

    @Test
    void getAll_mapsAllUsers() {
        when(userStore.findAll()).thenReturn(List.of(user(1L, "Ana"), user(2L, "Bob")));

        List<UserResponse> result = userService.getAll();

        assertThat(result).extracting(UserResponse::name).containsExactly("Ana", "Bob");
    }
}