package minijira.example.minijiraaa.application.service;

import lombok.RequiredArgsConstructor;
import minijira.example.minijiraaa.application.access.ActivityAccess;
import minijira.example.minijiraaa.application.access.ProjectAccess;
import minijira.example.minijiraaa.application.access.ProjectTaskAccess;
import minijira.example.minijiraaa.application.access.UserAccess;
import minijira.example.minijiraaa.application.activity.LogActivity;
import minijira.example.minijiraaa.application.dto.ActivityResponse;
import minijira.example.minijiraaa.application.dto.ProjectRequest;
import minijira.example.minijiraaa.application.dto.ProjectResponse;
import minijira.example.minijiraaa.domain.enums.ActivityType;
import minijira.example.minijiraaa.domain.exception.ConflictException;
import minijira.example.minijiraaa.domain.exception.NotFoundException;
import minijira.example.minijiraaa.domain.model.Project;
import minijira.example.minijiraaa.domain.model.User;
import minijira.example.minijiraaa.domain.store.ProjectStore;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService implements ProjectAccess {

    private final ProjectStore projectStore;

    // Broji taskove preko posebnog interfejsa (a ne preko TaskService), da ne
    // nastane kružna zavisnost: TaskService -> ProjectAccess -> ProjectService.
    private final ProjectTaskAccess projectTaskAccess;

    private final UserAccess userAccess;

    // Ostaje samo za čitanje history-ja (getHistory). Upis radi ActivityLogAspect.
    private final ActivityAccess activityAccess;

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAll(User currentUser) {
        return projectStore.findAccessibleByUserId(currentUser.getId())
                .stream()
                .map(project -> ProjectResponse.from(
                        project,
                        projectTaskAccess.countByProjectId(project.getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(Long id, User currentUser) {
        assertAccess(id, currentUser);

        Project project = getEntityById(id);

        return ProjectResponse.from(
                project,
                projectTaskAccess.countByProjectId(id)
        );
    }

    @LogActivity(ActivityType.PROJECT_CREATED)
    @Transactional
    public ProjectResponse create(ProjectRequest request, User owner) {
        String key = request.key().trim().toUpperCase();

        if (projectStore.existsByProjectKeyIgnoreCase(key)) {
            throw new ConflictException(
                    "Project with key " + key + " already exists."
            );
        }

        Project project = new Project();
        project.setName(request.name().trim());
        project.setProjectKey(key);
        project.setDescription(
                request.description() == null
                        ? null
                        : request.description().trim()
        );
        project.setOwner(owner);
        project.getParticipants().add(owner);

        projectStore.save(project);

        return ProjectResponse.from(project, 0);
    }

    @Override
    public Project getEntityById(Long id) {
        return projectStore.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Project with id " + id + " not found."
                        )
                );
    }

    @Override
    public void assertAccess(Long projectId, User user) {
        getEntityById(projectId);

        if (user == null
                || !projectStore.hasAccess(projectId, user.getId())) {
            throw new AccessDeniedException(
                    "You do not have access to this project."
            );
        }
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> getHistory(
            Long projectId,
            User currentUser
    ) {
        assertAccess(projectId, currentUser);
        return activityAccess.getForProject(projectId);
    }

    @LogActivity(ActivityType.PARTICIPANT_ADDED)
    @Transactional
    public ProjectResponse addParticipant(
            Long projectId,
            Long userId,
            User currentUser
    ) {
        Project project = getEntityById(projectId);

        // Samo vlasnik projekta može dodavati učesnike.
        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException(
                    "Only the project owner can add participants."
            );
        }

        User participant = userAccess.findById(userId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "User with id " + userId + " not found."
                        )
                );

        // Sprečavamo dodavanje istog učesnika više puta.
        boolean alreadyParticipant = project.getParticipants()
                .stream()
                .anyMatch(user -> user.getId().equals(userId));

        if (!alreadyParticipant) {
            project.getParticipants().add(participant);
        }

        Project savedProject = projectStore.save(project);

        long taskCount = projectTaskAccess.countByProjectId(projectId);

        return ProjectResponse.from(savedProject, taskCount);
    }
}
