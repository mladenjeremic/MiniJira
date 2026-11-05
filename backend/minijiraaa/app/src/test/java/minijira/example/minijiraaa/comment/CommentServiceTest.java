package minijira.example.minijiraaa.comment;

import minijira.example.minijiraaa.application.access.TaskAccess;
import minijira.example.minijiraaa.application.dto.CommentRequest;
import minijira.example.minijiraaa.application.dto.CommentResponse;
import minijira.example.minijiraaa.application.dto.ImageUpload;
import minijira.example.minijiraaa.application.service.CommentService;
import minijira.example.minijiraaa.domain.model.Comment;
import minijira.example.minijiraaa.domain.model.Project;
import minijira.example.minijiraaa.domain.model.Task;
import minijira.example.minijiraaa.domain.model.User;
import minijira.example.minijiraaa.domain.store.CommentStore;
import minijira.example.minijiraaa.domain.store.ImageStore;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import minijira.example.minijiraaa.application.mapper.UserMapper;
import minijira.example.minijiraaa.application.mapper.UserMapperImpl;
import minijira.example.minijiraaa.application.mapper.CommentMapper;
import minijira.example.minijiraaa.application.mapper.CommentMapperImpl;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock CommentStore commentStore;
    @Mock TaskAccess taskAccess;
    @Mock ImageStore imageStore;

    @Spy UserMapper userMapper = new UserMapperImpl();
    @Spy CommentMapper commentMapper = new CommentMapperImpl(userMapper);

    @InjectMocks CommentService commentService;

    private User author;
    private User other;
    private Task task;
    private Comment comment;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setId(1L);
        author.setName("Ana");

        other = new User();
        other.setId(2L);

        Project project = new Project();
        project.setId(1L);
        project.setProjectKey("MJ");

        task = new Task();
        task.setId(5L);
        task.setTaskNumber(5);
        task.setProject(project);

        comment = new Comment();
        comment.setId(9L);
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setText("hello");
    }

    /** Minimalan "pravi" PNG potpis (magic bytes) da prođe provera slike. */
    private static byte[] png() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
    }

    private static void assertStatus(ThrowingCallable call, int status) {
        assertThatThrownBy(call).isInstanceOfSatisfying(
                ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode().value()).isEqualTo(status));
    }

    // ---------- getForTask ----------

    @Test
    void getForTask_returnsCommentsOfTask() {
        when(taskAccess.getEntityForUser(5L, author)).thenReturn(task);
        when(commentStore.findByTaskIdOrderByCreatedAtAsc(5L)).thenReturn(List.of(comment));

        List<CommentResponse> result = commentService.getForTask(5L, author);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).text()).isEqualTo("hello");
        assertThat(result.get(0).hasImage()).isFalse();
    }

    // ---------- add ----------

    @Test
    void add_textOnly_savesTrimmedCommentWithoutTouchingImageStore() {
        when(taskAccess.getEntityForUser(5L, author)).thenReturn(task);

        CommentResponse response = commentService.add(5L, "  hi  ", null, author);

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentStore).save(captor.capture());
        Comment saved = captor.getValue();

        assertThat(saved.getText()).isEqualTo("hi");
        assertThat(saved.getAuthor()).isSameAs(author);
        assertThat(saved.getTask()).isSameAs(task);
        assertThat(saved.getImageFileName()).isNull();
        assertThat(response.hasImage()).isFalse();
        verifyNoInteractions(imageStore);
    }

    @Test
    void add_rejectsEmptyTextWithoutImage() {
        when(taskAccess.getEntityForUser(5L, author)).thenReturn(task);

        assertStatus(() -> commentService.add(5L, "   ", null, author), 400);

        verify(commentStore, never()).save(any());
    }

    @Test
    void add_rejectsTooLongText() {
        when(taskAccess.getEntityForUser(5L, author)).thenReturn(task);

        assertStatus(() -> commentService.add(5L, "a".repeat(2001), null, author), 400);

        verify(commentStore, never()).save(any());
    }

    @Test
    void add_withValidPng_savesImageEvenWithoutText() {
        byte[] bytes = png();
        when(taskAccess.getEntityForUser(5L, author)).thenReturn(task);
        when(imageStore.save(bytes, "png")).thenReturn("abc.png");

        CommentResponse response = commentService.add(
                5L, null, new ImageUpload(bytes, "image/png"), author);

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentStore).save(captor.capture());

        assertThat(captor.getValue().getText()).isEmpty();
        assertThat(captor.getValue().getImageFileName()).isEqualTo("abc.png");
        assertThat(captor.getValue().getImageContentType()).isEqualTo("image/png");
        assertThat(response.hasImage()).isTrue();
    }

    @Test
    void add_rejectsUnsupportedImageType() {
        when(taskAccess.getEntityForUser(5L, author)).thenReturn(task);

        assertStatus(() -> commentService.add(
                5L, "x", new ImageUpload(png(), "application/pdf"), author), 400);

        verifyNoInteractions(imageStore);
        verify(commentStore, never()).save(any());
    }

    @Test
    void add_rejectsTooLargeImage() {
        when(taskAccess.getEntityForUser(5L, author)).thenReturn(task);
        byte[] huge = new byte[5 * 1024 * 1024 + 1];

        assertStatus(() -> commentService.add(
                5L, "x", new ImageUpload(huge, "image/png"), author), 400);

        verifyNoInteractions(imageStore);
    }

    @Test
    void add_rejectsFileWhoseContentIsNotRealImage() {
        when(taskAccess.getEntityForUser(5L, author)).thenReturn(task);
        byte[] fake = "ovo nije slika".getBytes();

        assertStatus(() -> commentService.add(
                5L, "x", new ImageUpload(fake, "image/png"), author), 400);

        verifyNoInteractions(imageStore);
    }

    // ---------- update ----------

    @Test
    void update_byAuthor_savesTrimmedText() {
        when(taskAccess.getEntityForUser(5L, author)).thenReturn(task);
        when(commentStore.findById(9L)).thenReturn(Optional.of(comment));

        CommentResponse response = commentService.update(5L, 9L, new CommentRequest("  new  "), author);

        assertThat(comment.getText()).isEqualTo("new");
        assertThat(response.text()).isEqualTo("new");
        verify(commentStore).save(comment);
    }

    @Test
    void update_isForbiddenForSomeoneElsesComment() {
        when(taskAccess.getEntityForUser(5L, other)).thenReturn(task);
        when(commentStore.findById(9L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() ->
                commentService.update(5L, 9L, new CommentRequest("hacked"), other))
                .isInstanceOf(AccessDeniedException.class);

        verify(commentStore, never()).save(any());
    }

    @Test
    void update_returns404WhenCommentBelongsToAnotherTask() {
        Task otherTask = new Task();
        otherTask.setId(6L);
        comment.setTask(otherTask);

        when(taskAccess.getEntityForUser(5L, author)).thenReturn(task);
        when(commentStore.findById(9L)).thenReturn(Optional.of(comment));

        assertStatus(() -> commentService.update(5L, 9L, new CommentRequest("x"), author), 404);
    }

    @Test
    void update_returns404WhenCommentDoesNotExist() {
        when(taskAccess.getEntityForUser(5L, author)).thenReturn(task);
        when(commentStore.findById(9L)).thenReturn(Optional.empty());

        assertStatus(() -> commentService.update(5L, 9L, new CommentRequest("x"), author), 404);
    }

    // ---------- delete ----------

    @Test
    void delete_isForbiddenForNonAuthor() {
        when(taskAccess.getEntityForUser(5L, other)).thenReturn(task);
        when(commentStore.findById(9L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(5L, 9L, other))
                .isInstanceOf(AccessDeniedException.class);

        verify(commentStore, never()).delete(any());
    }

    @Test
    void delete_byAuthor_removesComment() {
        when(taskAccess.getEntityForUser(5L, author)).thenReturn(task);
        when(commentStore.findById(9L)).thenReturn(Optional.of(comment));

        commentService.delete(5L, 9L, author);

        verify(commentStore).delete(comment);
        verifyNoInteractions(imageStore);   // komentar nema sliku
    }

    @Test
    void delete_byAuthor_alsoDeletesImageFile() {
        comment.setImageFileName("abc.png");

        when(taskAccess.getEntityForUser(5L, author)).thenReturn(task);
        when(commentStore.findById(9L)).thenReturn(Optional.of(comment));

        commentService.delete(5L, 9L, author);

        // u unit testu nema transakcije, pa se fajl briše odmah
        verify(imageStore).delete("abc.png");
    }

    // ---------- getImage ----------

    @Test
    void getImage_returnsBytesAndContentType() {
        byte[] bytes = png();
        comment.setImageFileName("abc.png");
        comment.setImageContentType("image/png");

        when(taskAccess.getEntityForUser(5L, author)).thenReturn(task);
        when(commentStore.findById(9L)).thenReturn(Optional.of(comment));
        when(imageStore.load("abc.png")).thenReturn(bytes);

        ImageUpload result = commentService.getImage(5L, 9L, author);

        assertThat(result.data()).isSameAs(bytes);
        assertThat(result.contentType()).isEqualTo("image/png");
    }

    @Test
    void getImage_returns404WhenCommentHasNoImage() {
        when(taskAccess.getEntityForUser(5L, author)).thenReturn(task);
        when(commentStore.findById(9L)).thenReturn(Optional.of(comment));

        assertStatus(() -> commentService.getImage(5L, 9L, author), 404);

        verifyNoInteractions(imageStore);
    }
}