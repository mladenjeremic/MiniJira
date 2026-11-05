package minijira.example.minijiraaa.application.service;

import lombok.RequiredArgsConstructor;
import minijira.example.minijiraaa.application.activity.LogActivity;
import minijira.example.minijiraaa.application.dto.ImageUpload;
import minijira.example.minijiraaa.domain.store.CommentStore;
import minijira.example.minijiraaa.domain.store.ImageStore;
import minijira.example.minijiraaa.application.dto.CommentRequest;
import minijira.example.minijiraaa.application.dto.CommentResponse;
import minijira.example.minijiraaa.application.mapper.CommentMapper;
import minijira.example.minijiraaa.domain.enums.ActivityType;
import minijira.example.minijiraaa.domain.model.Comment;
import minijira.example.minijiraaa.domain.model.Task;
import minijira.example.minijiraaa.domain.model.User;
import minijira.example.minijiraaa.application.access.TaskAccess;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CommentService {

    // DODATO
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_TEXT_LENGTH = 2000;
    private static final Map<String, String> IMAGE_TYPES = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/gif", "gif",
            "image/webp", "webp"
    );

    private final CommentStore commentStore;
    private final TaskAccess taskAccess;
    private final ImageStore imageStore; // DODATO
    private final CommentMapper commentMapper;

    @Transactional(readOnly = true)
    public List<CommentResponse> getForTask(Long taskId, User user) {
        taskAccess.getEntityForUser(taskId, user);

        return commentStore.findByTaskIdOrderByCreatedAtAsc(taskId)
                .stream()
                .map(commentMapper::toResponse)
                .toList();
    }

    // DODATO: vraća sliku komentara (uz istu proveru pristupa projektu)
    @Transactional(readOnly = true)
    public ImageUpload getImage(Long taskId, Long commentId, User user) {
        taskAccess.getEntityForUser(taskId, user);

        Comment comment = commentStore.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Comment not found"
                ));

        if (!Objects.equals(comment.getTask().getId(), taskId)
                || comment.getImageFileName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Image not found"
            );
        }

        return new ImageUpload(
                imageStore.load(comment.getImageFileName()),
                comment.getImageContentType()
        );
    }

    // IZMENJENO: novi potpis (text + opciona slika); taskId je i dalje prvi argument zbog ActivityLogAspect
    @LogActivity(ActivityType.COMMENT_ADDED)
    @Transactional
    public CommentResponse add(
            Long taskId,
            String text,
            ImageUpload image,
            User author
    ) {
        Task task = taskAccess.getEntityForUser(taskId, author);

        String cleanText = text == null ? "" : text.trim();
        boolean hasImage = image != null
                && image.data() != null
                && image.data().length > 0;

        if (cleanText.isEmpty() && !hasImage) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Comment must contain text or an image"
            );
        }

        if (cleanText.length() > MAX_TEXT_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Comment is too long"
            );
        }

        Comment comment = new Comment();
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setText(cleanText);

        if (hasImage) {
            String extension = validateImage(image);
            String fileName = imageStore.save(image.data(), extension);

            // ako transakcija ne prođe, ne ostavljamo "siroče" fajl na disku
            deleteImageIfNotCommitted(fileName);

            comment.setImageFileName(fileName);
            comment.setImageContentType(image.contentType().toLowerCase());
        }

        commentStore.save(comment);

        return commentMapper.toResponse(comment);
    }

    @LogActivity(ActivityType.COMMENT_UPDATED)
    @Transactional
    public CommentResponse update(
            Long taskId,
            Long commentId,
            CommentRequest request,
            User user
    ) {
        taskAccess.getEntityForUser(taskId, user);

        Comment comment = commentStore.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Comment not found"
                ));

        if (!Objects.equals(comment.getTask().getId(), taskId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Comment not found"
            );
        }

        if (user == null
                || comment.getAuthor() == null
                || !Objects.equals(
                comment.getAuthor().getId(),
                user.getId()
        )) {
            throw new AccessDeniedException(
                    "You are not authorized to update this comment"
            );
        }

        comment.setText(request.text().trim());
        commentStore.save(comment);

        return commentMapper.toResponse(comment);
    }

    @LogActivity(ActivityType.COMMENT_DELETED)
    @Transactional
    public void delete(Long taskId, Long commentId, User user) {
        taskAccess.getEntityForUser(taskId, user);

        Comment comment = commentStore.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Comment not found"
                ));

        if (!Objects.equals(comment.getTask().getId(), taskId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Comment not found"
            );
        }

        if (user == null
                || comment.getAuthor() == null
                || !Objects.equals(
                comment.getAuthor().getId(),
                user.getId()
        )) {
            throw new AccessDeniedException(
                    "You are not authorized to delete this comment"
            );
        }

        commentStore.delete(comment);

        // DODATO: briše i fajl slike, ali tek kad se transakcija uspešno commit-uje
        deleteImageAfterCommit(comment.getImageFileName());
    }

    // ---------- DODATO: pomoćne metode za slike ----------

    /** Proverava tip, veličinu i potpis fajla; vraća ekstenziju. */
    private String validateImage(ImageUpload image) {
        String type = image.contentType() == null
                ? ""
                : image.contentType().toLowerCase();

        String extension = IMAGE_TYPES.get(type);

        if (extension == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only PNG, JPEG, GIF and WEBP images are allowed"
            );
        }

        if (image.data().length > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Image is too large (max 5 MB)"
            );
        }

        if (!hasValidSignature(image.data(), extension)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File is not a valid image"
            );
        }

        return extension;
    }

    /** Proverava "magic bytes", jer content-type šalje klijent i može da laže. */
    private static boolean hasValidSignature(byte[] d, String extension) {
        return switch (extension) {
            case "png" -> d.length > 8
                    && (d[0] & 0xFF) == 0x89
                    && d[1] == 'P' && d[2] == 'N' && d[3] == 'G';
            case "jpg" -> d.length > 3
                    && (d[0] & 0xFF) == 0xFF
                    && (d[1] & 0xFF) == 0xD8
                    && (d[2] & 0xFF) == 0xFF;
            case "gif" -> d.length > 6
                    && d[0] == 'G' && d[1] == 'I' && d[2] == 'F' && d[3] == '8';
            case "webp" -> d.length > 12
                    && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                    && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P';
            default -> false;
        };
    }

    /** Ako se transakcija poništi (rollback), briše upravo snimljen fajl. */
    private void deleteImageIfNotCommitted(String fileName) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            imageStore.delete(fileName);
                        }
                    }
                });
    }

    /** Briše fajl tek posle uspešnog commit-a (da rollback ne ostavi komentar bez slike). */
    private void deleteImageAfterCommit(String fileName) {
        if (fileName == null) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            imageStore.delete(fileName);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        imageStore.delete(fileName);
                    }
                });
    }
}