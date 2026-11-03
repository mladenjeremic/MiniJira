package minijira.example.minijiraaa.infrastructure.repository;

import minijira.example.minijiraaa.domain.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByProjectKeyIgnoreCase(String projectKey);

    // Svi projekti u kojima je korisnik vlasnik ili učesnik
    @Query("""
            select distinct p from Project p
            left join p.participants u
            where p.owner.id = :userId or u.id = :userId
            order by p.id
            """)
    List<Project> findAccessibleByUserId(@Param("userId") Long userId);

    // Broji koliko puta korisnik ima pristup projektu (0 = nema pristup)
    @Query("""
            select count(p) from Project p
            left join p.participants u
            where p.id = :projectId and (p.owner.id = :userId or u.id = :userId)
            """)
    long countAccess(@Param("projectId") Long projectId, @Param("userId") Long userId);

    default boolean hasAccess(Long projectId, Long userId) {
        return countAccess(projectId, userId) > 0;
    }
}