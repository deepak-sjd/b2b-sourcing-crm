package com.doctutorials.crm.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 100) String category,
        @Size(max = 1000) String description,
        @Size(max = 30) String unit,
        @DecimalMin("0") BigDecimal defaultPrice,
        Boolean active
) {}