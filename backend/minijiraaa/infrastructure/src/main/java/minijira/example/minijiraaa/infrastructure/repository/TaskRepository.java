package minijira.example.minijiraaa.infrastructure.repository;

import minijira.example.minijiraaa.domain.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    long countByProjectId(Long projectId);

    @Query("select coalesce(max(t.taskNumber), 0) from Task t where t.project.id = :projectId")
    int findMaxTaskNumber(@Param("projectId") Long projectId);
}