package minijira.example.minijiraaa.application.mapper;

import minijira.example.minijiraaa.application.dto.ProjectMemberResponse;
import minijira.example.minijiraaa.application.dto.UserResponse;
import minijira.example.minijiraaa.domain.model.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = MapStructConfig.class)
public interface UserMapper {

    UserResponse toResponse(User user);

    ProjectMemberResponse toMemberResponse(User user);

    List<UserResponse> toResponseList(List<User> users);
}