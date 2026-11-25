package com.doctutorials.crm.product;

import com.doctutorials.crm.audit.AuditAction;
import com.doctutorials.crm.audit.AuditService;
import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.product.dto.ProductRequest;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final AuditService auditService;

    @Transactional
    public Product create(ProductRequest req) {
        String name = req.name().trim();
        if (productRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("A product or service named '" + name + "' already exists");
        }
        Product product = new Product();
        apply(product, req, name);
        Product saved = productRepository.save(product);
        auditService.log("Product", saved.getId(), AuditAction.CREATE, "Product created: " + saved.getName());
        return saved;
    }

    @Transactional
    public Product update(UUID id, ProductRequest req) {
        Product product = getById(id);
        String name = req.name().trim();
        if (productRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BadRequestException("A product or service named '" + name + "' already exists");
        }
        apply(product, req, name);
        Product saved = productRepository.save(product);
        auditService.log("Product", id, AuditAction.UPDATE, "Product updated: " + saved.getName());
        return saved;
    }

    public Product getById(UUID id) {
        return productRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Product", id));
    }

    public Page<Product> list(String q, Boolean active, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("category"), "")), like)));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return productRepository.findAll(spec, pageable);
    }

    private static void apply(Product product, ProductRequest req, String name) {
        product.setName(name);
        product.setCategory(blankToNull(req.category()));
        product.setDescription(blankToNull(req.description()));
        product.setUnit(req.unit() == null || req.unit().isBlank() ? "Nos" : req.unit().trim());
        product.setDefaultPrice(req.defaultPrice());
        product.setActive(req.active() == null || req.active());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}