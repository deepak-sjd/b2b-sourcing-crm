package com.doctutorials.crm.common.event;

import java.time.Instant;
import java.util.UUID;

/** Published once per task when the sweep flips it to OVERDUE. Consumers dedupe on taskId. */
public record TaskOverdue(UUID taskId, UUID leadId, UUID assigneeId, Instant dueDate) {
}
