package minijira.example.minijiraaa.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import minijira.example.minijiraaa.application.dto.ActivityResponse;
import minijira.example.minijiraaa.application.dto.ProjectRequest;
import minijira.example.minijiraaa.application.dto.ProjectResponse;
import minijira.example.minijiraaa.domain.model.User;
import minijira.example.minijiraaa.application.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public List<ProjectResponse> getAll(@AuthenticationPrincipal User user) {
        return projectService.getAll(user);
    }

    @GetMapping("/{id}")
    public ProjectResponse getById(@PathVariable("id") Long id,
                                   @AuthenticationPrincipal User user) {
        return projectService.getById(id, user);
    }

    // NOVO: istorija izmena projekta
    @GetMapping("/{id}/history")
    public List<ActivityResponse> getHistory(@PathVariable("id") Long id,
                                             @AuthenticationPrincipal User user) {
        return projectService.getHistory(id, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody ProjectRequest request,
                                  @AuthenticationPrincipal User user) {
        return projectService.create(request, user);
    }

    @PostMapping("/{id}/participants/{userId}")
    public ProjectResponse addParticipant(
            @PathVariable("id") Long projectId,
            @PathVariable("userId") Long userId,
            @AuthenticationPrincipal User user
    ) {
        return projectService.addParticipant(
                projectId,
                userId,
                user
        );
    }
}