package minijira.example.minijiraaa.application.mapper;

import minijira.example.minijiraaa.application.dto.CommentResponse;
import minijira.example.minijiraaa.domain.model.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapStructConfig.class, uses = UserMapper.class)
public interface CommentMapper {

    @Mapping(target = "hasImage", expression = "java(comment.getImageFileName() != null)")
    CommentResponse toResponse(Comment comment);

    List<CommentResponse> toResponseList(List<Comment> comments);
}