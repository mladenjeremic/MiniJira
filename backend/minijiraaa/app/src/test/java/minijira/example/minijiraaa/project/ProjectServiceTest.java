package minijira.example.minijiraaa.project;

import minijira.example.minijiraaa.application.access.ActivityAccess;
import minijira.example.minijiraaa.application.access.ProjectTaskAccess;
import minijira.example.minijiraaa.application.access.UserAccess;
import minijira.example.minijiraaa.application.dto.ActivityResponse;
import minijira.example.minijiraaa.application.dto.ProjectRequest;
import minijira.example.minijiraaa.application.dto.ProjectResponse;
import minijira.example.minijiraaa.application.service.ProjectService;
import minijira.example.minijiraaa.domain.enums.ActivityType;
import minijira.example.minijiraaa.domain.exception.ConflictException;
import minijira.example.minijiraaa.domain.exception.NotFoundException;
import minijira.example.minijiraaa.domain.model.Project;
import minijira.example.minijiraaa.domain.model.User;
import minijira.example.minijiraaa.domain.store.ProjectStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import minijira.example.minijiraaa.application.mapper.UserMapper;
import minijira.example.minijiraaa.application.mapper.UserMapperImpl;
import minijira.example.minijiraaa.application.mapper.ProjectMapper;
import minijira.example.minijiraaa.application.mapper.ProjectMapperImpl;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock ProjectStore projectStore;
    @Mock ProjectTaskAccess projectTaskAccess;
    @Mock UserAccess userAccess;
    @Mock ActivityAccess activityAccess;

    @Spy UserMapper userMapper = new UserMapperImpl();
    @Spy ProjectMapper projectMapper = new ProjectMapperImpl(userMapper);

    @InjectMocks ProjectService projectService;

    private User owner;
    private User bob;
    private Project project;

    private static User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@mail.com");
        return user;
    }

    @BeforeEach
    void setUp() {
        owner = user(1L, "Ana");
        bob = user(2L, "Bob");

        project = new Project();
        project.setId(10L);
        project.setName("MiniJira");
        project.setProjectKey("MJ");
        project.setOwner(owner);
        project.getParticipants().add(owner);
    }

    // ---------- create ----------

    @Test
    void create_setsOwnerAsParticipant_upperCasesKey_andTrimsFields() {
        ProjectRequest request = new ProjectRequest("  Web Shop ", "ws", " desc ");
        when(projectStore.existsByProjectKeyIgnoreCase("WS")).thenReturn(false);

        ProjectResponse response = projectService.create(request, owner);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectStore).save(captor.capture());
        Project saved = captor.getValue();

        assertThat(saved.getProjectKey()).isEqualTo("WS");
        assertThat(saved.getName()).isEqualTo("Web Shop");
        assertThat(saved.getDescription()).isEqualTo("desc");
        assertThat(saved.getOwner()).isSameAs(owner);
        assertThat(saved.getParticipants()).contains(owner);

        assertThat(response.key()).isEqualTo("WS");
        assertThat(response.taskCount()).isZero();
    }

    @Test
    void create_keepsDescriptionNullWhenNotProvided() {
        projectService.create(new ProjectRequest("X", "ab", null), owner);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectStore).save(captor.capture());

        assertThat(captor.getValue().getDescription()).isNull();
    }

    @Test
    void create_throwsConflictWhenKeyAlreadyUsed() {
        when(projectStore.existsByProjectKeyIgnoreCase("MJ")).thenReturn(true);

        assertThatThrownBy(() -> projectService.create(new ProjectRequest("X", "mj", null), owner))
                .isInstanceOf(ConflictException.class);

        verify(projectStore, never()).save(any());
    }

    // ---------- čitanje ----------

    @Test
    void getAll_returnsAccessibleProjectsWithTaskCount() {
        when(projectStore.findAccessibleByUserId(1L)).thenReturn(List.of(project));
        when(projectTaskAccess.countByProjectId(10L)).thenReturn(3L);

        List<ProjectResponse> result = projectService.getAll(owner);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).key()).isEqualTo("MJ");
        assertThat(result.get(0).taskCount()).isEqualTo(3);
    }

    @Test
    void getById_returnsProjectForMember() {
        when(projectStore.findById(10L)).thenReturn(Optional.of(project));
        when(projectStore.hasAccess(10L, 1L)).thenReturn(true);
        when(projectTaskAccess.countByProjectId(10L)).thenReturn(2L);

        ProjectResponse response = projectService.getById(10L, owner);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.taskCount()).isEqualTo(2);
    }

    @Test
    void getById_isDeniedForNonMember_andDoesNotCountTasks() {
        when(projectStore.findById(10L)).thenReturn(Optional.of(project));
        when(projectStore.hasAccess(10L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> projectService.getById(10L, bob))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(projectTaskAccess);
    }

    @Test
    void getHistory_returnsActivityForMember() {
        List<ActivityResponse> history = List.of(new ActivityResponse(
                1L, ActivityType.PROJECT_CREATED, "Ana created the project",
                null, null, null, null, null, null));

        when(projectStore.findById(10L)).thenReturn(Optional.of(project));
        when(projectStore.hasAccess(10L, 1L)).thenReturn(true);
        when(activityAccess.getForProject(10L)).thenReturn(history);

        assertThat(projectService.getHistory(10L, owner)).isSameAs(history);
    }

    @Test
    void getHistory_isDeniedForNonMember() {
        when(projectStore.findById(10L)).thenReturn(Optional.of(project));
        when(projectStore.hasAccess(10L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> projectService.getHistory(10L, bob))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(activityAccess);
    }

    // ---------- pristup ----------

    @Test
    void assertAccess_allowsMember() {
        when(projectStore.findById(10L)).thenReturn(Optional.of(project));
        when(projectStore.hasAccess(10L, 1L)).thenReturn(true);

        assertThatCode(() -> projectService.assertAccess(10L, owner))
                .doesNotThrowAnyException();
    }

    @Test
    void assertAccess_deniesNonMember() {
        when(projectStore.findById(10L)).thenReturn(Optional.of(project));
        when(projectStore.hasAccess(10L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> projectService.assertAccess(10L, bob))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assertAccess_deniesNullUser() {
        when(projectStore.findById(10L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.assertAccess(10L, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assertAccess_throwsNotFoundForMissingProject() {
        when(projectStore.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.assertAccess(99L, owner))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- addParticipant ----------

    @Test
    void addParticipant_ownerCanAddNewUser() {
        when(projectStore.findById(10L)).thenReturn(Optional.of(project));
        when(userAccess.findById(2L)).thenReturn(Optional.of(bob));
        when(projectStore.save(project)).thenReturn(project);
        when(projectTaskAccess.countByProjectId(10L)).thenReturn(0L);

        ProjectResponse response = projectService.addParticipant(10L, 2L, owner);

        assertThat(project.getParticipants()).contains(owner, bob);
        assertThat(response.participants()).hasSize(2);
    }

    @Test
    void addParticipant_onlyOwnerAllowed() {
        when(projectStore.findById(10L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.addParticipant(10L, 2L, bob))
                .isInstanceOf(AccessDeniedException.class);

        verify(projectStore, never()).save(any());
    }

    @Test
    void addParticipant_doesNotAddSameUserTwice() {
        project.getParticipants().add(bob);

        when(projectStore.findById(10L)).thenReturn(Optional.of(project));
        when(userAccess.findById(2L)).thenReturn(Optional.of(bob));
        when(projectStore.save(project)).thenReturn(project);
        when(projectTaskAccess.countByProjectId(10L)).thenReturn(0L);

        projectService.addParticipant(10L, 2L, owner);

        assertThat(project.getParticipants()).hasSize(2);
    }

    @Test
    void addParticipant_throwsNotFoundForUnknownUser() {
        when(projectStore.findById(10L)).thenReturn(Optional.of(project));
        when(userAccess.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.addParticipant(10L, 77L, owner))
                .isInstanceOf(NotFoundException.class);
    }
}