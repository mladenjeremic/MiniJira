package minijira.example.minijiraaa.infrastructure.repository;

import minijira.example.minijiraaa.domain.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    // Poslednjih 200 događaja projekta, najnoviji prvi
    List<ActivityLog> findTop200ByProjectIdOrderByCreatedAtDescIdDesc(Long projectId);
}