
package minijira.example.minijiraaa.domain.store;

import minijira.example.minijiraaa.domain.model.Task;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface TaskStore {

    long countByProjectId(Long projectId);

    int findMaxTaskNumber(Long projectId);

    List<Task> findAll(
            Specification<Task> specification,
            Sort sort
    );

    Optional<Task> findById(Long taskId);

    Task save(Task task);

    void delete(Task task);
}
