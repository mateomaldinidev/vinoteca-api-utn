package com.utn.corralon.features.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be less than 255 characters")
    @Schema(description = "Product name", example = "Cemento Portland 50kg")
    private String name;

    @Size(max = 255, message = "Description must be less than 255 characters")
    @Schema(description = "Product description", example = "Cemento Portland de alta resistencia para construcción")
    private String description;

    @NotNull(message = "Supplier is required")
    @Schema(description = "External identifier of the supplier", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID supplierId;

    @NotNull(message = "Category is required")
    @Schema(description = "External identifier of the category", example = "550e8400-e29b-41d4-a716-446655440002")
    private UUID categoryId;

    @NotNull(message = "Brand is required")
    @Schema(description = "External identifier of the brand", example = "550e8400-e29b-41d4-a716-446655440003")
    private UUID brandId;
}
