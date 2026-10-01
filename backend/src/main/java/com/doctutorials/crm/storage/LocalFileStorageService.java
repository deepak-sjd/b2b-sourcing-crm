package com.doctutorials.crm.storage;

import com.doctutorials.crm.common.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Stores files on local disk under app.storage.local.base-dir. This is the
 * default (app.storage.provider=local) so the app runs and is fully
 * testable with zero cloud setup. Switch to S3FileStorageService for a real
 * deployment by setting app.storage.provider=s3.
 */
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    @Value("${app.storage.local.base-dir}")
    private String baseDirPath;

    private Path baseDir;

    @PostConstruct
    void init() {
        baseDir = Path.of(baseDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory: " + baseDir, e);
        }
    }

    @Override
    public StoredFile store(UUID leadId, String originalFileName, String contentType, InputStream content,
            long sizeBytes) {
        // Storage key is server-generated (UUID), never the client's filename - this is
        // what rules out path traversal ("../../etc/passwd") and filename collisions.
        // The original filename is kept only as display metadata on the Document
        // entity.
        String extension = extractSafeExtension(originalFileName);
        String key = leadId + "/" + UUID.randomUUID() + extension;

        Path target = resolveWithinBaseDir(key);
        try {
            Files.createDirectories(target.getParent());
            try (content) {
                Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store uploaded file", e);
        }
        return new StoredFile(key, sizeBytes);
    }

    @Override
    public Resource load(String storageKey) {
        Path file = resolveWithinBaseDir(storageKey);
        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BadRequestException("File is missing from storage");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new BadRequestException("Invalid storage key");
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolveWithinBaseDir(storageKey));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete stored file", e);
        }
    }

    /**
     * Resolves a storage key to an on-disk path and verifies the result is still
     * inside baseDir. Keys are always server-generated UUIDs today so this can't
     * actually be tricked - this check is defense in depth for the day someone
     * changes how keys are built and forgets this constraint exists.
     */
    private Path resolveWithinBaseDir(String storageKey) {
        Path resolved = baseDir.resolve(storageKey).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new BadRequestException("Invalid storage key");
        }
        return resolved;
    }

    private String extractSafeExtension(String originalFileName) {
        if (originalFileName == null)
            return "";
        int dot = originalFileName.lastIndexOf('.');
        if (dot < 0 || dot == originalFileName.length() - 1)
            return "";
        String ext = originalFileName.substring(dot + 1).toLowerCase();
        // Only keep it if it's plain alphanumerics - anything else (path separators,
        // null bytes, etc. smuggled into a filename) is dropped rather than trusted.
        return ext.matches("[a-z0-9]{1,10}") ? "." + ext : "";
    }
}
