package com.doctutorials.crm.product.dto;

import com.doctutorials.crm.product.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(UUID id, String name, String category, String description, String unit,
                              BigDecimal defaultPrice, boolean active, Instant createdAt) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getCategory(), p.getDescription(),
                p.getUnit(), p.getDefaultPrice(), p.isActive(), p.getCreatedAt());
    }
}