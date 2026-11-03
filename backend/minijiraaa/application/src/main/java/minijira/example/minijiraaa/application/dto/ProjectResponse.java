package minijira.example.minijiraaa.application.dto;

import minijira.example.minijiraaa.domain.model.Project;

import java.util.List;

public record ProjectResponse(
        Long id,
        String name,
        String key,
        String description,
        long taskCount,
        ProjectMemberResponse owner,
        List<ProjectMemberResponse> participants
) {
    public static ProjectResponse from(
            Project project,
            long taskCount
    ) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getProjectKey(),
                project.getDescription(),
                taskCount,
                ProjectMemberResponse.from(project.getOwner()),
                project.getParticipants()
                        .stream()
                        .map(ProjectMemberResponse::from)
                        .toList()
        );
    }
}