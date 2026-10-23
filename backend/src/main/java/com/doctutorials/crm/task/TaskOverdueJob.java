package com.doctutorials.crm.task;

import com.doctutorials.crm.tenant.TenantRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskOverdueJob {

    private final TaskService taskService;
    private final TenantRunner tenantRunner;

    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void run() {
        tenantRunner.forEachActiveTenant(id -> taskService.markOverdueTasks());
    }
}