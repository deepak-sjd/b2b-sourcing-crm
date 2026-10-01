package com.doctutorials.crm.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.UUID;

/**
 * Real production storage: an S3-compatible bucket. Active when
 * app.storage.provider=s3 (set CRM_STORAGE_PROVIDER=s3 plus the
 * CRM_S3_* env vars below). Not exercised by default - LocalFileStorageService
 * runs instead until you deliberately switch this on.
 *
 * Downloads are proxied through our own /download endpoint (not a presigned
 * S3 URL) so access control is enforced in exactly one place - the lead
 * visibility check in DocumentService - rather than needing a second,
 * separately-expiring authorization path via presigned URLs. That's a
 * reasonable phase-1 tradeoff; if downloads become high-volume enough that
 * proxying through the app is a bottleneck, presigned URLs are the natural
 * next optimization.
 */
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;

    @Value("${app.storage.s3.bucket}")
    private String bucket;

    public S3FileStorageService(@Value("${app.storage.s3.region}") String region) {
        this.s3Client = S3Client.builder().region(Region.of(region)).build();
    }

    @Override
    public StoredFile store(UUID leadId, String originalFileName, String contentType, InputStream content,
            long sizeBytes) {
        String extension = extractSafeExtension(originalFileName);
        String key = "leads/" + leadId + "/" + UUID.randomUUID() + extension;

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .contentLength(sizeBytes)
                        .build(),
                RequestBody.fromInputStream(content, sizeBytes));

        return new StoredFile(key, sizeBytes);
    }

    @Override
    public Resource load(String storageKey) {
        var response = s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(storageKey).build());
        return new InputStreamResource(response);
    }

    @Override
    public void delete(String storageKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storageKey).build());
    }

    private String extractSafeExtension(String originalFileName) {
        if (originalFileName == null)
            return "";
        int dot = originalFileName.lastIndexOf('.');
        if (dot < 0 || dot == originalFileName.length() - 1)
            return "";
        String ext = originalFileName.substring(dot + 1).toLowerCase();
        return ext.matches("[a-z0-9]{1,10}") ? "." + ext : "";
    }
}
