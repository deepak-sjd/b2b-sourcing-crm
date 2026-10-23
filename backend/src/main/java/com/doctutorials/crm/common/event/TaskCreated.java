package com.doctutorials.crm.common.event;

import com.doctutorials.crm.task.TaskType;

import java.time.Instant;
import java.util.UUID;

/** Placeholder until D1's event bus lands - move/rename to match DomainEvent when it does. */
public record TaskCreated(UUID taskId, UUID leadId, UUID assigneeId, TaskType type, Instant dueDate) {
}
