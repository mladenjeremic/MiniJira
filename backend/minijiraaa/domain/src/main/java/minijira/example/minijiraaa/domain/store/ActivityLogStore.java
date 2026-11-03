
package minijira.example.minijiraaa.domain.store;

import minijira.example.minijiraaa.domain.model.ActivityLog;

import java.util.List;

public interface ActivityLogStore {

    ActivityLog save(ActivityLog activityLog);

    List<ActivityLog> findLatestByProjectId(Long projectId);
}
