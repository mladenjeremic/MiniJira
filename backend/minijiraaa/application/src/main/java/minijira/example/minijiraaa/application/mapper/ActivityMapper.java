package minijira.example.minijiraaa.application.mapper;

import minijira.example.minijiraaa.application.dto.ActivityResponse;
import minijira.example.minijiraaa.domain.model.ActivityLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapStructConfig.class, uses = UserMapper.class)
public interface ActivityMapper {

    @Mapping(target = "message", expression = "java(buildMessage(entry))")
    @Mapping(target = "field", source = "fieldName")
    ActivityResponse toResponse(ActivityLog entry);

    List<ActivityResponse> toResponseList(List<ActivityLog> entries);

    default String buildMessage(ActivityLog entry) {
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