
package minijira.example.minijiraaa.domain.store;

import minijira.example.minijiraaa.domain.model.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentStore {

    List<Comment> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    Optional<Comment> findById(Long commentId);

    Comment save(Comment comment);

    void delete(Comment comment);
}
