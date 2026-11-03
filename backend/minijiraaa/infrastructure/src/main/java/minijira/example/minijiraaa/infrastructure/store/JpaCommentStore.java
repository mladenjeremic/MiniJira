package minijira.example.minijiraaa.infrastructure.store;

import lombok.RequiredArgsConstructor;
import minijira.example.minijiraaa.domain.store.CommentStore;
import minijira.example.minijiraaa.domain.model.Comment;
import minijira.example.minijiraaa.infrastructure.repository.CommentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaCommentStore implements CommentStore {

    private final CommentRepository commentRepository;

    @Override
    public List<Comment> findByTaskIdOrderByCreatedAtAsc(Long taskId) {
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    @Override
    public Optional<Comment> findById(Long commentId) {
        return commentRepository.findById(commentId);
    }

    @Override
    public Comment save(Comment comment) {
        return commentRepository.save(comment);
    }

    @Override
    public void delete(Comment comment) {
        commentRepository.delete(comment);
    }
}