package minijira.example.minijiraaa.infrastructure.store;

import lombok.RequiredArgsConstructor;
import minijira.example.minijiraaa.domain.model.Project;
import minijira.example.minijiraaa.domain.store.ProjectStore;
import minijira.example.minijiraaa.infrastructure.repository.ProjectRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaProjectStore implements ProjectStore {

    private final ProjectRepository projectRepository;

    @Override
    public List<Project> findAccessibleByUserId(Long userId) {
        return projectRepository.findAccessibleByUserId(userId);
    }

    @Override
    public Optional<Project> findById(Long projectId) {
        return projectRepository.findById(projectId);
    }

    @Override
    public Project save(Project project) {
        return projectRepository.save(project);
    }

    @Override
    public boolean existsByProjectKeyIgnoreCase(String projectKey) {
        return projectRepository.existsByProjectKeyIgnoreCase(projectKey);
    }

    @Override
    public boolean hasAccess(Long projectId, Long userId) {
        return projectRepository.hasAccess(projectId, userId);
    }
}