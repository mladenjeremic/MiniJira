package minijira.example.minijiraaa.application.dto;

import minijira.example.minijiraaa.domain.enums.ActivityType;
import minijira.example.minijiraaa.domain.model.ActivityLog;

import java.time.Instant;

public record ActivityResponse(
        Long id,
        ActivityType type,
        String message,
        String taskKey,
        String field,
        String oldValue,
        String newValue,
        Instant createdAt,
        UserResponse actor
) {

    public static ActivityResponse from(ActivityLog entry) {
        return new ActivityResponse(
                entry.getId(),
                entry.getType(),
                buildMessage(entry),
                entry.getTaskKey(),
                entry.getFieldName(),
                entry.getOldValue(),
                entry.getNewValue(),
                entry.getCreatedAt(),
                UserResponse.from(entry.getActor())
        );
    }

    private static String buildMessage(ActivityLog entry) {
        String who = entry.getActor() != null ? entry.getActor().getName() : "Someone";
        String task = entry.getTaskKey();

        return switch (entry.getType()) {
            case PROJECT_CREATED -> who + " created the project";
            case PARTICIPANT_ADDED -> who + " added " + entry.getNewValue() + " to the project";
            case TASK_CREATED -> who + " created " + task;
            case TASK_DELETED -> who + " deleted " + task;
            case TASK_UPDATED -> updatedMessage(who, entry);
            case COMMENT_ADDED -> who + " commented on " + task;
            case COMMENT_UPDATED -> who + " edited a comment on " + task;
            case COMMENT_DELETED -> who + " deleted a comment on " + task;
        };
    }

    private static String updatedMessage(String who, ActivityLog entry) {
        String base = who + " changed the " + entry.getFieldName() + " of " + entry.getTaskKey();

        // Za opis ne čuvamo tekst, pa nema "from ... to ..."
        if (entry.getOldValue() == null && entry.getNewValue() == null) {
            return base;
        }

        return base + " from " + valueOrNone(entry.getOldValue())
                + " to " + valueOrNone(entry.getNewValue());
    }

    private static String valueOrNone(String value) {
        return (value == null || value.isBlank()) ? "none" : value;
    }
}