package minijira.example.minijiraaa.task;

import minijira.example.minijiraaa.application.access.ProjectAccess;
import minijira.example.minijiraaa.application.access.UserAccess;
import minijira.example.minijiraaa.application.dto.TaskRequest;
import minijira.example.minijiraaa.application.dto.TaskResponse;
import minijira.example.minijiraaa.application.service.TaskService;
import minijira.example.minijiraaa.domain.enums.Priority;
import minijira.example.minijiraaa.domain.enums.Status;
import minijira.example.minijiraaa.domain.enums.Type;
import minijira.example.minijiraaa.domain.exception.NotFoundException;
import minijira.example.minijiraaa.domain.model.Project;
import minijira.example.minijiraaa.domain.model.Task;
import minijira.example.minijiraaa.domain.model.User;
import minijira.example.minijiraaa.domain.store.TaskStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import minijira.example.minijiraaa.application.mapper.UserMapper;
import minijira.example.minijiraaa.application.mapper.UserMapperImpl;
import minijira.example.minijiraaa.application.mapper.TaskMapper;
import minijira.example.minijiraaa.application.mapper.TaskMapperImpl;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskStore taskStore;
    @Mock ProjectAccess projectAccess;
    @Mock UserAccess userAccess;

    @Spy UserMapper userMapper = new UserMapperImpl();
    @Spy TaskMapper taskMapper = new TaskMapperImpl(userMapper);

    @InjectMocks TaskService taskService;

    private User owner;
    private User bob;
    private Project project;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setName("Ana");

        bob = new User();
        bob.setId(2L);
        bob.setName("Bob");

        project = new Project();
        project.setId(1L);
        project.setProjectKey("MJ");
        project.setOwner(owner);
        project.getParticipants().add(owner);
    }

    private Task existingTask() {
        Task task = new Task();
        task.setId(5L);
        task.setTaskNumber(5);
        task.setTitle("Old title");
        task.setDescription("desc");
        task.setType(Type.TASK);
        task.setPriority(Priority.MEDIUM);
        task.setStatus(Status.TODO);
        task.setProject(project);
        return task;
    }

    private static TaskRequest request(String title, Long assigneeId) {
        return new TaskRequest(title, "desc", Type.TASK, Priority.MEDIUM, Status.TODO, assigneeId);
    }

    // ---------- create ----------

    @Test
    void create_givesNextTaskNumber_trimsTitle_andChecksAccess() {
        TaskRequest request = new TaskRequest(
                "  Fix bug ", "desc", Type.BUG, Priority.HIGH, Status.TODO, null);

        when(projectAccess.getEntityById(1L)).thenReturn(project);
        when(taskStore.findMaxTaskNumber(1L)).thenReturn(4);
        when(taskStore.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response = taskService.create(1L, request, owner);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskStore).save(captor.capture());
        Task saved = captor.getValue();

        assertThat(saved.getTaskNumber()).isEqualTo(5);      // 4 + 1
        assertThat(saved.getTitle()).isEqualTo("Fix bug");   // trim
        assertThat(saved.getProject()).isSameAs(project);

        assertThat(response.key()).isEqualTo("MJ-5");
        verify(projectAccess).assertAccess(1L, owner);
    }

    @Test
    void create_isRejectedWhenUserHasNoAccessToProject() {
        User stranger = new User();
        stranger.setId(9L);

        doThrow(new AccessDeniedException("no access"))
                .when(projectAccess).assertAccess(1L, stranger);

        assertThatThrownBy(() -> taskService.create(1L, request("T", null), stranger))
                .isInstanceOf(AccessDeniedException.class);

        verify(taskStore, never()).save(any());
    }

    @Test
    void create_withProjectMemberAsAssignee_setsAssignee() {
        project.getParticipants().add(bob);

        when(projectAccess.getEntityById(1L)).thenReturn(project);
        when(userAccess.findById(2L)).thenReturn(Optional.of(bob));
        when(taskStore.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response = taskService.create(1L, request("T", 2L), owner);

        assertThat(response.assignee().id()).isEqualTo(2L);
    }

    @Test
    void create_rejectsAssigneeWhoIsNotProjectMember() {
        when(projectAccess.getEntityById(1L)).thenReturn(project);

        assertThatThrownBy(() -> taskService.create(1L, request("T", 99L), owner))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(
                        ((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));

        verify(taskStore, never()).save(any());
    }

    @Test
    void create_throwsNotFoundWhenAssigneeUserDoesNotExist() {
        project.getParticipants().add(bob);

        when(projectAccess.getEntityById(1L)).thenReturn(project);
        when(userAccess.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.create(1L, request("T", 2L), owner))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- update ----------

    @Test
    void update_changesAllFieldsAndTrimsTitle() {
        Task task = existingTask();
        when(taskStore.findById(5L)).thenReturn(Optional.of(task));
        when(taskStore.save(task)).thenReturn(task);

        TaskRequest request = new TaskRequest(
                "  New title ", "new desc", Type.BUG, Priority.HIGH, Status.DONE, null);

        TaskResponse response = taskService.update(5L, request, owner);

        assertThat(task.getTitle()).isEqualTo("New title");
        assertThat(task.getDescription()).isEqualTo("new desc");
        assertThat(task.getType()).isEqualTo(Type.BUG);
        assertThat(task.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(task.getStatus()).isEqualTo(Status.DONE);
        assertThat(response.status()).isEqualTo(Status.DONE);
        verify(projectAccess).assertAccess(1L, owner);
    }

    @Test
    void update_keepsAssigneeWithoutLookupWhenUnchanged() {
        Task task = existingTask();
        task.setAssignee(bob);

        when(taskStore.findById(5L)).thenReturn(Optional.of(task));
        when(taskStore.save(task)).thenReturn(task);

        taskService.update(5L, request("Old title", 2L), owner);

        assertThat(task.getAssignee()).isSameAs(bob);
        verifyNoInteractions(userAccess);
    }

    @Test
    void update_removesAssigneeWhenIdIsNull() {
        Task task = existingTask();
        task.setAssignee(bob);

        when(taskStore.findById(5L)).thenReturn(Optional.of(task));
        when(taskStore.save(task)).thenReturn(task);

        taskService.update(5L, request("Old title", null), owner);

        assertThat(task.getAssignee()).isNull();
    }

    // ---------- updateStatus (drag & drop) ----------

    @Test
    void updateStatus_changesStatus() {
        Task task = existingTask();
        when(taskStore.findById(5L)).thenReturn(Optional.of(task));
        when(taskStore.save(task)).thenReturn(task);

        TaskResponse response = taskService.updateStatus(5L, Status.IN_PROGRESS, owner);

        assertThat(response.status()).isEqualTo(Status.IN_PROGRESS);
        assertThat(task.getStatus()).isEqualTo(Status.IN_PROGRESS);
    }

    // ---------- delete ----------

    @Test
    void delete_removesTask() {
        Task task = existingTask();
        when(taskStore.findById(5L)).thenReturn(Optional.of(task));

        taskService.delete(5L, owner);

        verify(taskStore).delete(task);
    }

    // ---------- pristup i greške ----------

    @Test
    void getEntityForUser_throwsNotFoundForUnknownTask() {
        when(taskStore.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getEntityForUser(404L, owner))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getEntityForUser_checksProjectAccess() {
        Task task = existingTask();
        User stranger = new User();
        stranger.setId(9L);

        when(taskStore.findById(5L)).thenReturn(Optional.of(task));
        doThrow(new AccessDeniedException("no access"))
                .when(projectAccess).assertAccess(1L, stranger);

        assertThatThrownBy(() -> taskService.getEntityForUser(5L, stranger))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---------- getForProject ----------

    @Test
    void getForProject_neverTouchesTheStoreWithoutAccess() {
        User stranger = new User();
        stranger.setId(9L);

        doThrow(new AccessDeniedException("no access"))
                .when(projectAccess).assertAccess(1L, stranger);

        assertThatThrownBy(() -> taskService.getForProject(
                1L, null, null, null, null, null, null, null, stranger))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(taskStore);
    }

    @Test
    void getForProject_returnsMappedTasks() {
        when(taskStore.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(existingTask()));

        List<TaskResponse> result = taskService.getForProject(
                1L, null, null, null, null, null, null, null, owner);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).key()).isEqualTo("MJ-5");
    }

    @ParameterizedTest
    @CsvSource({
            ",,taskNumber,ASC",
            "key,desc,taskNumber,DESC",
            "title,asc,title,ASC",
            "assignee,DESC,assignee.name,DESC",
            "nepoznato,,taskNumber,ASC"
    })
    void getForProject_buildsSortFromParameters(
            String sortBy, String dir, String property, Sort.Direction direction) {

        taskService.getForProject(1L, null, null, null, null, null, sortBy, dir, owner);

        ArgumentCaptor<Sort> captor = ArgumentCaptor.forClass(Sort.class);
        verify(taskStore).findAll(any(Specification.class), captor.capture());

        Sort.Order order = captor.getValue().iterator().next();
        assertThat(order.getProperty()).isEqualTo(property);
        assertThat(order.getDirection()).isEqualTo(direction);
    }
}