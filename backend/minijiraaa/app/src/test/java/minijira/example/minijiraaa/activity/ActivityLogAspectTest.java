package minijira.example.minijiraaa.activity;

import minijira.example.minijiraaa.application.access.ActivityAccess;
import minijira.example.minijiraaa.application.access.ProjectAccess;
import minijira.example.minijiraaa.application.access.TaskAccess;
import minijira.example.minijiraaa.application.activity.ActivityLogAspect;
import minijira.example.minijiraaa.application.activity.LogActivity;
import minijira.example.minijiraaa.application.dto.ProjectResponse;
import minijira.example.minijiraaa.application.dto.TaskResponse;
import minijira.example.minijiraaa.domain.enums.ActivityType;
import minijira.example.minijiraaa.domain.enums.Priority;
import minijira.example.minijiraaa.domain.enums.Status;
import minijira.example.minijiraaa.domain.enums.Type;
import minijira.example.minijiraaa.domain.exception.ConflictException;
import minijira.example.minijiraaa.domain.model.Project;
import minijira.example.minijiraaa.domain.model.Task;
import minijira.example.minijiraaa.domain.model.User;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityLogAspectTest {

    @Mock ActivityAccess activityAccess;
    @Mock TaskAccess taskAccess;
    @Mock ProjectAccess projectAccess;
    @Mock TransactionTemplate transactionTemplate;

    @Mock ProceedingJoinPoint pjp;      // "pozvana metoda servisa"
    @Mock LogActivity logActivity;      // anotacija @LogActivity(...)

    @InjectMocks ActivityLogAspect aspect;

    private User owner;
    private User bob;
    private Project project;
    private Task task;

    @BeforeEach
    void setUp() {
        // TransactionTemplate samo odmah izvrši lambdu, bez prave transakcije
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(null);
        });

        owner = new User();
        owner.setId(1L);
        owner.setName("Ana");

        bob = new User();
        bob.setId(2L);
        bob.setName("Bob");

        project = new Project();
        project.setId(10L);
        project.setProjectKey("MJ");
        project.setOwner(owner);
        project.getParticipants().add(owner);

        task = new Task();
        task.setId(5L);
        task.setTaskNumber(5);
        task.setTitle("Old");
        task.setDescription("desc");
        task.setType(Type.TASK);
        task.setPriority(Priority.MEDIUM);
        task.setStatus(Status.TODO);
        task.setProject(project);
    }

    /** Aspekt čita samo prvi argument (id) i prvog User-a među argumentima (actor). */
    private Object run(ActivityType type, Object... args) {
        when(logActivity.value()).thenReturn(type);
        when(pjp.getArgs()).thenReturn(args);
        return aspect.around(pjp, logActivity);
    }

    // ---------- projekti ----------

    @Test
    void projectCreated_logsAfterServiceReturns() throws Throwable {
        ProjectResponse result = new ProjectResponse(10L, "MiniJira", "MJ", null, 0, null, List.of());
        when(pjp.proceed()).thenReturn(result);
        when(projectAccess.getEntityById(10L)).thenReturn(project);

        Object returned = run(ActivityType.PROJECT_CREATED, "request", owner);

        assertThat(returned).isSameAs(result);
        verify(activityAccess).log(project, owner, ActivityType.PROJECT_CREATED,
                null, null, null, null);
    }

    @Test
    void participantAdded_logsOnlyNewlyAddedParticipant() throws Throwable {
        when(projectAccess.getEntityById(10L)).thenReturn(project);
        when(pjp.proceed()).thenAnswer(inv -> {
            project.getParticipants().add(bob);   // servis je dodao Boba
            return null;
        });

        run(ActivityType.PARTICIPANT_ADDED, 10L, 2L, owner);

        verify(activityAccess).log(project, owner, ActivityType.PARTICIPANT_ADDED,
                null, null, null, "Bob");
        verifyNoMoreInteractions(activityAccess);
    }

    @Test
    void participantAdded_logsNothingWhenNobodyWasAdded() throws Throwable {
        when(projectAccess.getEntityById(10L)).thenReturn(project);

        run(ActivityType.PARTICIPANT_ADDED, 10L, 1L, owner);

        verifyNoInteractions(activityAccess);
    }

    // ---------- taskovi ----------

    @Test
    void taskCreated_logsKeyTakenFromServiceResult() throws Throwable {
        TaskResponse result = new TaskResponse(5L, "MJ-5", 5, "Fix", null,
                Status.TODO, Priority.LOW, Type.TASK, 10L, null, null);
        when(projectAccess.getEntityById(10L)).thenReturn(project);
        when(pjp.proceed()).thenReturn(result);

        run(ActivityType.TASK_CREATED, 10L, "request", owner);

        verify(activityAccess).log(project, owner, ActivityType.TASK_CREATED,
                "MJ-5", null, null, null);
    }

    @Test
    void taskUpdated_logsOnlyTheFieldThatChanged() throws Throwable {
        when(taskAccess.getEntityForUser(5L, owner)).thenReturn(task);
        when(pjp.proceed()).thenAnswer(inv -> {
            task.setStatus(Status.DONE);
            return null;
        });

        run(ActivityType.TASK_UPDATED, 5L, owner);

        verify(activityAccess).log(project, owner, ActivityType.TASK_UPDATED,
                "MJ-5", "status", "Todo", "Done");
        verifyNoMoreInteractions(activityAccess);
    }

    @Test
    void taskUpdated_logsEachChangedFieldSeparately() throws Throwable {
        when(taskAccess.getEntityForUser(5L, owner)).thenReturn(task);
        when(pjp.proceed()).thenAnswer(inv -> {
            task.setTitle("New");
            task.setPriority(Priority.HIGH);
            return null;
        });

        run(ActivityType.TASK_UPDATED, 5L, owner);

        verify(activityAccess).log(project, owner, ActivityType.TASK_UPDATED,
                "MJ-5", "title", "Old", "New");
        verify(activityAccess).log(project, owner, ActivityType.TASK_UPDATED,
                "MJ-5", "priority", "Medium", "High");
        verifyNoMoreInteractions(activityAccess);
    }

    @Test
    void taskUpdated_logsAssigneeChange_withUnassignedAsOldValue() throws Throwable {
        when(taskAccess.getEntityForUser(5L, owner)).thenReturn(task);
        when(pjp.proceed()).thenAnswer(inv -> {
            task.setAssignee(bob);
            return null;
        });

        run(ActivityType.TASK_UPDATED, 5L, owner);

        verify(activityAccess).log(project, owner, ActivityType.TASK_UPDATED,
                "MJ-5", "assignee", "Unassigned", "Bob");
    }

    @Test
    void taskUpdated_withNoRealChanges_logsNothing() throws Throwable {
        when(taskAccess.getEntityForUser(5L, owner)).thenReturn(task);
        when(pjp.proceed()).thenAnswer(inv -> {
            task.setDescription("desc   ");   // samo razmaci, ne računa se kao izmena
            return null;
        });

        run(ActivityType.TASK_UPDATED, 5L, owner);

        verifyNoInteractions(activityAccess);
    }

    @Test
    void taskDeleted_logsAfterServiceCall_usingKeyReadBeforeDeletion() throws Throwable {
        when(taskAccess.getEntityForUser(5L, owner)).thenReturn(task);

        run(ActivityType.TASK_DELETED, 5L, owner);

        InOrder order = inOrder(pjp, activityAccess);
        order.verify(pjp).proceed();
        order.verify(activityAccess).log(project, owner, ActivityType.TASK_DELETED,
                "MJ-5", null, null, null);
    }

    // ---------- komentari ----------

    @Test
    void commentAdded_logsOnTheTaskKey() throws Throwable {
        when(taskAccess.getEntityForUser(5L, owner)).thenReturn(task);

        run(ActivityType.COMMENT_ADDED, 5L, "tekst", null, owner);

        verify(activityAccess).log(project, owner, ActivityType.COMMENT_ADDED,
                "MJ-5", null, null, null);
    }

    // ---------- greške ----------

    @Test
    void whenServiceThrows_nothingIsLogged_andExceptionPropagates() throws Throwable {
        when(pjp.proceed()).thenThrow(new ConflictException("key exists"));

        assertThatThrownBy(() -> run(ActivityType.PROJECT_CREATED, "request", owner))
                .isInstanceOf(ConflictException.class);

        verifyNoInteractions(activityAccess);
    }

    @Test
    void checkedExceptionIsWrappedInIllegalStateException() throws Throwable {
        when(pjp.proceed()).thenThrow(new Exception("boom"));

        assertThatThrownBy(() -> run(ActivityType.PROJECT_CREATED, "request", owner))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(Exception.class);
    }
}