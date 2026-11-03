package minijira.example.minijiraaa.application.service;

import lombok.RequiredArgsConstructor;
import minijira.example.minijiraaa.application.access.ProjectTaskAccess;
import minijira.example.minijiraaa.domain.store.TaskCountStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskCountService implements ProjectTaskAccess {

    private final TaskCountStore taskCountStore;

    @Override
    @Transactional(readOnly = true)
    public long countByProjectId(Long projectId) {
        return taskCountStore.countByProjectId(projectId);
    }
}