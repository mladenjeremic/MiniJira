package minijira.example.minijiraaa.application.dto;

import minijira.example.minijiraaa.domain.enums.Priority;
import minijira.example.minijiraaa.domain.enums.Status;
import minijira.example.minijiraaa.domain.enums.Type;
import minijira.example.minijiraaa.domain.model.Task;

import java.time.Instant;

public record TaskResponse(
        Long id,
        String key,
        int number,
        String title,
        String description,
        Status status,
        Priority priority,
        Type type,
        Long projectId,
        UserResponse assignee,
        Instant createdAt) {

    public static TaskResponse from(Task task) {
        String key = task.getProject().getProjectKey() + "-" + task.getTaskNumber();
        return new TaskResponse(
                task.getId(),
                key,
                task.getTaskNumber(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getType(),
                task.getProject().getId(),
                UserResponse.from(task.getAssignee()),
                task.getCreatedAt());
    }
}