package minijira.example.minijiraaa.infrastructure.store;

import lombok.extern.slf4j.Slf4j;
import minijira.example.minijiraaa.domain.exception.NotFoundException;
import minijira.example.minijiraaa.domain.store.ImageStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Component
public class LocalImageStore implements ImageStore {

    private final Path root;

    public LocalImageStore(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String save(byte[] data, String extension) {
        String fileName = UUID.randomUUID() + "." + extension;

        try {
            Files.createDirectories(root);
            Files.write(resolve(fileName), data);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store image", e);
        }

        return fileName;
    }

    @Override
    public byte[] load(String fileName) {
        try {
            return Files.readAllBytes(resolve(fileName));
        } catch (NoSuchFileException e) {
            throw new NotFoundException("Image not found.");
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read image", e);
        }
    }

    @Override
    public void delete(String fileName) {
        try {
            Files.deleteIfExists(resolve(fileName));
        } catch (IOException e) {
            log.warn("Could not delete image {}: {}", fileName, e.getMessage());
        }
    }

    /** Sprečava izlazak iz upload foldera (path traversal). */
    private Path resolve(String fileName) {
        Path path = root.resolve(fileName).normalize();

        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid file name");
        }

        return path;
    }
}