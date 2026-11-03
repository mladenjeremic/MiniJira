package minijira.example.minijiraaa.application.service;

import lombok.RequiredArgsConstructor;
import minijira.example.minijiraaa.application.access.ActivityAccess;
import minijira.example.minijiraaa.domain.store.ActivityLogStore;
import minijira.example.minijiraaa.application.dto.ActivityResponse;
import minijira.example.minijiraaa.domain.enums.ActivityType;
import minijira.example.minijiraaa.domain.model.ActivityLog;
import minijira.example.minijiraaa.domain.model.Project;
import minijira.example.minijiraaa.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityLogService implements ActivityAccess {

    private static final int MAX_VALUE_LENGTH = 500;

    private final ActivityLogStore activityLogStore;

    @Override
    @Transactional
    public void log(
            Project project,
            User actor,
            ActivityType type,
            String taskKey,
            String fieldName,
            String oldValue,
            String newValue
    ) {
        ActivityLog entry = new ActivityLog();
        entry.setProject(project);
        entry.setActor(actor);
        entry.setType(type);
        entry.setTaskKey(taskKey);
        entry.setFieldName(fieldName);
        entry.setOldValue(cut(oldValue));
        entry.setNewValue(cut(newValue));

        activityLogStore.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponse> getForProject(Long projectId) {
        return activityLogStore.findLatestByProjectId(projectId)
                .stream()
                .map(ActivityResponse::from)
                .toList();
    }

    private static String cut(String value) {
        if (value == null || value.length() <= MAX_VALUE_LENGTH) {
            return value;
        }

        return value.substring(0, MAX_VALUE_LENGTH);
    }
}