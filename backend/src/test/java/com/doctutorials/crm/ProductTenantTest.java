package com.doctutorials.crm;

import com.doctutorials.crm.organization.Organization;
import com.doctutorials.crm.organization.OrganizationRepository;
import com.doctutorials.crm.product.Product;
import com.doctutorials.crm.product.ProductRepository;
import com.doctutorials.crm.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ProductTenantTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired OrganizationRepository organizations;
    @Autowired ProductRepository products;

    private Organization newOrg(String prefix) {
        Organization org = new Organization();
        org.setName(prefix);
        org.setSlug(prefix + "-" + UUID.randomUUID());
        return organizations.save(org);
    }

    private Product product(String name) {
        Product p = new Product();
        p.setName(name);
        return p;
    }

    @Test
    void productsAreTenantScopedAndNamesAreUniquePerTenant() {
        Organization a = newOrg("pa");
        Organization b = newOrg("pb");

        Product inA = TenantContext.supply(a.getId(), () -> products.save(product("Widget")));
        TenantContext.supply(b.getId(), () -> products.save(product("Widget"))); // same name, other tenant: fine

        assertThat(TenantContext.supply(b.getId(), () -> products.findById(inA.getId()))).isEmpty();
        assertThat(TenantContext.supply(a.getId(), () -> products.findAll())).hasSize(1);
        assertThatThrownBy(() -> TenantContext.supply(a.getId(), () -> products.saveAndFlush(product("WIDGET"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}