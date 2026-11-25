package com.doctutorials.crm.product;

import com.doctutorials.crm.tenant.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product extends TenantEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String category;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 30)
    private String unit = "Nos";

    @Column(precision = 14, scale = 2)
    private BigDecimal defaultPrice;

    @Column(nullable = false)
    private boolean active = true;
}