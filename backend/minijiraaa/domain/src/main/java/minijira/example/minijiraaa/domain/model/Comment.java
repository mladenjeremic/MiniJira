package minijira.example.minijiraaa.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content", nullable = false, length = 2000)
    private String text;

    // DODATO: ime fajla na disku (UUID.ext); null ako komentar nema sliku
    @Column(name = "image_file_name", length = 100)
    private String imageFileName;

    // DODATO: image/png, image/jpeg, image/gif ili image/webp
    @Column(name = "image_content_type", length = 50)
    private String imageContentType;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}