package minijira.example.minijiraaa.infrastructure.store;

import lombok.RequiredArgsConstructor;
import minijira.example.minijiraaa.domain.store.ActivityLogStore;
import minijira.example.minijiraaa.domain.model.ActivityLog;
import minijira.example.minijiraaa.infrastructure.repository.ActivityLogRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JpaActivityLogStore implements ActivityLogStore {

    private final ActivityLogRepository activityLogRepository;

    @Override
    public ActivityLog save(ActivityLog activityLog) {
        return activityLogRepository.save(activityLog);
    }

    @Override
    public List<ActivityLog> findLatestByProjectId(Long projectId) {
        return activityLogRepository
                .findTop200ByProjectIdOrderByCreatedAtDescIdDesc(projectId);
    }
}