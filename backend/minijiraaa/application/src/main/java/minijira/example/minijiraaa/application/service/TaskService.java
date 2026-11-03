package minijira.example.minijiraaa.application.service;

import lombok.RequiredArgsConstructor;
import minijira.example.minijiraaa.application.access.ProjectAccess;
import minijira.example.minijiraaa.application.access.TaskAccess;
import minijira.example.minijiraaa.application.access.UserAccess;
import minijira.example.minijiraaa.application.activity.LogActivity;
import minijira.example.minijiraaa.application.dto.TaskRequest;
import minijira.example.minijiraaa.application.dto.TaskResponse;
import minijira.example.minijiraaa.domain.enums.ActivityType;
import minijira.example.minijiraaa.domain.enums.Priority;
import minijira.example.minijiraaa.domain.enums.Status;
import minijira.example.minijiraaa.domain.enums.Type;
import minijira.example.minijiraaa.domain.exception.NotFoundException;
import minijira.example.minijiraaa.domain.model.Project;
import minijira.example.minijiraaa.domain.model.Task;
import minijira.example.minijiraaa.domain.model.User;
import minijira.example.minijiraaa.domain.store.TaskStore;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService implements TaskAccess {

    private final TaskStore taskStore;
    private final ProjectAccess projectAccess;
    private final UserAccess userAccess;

    @Transactional(readOnly = true)
    public List<TaskResponse> getForProject(
            Long projectId,
            String q,
            Long assigneeId,
            Priority priority,
            Type type,
            Status status,
            String sortBy,
            String dir,
            User currentUser
    ) {
        projectAccess.assertAccess(projectId, currentUser);

        Specification<Task> spec = (root, query, cb) ->
                cb.equal(root.get("project").get("id"), projectId);

        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("title")), pattern));
        }

        if (assigneeId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("assignee").get("id"), assigneeId));
        }

        if (priority != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("priority"), priority));
        }

        if (type != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("type"), type));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), status));
        }

        return taskStore.findAll(spec, buildSort(sortBy, dir))
                .stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(Long id, User currentUser) {
        return TaskResponse.from(getEntityForUser(id, currentUser));
    }

    @LogActivity(ActivityType.TASK_CREATED)
    @Transactional
    public TaskResponse create(
            Long projectId,
            TaskRequest request,
            User currentUser
    ) {
        projectAccess.assertAccess(projectId, currentUser);
        Project project = projectAccess.getEntityById(projectId);

        Task task = new Task();
        task.setProject(project);
        task.setTaskNumber(taskStore.findMaxTaskNumber(projectId) + 1);

        apply(task, request);

        return TaskResponse.from(taskStore.save(task));
    }

    @LogActivity(ActivityType.TASK_UPDATED)
    @Transactional
    public TaskResponse update(
            Long id,
            TaskRequest request,
            User currentUser
    ) {
        Task task = getEntityForUser(id, currentUser);

        apply(task, request);

        return TaskResponse.from(taskStore.save(task));
    }

    @LogActivity(ActivityType.TASK_UPDATED)
    @Transactional
    public TaskResponse updateStatus(
            Long id,
            Status status,
            User currentUser
    ) {
        Task task = getEntityForUser(id, currentUser);

        task.setStatus(status);

        return TaskResponse.from(taskStore.save(task));
    }

    @LogActivity(ActivityType.TASK_DELETED)
    @Transactional
    public void delete(Long id, User currentUser) {
        Task task = getEntityForUser(id, currentUser);

        taskStore.delete(task);
    }

    @Override
    @Transactional(readOnly = true)
    public Task getEntityForUser(Long id, User user) {
        Task task = getEntityById(id);
        projectAccess.assertAccess(task.getProject().getId(), user);
        return task;
    }

    private Task getEntityById(Long id) {
        return taskStore.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Task with id " + id + " not found."
                        )
                );
    }

    private void apply(Task task, TaskRequest request) {
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setType(request.type());
        task.setPriority(request.priority());
        task.setStatus(request.status());

        Long assigneeId = request.assigneeId();

        if (assigneeId == null) {
            task.setAssignee(null);
            return;
        }

        boolean unchanged = task.getAssignee() != null
                && task.getAssignee().getId().equals(assigneeId);

        if (unchanged) {
            return;
        }

        Project project = task.getProject();

        boolean isMember = project.getOwner().getId().equals(assigneeId)
                || project.getParticipants().stream()
                .anyMatch(member -> member.getId().equals(assigneeId));

        if (!isMember) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Assignee must be a member of the project."
            );
        }

        User assignee = userAccess.findById(assigneeId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "User with id " + assigneeId + " not found."
                        )
                );

        task.setAssignee(assignee);
    }

    private Sort buildSort(String sortBy, String dir) {
        String property = switch (sortBy == null ? "key" : sortBy) {
            case "title" -> "title";
            case "type" -> "type";
            case "status" -> "status";
            case "priority" -> "priority";
            case "assignee" -> "assignee.name";
            case "createdAt" -> "createdAt";
            default -> "taskNumber";
        };

        Sort.Direction direction = "desc".equalsIgnoreCase(dir)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return Sort.by(direction, property);
    }
}