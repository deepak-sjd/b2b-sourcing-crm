package com.doctutorials.crm.task.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TaskActionRequests {

    private TaskActionRequests() {
    }

    public record Snooze(@NotNull Instant until) {
    }

    public record Cancel(@Size(max = 500) String reason) {
    }

    public record Reassign(@NotNull UUID assigneeId) {
    }

    public record BulkComplete(@NotEmpty @Size(max = 100) List<UUID> ids) {
    }
}
