package minijira.example.minijiraaa.infrastructure.store;

import lombok.RequiredArgsConstructor;
import minijira.example.minijiraaa.domain.model.Task;
import minijira.example.minijiraaa.domain.store.TaskCountStore;
import minijira.example.minijiraaa.domain.store.TaskStore;
import minijira.example.minijiraaa.infrastructure.repository.TaskRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaTaskStore implements TaskStore, TaskCountStore {

    private final TaskRepository taskRepository;

    @Override
    public long countByProjectId(Long projectId) {
        return taskRepository.countByProjectId(projectId);
    }

    @Override
    public int findMaxTaskNumber(Long projectId) {
        return taskRepository.findMaxTaskNumber(projectId);
    }

    @Override
    public List<Task> findAll(
            Specification<Task> specification,
            Sort sort
    ) {
        return taskRepository.findAll(specification, sort);
    }

    @Override
    public Optional<Task> findById(Long taskId) {
        return taskRepository.findById(taskId);
    }

    @Override
    public Task save(Task task) {
        return taskRepository.save(task);
    }

    @Override
    public void delete(Task task) {
        taskRepository.delete(task);
    }
}