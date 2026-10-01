package com.doctutorials.crm.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.UUID;

/**
 * Abstraction over "where do uploaded files actually live" so DocumentService
 * doesn't care whether it's local disk or S3. Swap implementations via
 * app.storage.provider without touching any calling code - same pattern as
 * MailService (ConsoleMailService/SmtpMailService).
 */
public interface FileStorageService {

    /**
     * Stores the file content and returns the server-generated key used to
     * retrieve/delete it later. leadId is used only to namespace/organize
     * storage (e.g. an S3 key prefix) - callers must never derive access
     * control from this key, only DocumentService's lead-visibility check does
     * that.
     */
    StoredFile store(UUID leadId, String originalFileName, String contentType, InputStream content, long sizeBytes);

    /**
     * Loads the file content by storage key for streaming back in a download
     * response.
     */
    Resource load(String storageKey);

    void delete(String storageKey);

    record StoredFile(String storageKey, long sizeBytes) {
    }
}
