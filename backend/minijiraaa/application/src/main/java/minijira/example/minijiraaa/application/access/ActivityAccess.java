package minijira.example.minijiraaa.application.access;

import minijira.example.minijiraaa.application.dto.ActivityResponse;
import minijira.example.minijiraaa.domain.enums.ActivityType;
import minijira.example.minijiraaa.domain.model.Project;
import minijira.example.minijiraaa.domain.model.Task;
import minijira.example.minijiraaa.domain.model.User;

import java.util.List;

public interface ActivityAccess {

    void log(
            Project project,
            User actor,
            ActivityType type,
            String taskKey,
            String fieldName,
            String oldValue,
            String newValue
    );

    List<ActivityResponse> getForProject(Long projectId);

    /** MJ-3 */
    static String keyOf(Task task) {
        if (task == null) {
            return null;
        }

        return task.getProject().getProjectKey() + "-"
                + task.getTaskNumber();
    }

    /** IN_PROGRESS -> In Progress */
    static String label(Enum<?> value) {
        if (value == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();

        for (String word : value.name().split("_")) {
            if (result.length() > 0) {
                result.append(" ");
            }

            result.append(word.charAt(0))
                    .append(word.substring(1).toLowerCase());
        }

        return result.toString();
    }
}