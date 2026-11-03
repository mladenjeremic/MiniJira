package minijira.example.minijiraaa.application.activity;

import lombok.RequiredArgsConstructor;
import minijira.example.minijiraaa.application.access.ActivityAccess;
import minijira.example.minijiraaa.application.dto.ProjectResponse;
import minijira.example.minijiraaa.application.dto.TaskResponse;
import minijira.example.minijiraaa.domain.enums.ActivityType;
import minijira.example.minijiraaa.domain.model.Project;
import minijira.example.minijiraaa.domain.model.Task;
import minijira.example.minijiraaa.domain.model.User;
import minijira.example.minijiraaa.application.access.ProjectAccess;
import minijira.example.minijiraaa.application.access.TaskAccess;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Aspect
@Component
@RequiredArgsConstructor
public class ActivityLogAspect {

    private final ActivityAccess activityAccess;
    private final TaskAccess taskAccess;
    private final ProjectAccess projectAccess;
    private final TransactionTemplate transactionTemplate;

    @Around("@annotation(logActivity)")
    public Object around(ProceedingJoinPoint pjp, LogActivity logActivity) {
        ActivityType type = logActivity.value();
        User actor = actorFrom(pjp.getArgs());

        return transactionTemplate.execute(status -> {
            try {
                return switch (type) {
                    case PROJECT_CREATED -> projectCreated(pjp, actor);
                    case PARTICIPANT_ADDED -> participantAdded(pjp, actor);
                    case TASK_CREATED -> taskCreated(pjp, actor);
                    case TASK_UPDATED -> taskUpdated(pjp, actor);
                    case TASK_DELETED, COMMENT_ADDED, COMMENT_UPDATED, COMMENT_DELETED ->
                            onExistingTask(pjp, type, actor);
                };
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable t) {
                throw new IllegalStateException(t);
            }
        });
    }

    // ---------- projekti ----------

    private Object projectCreated(ProceedingJoinPoint pjp, User actor) throws Throwable {
        Object result = pjp.proceed();

        Project project = projectAccess.getEntityById(((ProjectResponse) result).id());
        activityAccess.log(project, actor, ActivityType.PROJECT_CREATED,
                null, null, null, null);

        return result;
    }

    private Object participantAdded(ProceedingJoinPoint pjp, User actor) throws Throwable {
        Project project = projectAccess.getEntityById((Long) pjp.getArgs()[0]);

        Set<Long> before = new HashSet<>();
        project.getParticipants().forEach(user -> before.add(user.getId()));

        Object result = pjp.proceed();

        for (User participant : project.getParticipants()) {
            if (!before.contains(participant.getId())) {
                activityAccess.log(project, actor, ActivityType.PARTICIPANT_ADDED,
                        null, null, null, participant.getName());
            }
        }

        return result;
    }

    // ---------- taskovi ----------

    private Object taskCreated(ProceedingJoinPoint pjp, User actor) throws Throwable {
        Project project = projectAccess.getEntityById((Long) pjp.getArgs()[0]);

        Object result = pjp.proceed();

        activityAccess.log(project, actor, ActivityType.TASK_CREATED,
                ((TaskResponse) result).key(), null, null, null);

        return result;
    }

    private Object taskUpdated(ProceedingJoinPoint pjp, User actor) throws Throwable {
        Task task = taskAccess.getEntityForUser((Long) pjp.getArgs()[0], actor);

        Snapshot before = Snapshot.of(task);
        Object result = pjp.proceed();
        Snapshot after = Snapshot.of(task);

        logChange(task, actor, "title", before.title, after.title);

        if (!before.description.equals(after.description)) {
            logChange(task, actor, "description",
                    before.rawDescription, after.rawDescription);
        }

        logChange(task, actor, "type", before.type, after.type);
        logChange(task, actor, "priority", before.priority, after.priority);
        logChange(task, actor, "status", before.status, after.status);

        if (!Objects.equals(before.assigneeId, after.assigneeId)) {
            logChange(task, actor, "assignee", before.assigneeName, after.assigneeName);
        }

        return result;
    }

    private Object onExistingTask(
            ProceedingJoinPoint pjp, ActivityType type, User actor
    ) throws Throwable {
        Task task = taskAccess.getEntityForUser((Long) pjp.getArgs()[0], actor);
        Project project = task.getProject();
        String key = ActivityAccess.keyOf(task);

        Object result = pjp.proceed();

        activityAccess.log(project, actor, type, key, null, null, null);

        return result;
    }

    // ---------- pomoćne ----------

    private void logChange(Task task, User actor, String field,
                           String oldValue, String newValue) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }

        activityAccess.log(task.getProject(), actor, ActivityType.TASK_UPDATED,
                ActivityAccess.keyOf(task), field, oldValue, newValue);
    }

    private static User actorFrom(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof User user) {
                return user;
            }
        }
        return null;
    }

    private record Snapshot(
            String title,
            String rawDescription,
            String description,
            String type,
            String priority,
            String status,
            Long assigneeId,
            String assigneeName
    ) {
        static Snapshot of(Task task) {
            User assignee = task.getAssignee();

            return new Snapshot(
                    task.getTitle(),
                    task.getDescription(),
                    task.getDescription() == null ? "" : task.getDescription().trim(),
                    ActivityAccess.label(task.getType()),
                    ActivityAccess.label(task.getPriority()),
                    ActivityAccess.label(task.getStatus()),
                    assignee == null ? null : assignee.getId(),
                    assignee == null ? "Unassigned" : assignee.getName()
            );
        }
    }
}