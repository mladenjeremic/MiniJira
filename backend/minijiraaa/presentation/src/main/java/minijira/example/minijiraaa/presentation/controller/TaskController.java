package minijira.example.minijiraaa.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import minijira.example.minijiraaa.application.dto.StatusRequest;
import minijira.example.minijiraaa.application.dto.TaskRequest;
import minijira.example.minijiraaa.application.dto.TaskResponse;
import minijira.example.minijiraaa.domain.enums.Priority;
import minijira.example.minijiraaa.domain.enums.Status;
import minijira.example.minijiraaa.domain.enums.Type;
import minijira.example.minijiraaa.domain.model.User;
import minijira.example.minijiraaa.application.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/api/projects/{projectId}/tasks")
    public List<TaskResponse> getForProject(
            @PathVariable("projectId") Long projectId,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "assigneeId", required = false) Long assigneeId,
            @RequestParam(name = "priority", required = false) Priority priority,
            @RequestParam(name = "type", required = false) Type type,
            @RequestParam(name = "status", required = false) Status status,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "dir", required = false) String dir,
            @AuthenticationPrincipal User user) {
        return taskService.getForProject(projectId, q, assigneeId, priority, type, status, sortBy, dir, user);
    }

    @PostMapping("/api/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@PathVariable("projectId") Long projectId,
                               @Valid @RequestBody TaskRequest request,
                               @AuthenticationPrincipal User user) {
        return taskService.create(projectId, request, user);
    }

    @GetMapping("/api/tasks/{id}")
    public TaskResponse getById(@PathVariable("id") Long id,
                                @AuthenticationPrincipal User user) {
        return taskService.getById(id, user);
    }

    @PutMapping("/api/tasks/{id}")
    public TaskResponse update(@PathVariable("id") Long id,
                               @Valid @RequestBody TaskRequest request,
                               @AuthenticationPrincipal User user) {
        return taskService.update(id, request, user);
    }

    /** Koristi se za drag & drop izmedju kolona. */
    @PatchMapping("/api/tasks/{id}/status")
    public TaskResponse updateStatus(@PathVariable("id") Long id,
                                     @Valid @RequestBody StatusRequest request,
                                     @AuthenticationPrincipal User user) {
        return taskService.updateStatus(id, request.status(), user);
    }

    @DeleteMapping("/api/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id,
                       @AuthenticationPrincipal User user) {
        taskService.delete(id, user);
    }
}