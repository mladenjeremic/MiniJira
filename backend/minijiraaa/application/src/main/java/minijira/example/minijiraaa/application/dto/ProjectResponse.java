package minijira.example.minijiraaa.application.dto;

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
}