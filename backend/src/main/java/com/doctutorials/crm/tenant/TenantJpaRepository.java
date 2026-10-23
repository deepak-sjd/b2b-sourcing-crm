package com.doctutorials.crm.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Base class for every repository. Hibernate's tenant filter does not apply to loads by
 * primary key (em.find), so for tenant-owned entities findById goes through a query,
 * which IS filtered. deleteById and existsById go through findById / queries, so they
 * are covered too. Non-tenant entities (User, Organization, tokens) behave as before.
 */
public class TenantJpaRepository<T, ID> extends SimpleJpaRepository<T, ID> {

    private final EntityManager entityManager;
    private final JpaEntityInformation<T, ?> entityInformation;

    public TenantJpaRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.entityInformation = entityInformation;
    }

    private boolean tenantOwned() {
        return TenantEntity.class.isAssignableFrom(entityInformation.getJavaType());
    }

    @Override
    public Optional<T> findById(ID id) {
        if (!tenantOwned()) {
            return super.findById(id);
        }
        String jpql = "select e from " + entityInformation.getEntityName() + " e where e."
                + entityInformation.getRequiredIdAttribute().getName() + " = :id";
        List<T> found = entityManager.createQuery(jpql, entityInformation.getJavaType())
                .setParameter("id", id)
                .getResultList();
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    @Override
    public T getReferenceById(ID id) {
        if (!tenantOwned()) {
            return super.getReferenceById(id);
        }
        return findById(id).orElseThrow(() -> new EntityNotFoundException(
                entityInformation.getEntityName() + " not found: " + id));
    }
}