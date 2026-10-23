package com.doctutorials.crm.config;

import com.doctutorials.crm.tenant.TenantJpaRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.doctutorials.crm", repositoryBaseClass = TenantJpaRepository.class)
public class TenantRepositoryConfig {
}