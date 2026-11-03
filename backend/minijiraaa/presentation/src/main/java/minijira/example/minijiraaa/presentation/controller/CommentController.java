package minijira.example.minijiraaa.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import minijira.example.minijiraaa.application.dto.ImageUpload;
import minijira.example.minijiraaa.application.service.CommentService;
import minijira.example.minijiraaa.application.dto.CommentRequest;
import minijira.example.minijiraaa.application.dto.CommentResponse;
import minijira.example.minijiraaa.domain.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public List<CommentResponse> getForTask(@PathVariable("taskId") Long taskId,
                                            @AuthenticationPrincipal User user) {
        return commentService.getForTask(taskId, user);
    }

    // DODATO: tekst + opciona slika (multipart/form-data)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addWithImage(
            @PathVariable("taskId") Long taskId,
            @RequestParam("text") String text,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal User user) throws IOException {

        ImageUpload upload = (image == null || image.isEmpty())
                ? null
                : new ImageUpload(image.getBytes(), image.getContentType());

        return commentService.add(taskId, text, upload, user);
    }

    // IZMENJENO: ostaje JSON verzija (samo tekst), da stari frontend ne pukne
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse add(@PathVariable("taskId") Long taskId,
                               @Valid @RequestBody CommentRequest request,
                               @AuthenticationPrincipal User user) {
        return commentService.add(taskId, request.text(), null, user);
    }

    // DODATO: vraća sliku komentara (traži Bearer token, zato je frontend učitava kao blob)
    @GetMapping("/{commentId}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable("taskId") Long taskId,
                                           @PathVariable("commentId") Long commentId,
                                           @AuthenticationPrincipal User user) {
        ImageUpload image = commentService.getImage(taskId, commentId, user);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.data());
    }

    @PatchMapping("/{commentId}")
    public CommentResponse update(@PathVariable("taskId") Long taskId,
                                  @PathVariable("commentId") Long commentId,
                                  @Valid @RequestBody CommentRequest request,
                                  @AuthenticationPrincipal User user) {
        return commentService.update(taskId, commentId, request, user);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("taskId") Long taskId,
                       @PathVariable("commentId") Long commentId,
                       @AuthenticationPrincipal User user) {
        commentService.delete(taskId, commentId, user);
    }
}