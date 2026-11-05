package minijira.example.minijiraaa.application.mapper;

import minijira.example.minijiraaa.application.dto.TaskRequest;
import minijira.example.minijiraaa.application.dto.TaskResponse;
import minijira.example.minijiraaa.domain.model.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = MapStructConfig.class, uses = UserMapper.class)
public interface TaskMapper {

    @Mapping(target = "key",
            expression = "java(task.getProject().getProjectKey() + \"-\" + task.getTaskNumber())")
    @Mapping(target = "number", source = "taskNumber")
    @Mapping(target = "projectId", source = "project.id")
    TaskResponse toResponse(Task task);

    List<TaskResponse> toResponseList(List<Task> tasks);

    /** Prepisuje polja iz zahteva na postojeći task (zamena za prvi deo apply metode). */
    @Mapping(target = "title", expression = "java(request.title().trim())")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "taskNumber", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "comments", ignore = true)
    void updateEntity(TaskRequest request, @MappingTarget Task task);
}