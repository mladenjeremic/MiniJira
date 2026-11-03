
package minijira.example.minijiraaa.domain.store;

import minijira.example.minijiraaa.domain.model.Project;

import java.util.List;
import java.util.Optional;

public interface ProjectStore {

    List<Project> findAccessibleByUserId(Long userId);

    Optional<Project> findById(Long projectId);

    Project save(Project project);

    boolean existsByProjectKeyIgnoreCase(String projectKey);

    boolean hasAccess(Long projectId, Long userId);
}
