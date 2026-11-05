package minijira.example.minijiraaa.application.mapper;

import minijira.example.minijiraaa.application.dto.ProjectResponse;
import minijira.example.minijiraaa.domain.model.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class, uses = UserMapper.class)
public interface ProjectMapper {

    @Mapping(target = "id", source = "project.id")
    @Mapping(target = "name", source = "project.name")
    @Mapping(target = "key", source = "project.projectKey")
    @Mapping(target = "description", source = "project.description")
    @Mapping(target = "taskCount", source = "taskCount")
    @Mapping(target = "owner", source = "project.owner")
    @Mapping(target = "participants", source = "project.participants")
    ProjectResponse toResponse(Project project, long taskCount);
}