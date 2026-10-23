package com.doctutorials.crm.common.event;

import java.util.UUID;

public record TaskCompleted(UUID taskId, UUID leadId, UUID completedById) {
}
