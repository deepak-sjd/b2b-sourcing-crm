package com.doctutorials.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the DocTutorials CRM backend.
 *
 * Modules wired in this codebase:
 *  - auth      : login / JWT issuing
 *  - user      : CRM users (admins, counselors, managers)
 *  - team      : grouping of users for reporting/ownership
 *  - lead      : the core pipeline object (student/customer lead)
 *  - task      : follow-ups tied to leads
 *  - call      : call logs tied to leads
 *  - note      : free-text notes tied to leads
 *  - audit     : who-did-what-when, written via AuditService
 *  - report    : aggregate/group-by analytics over leads/tasks
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class CrmApplication {
    public static void main(String[] args) {
        SpringApplication.run(CrmApplication.class, args);
    }
}
