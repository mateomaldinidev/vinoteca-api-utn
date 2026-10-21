package com.utn.corralon.features.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.SecondaryRow;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductResponseDTO{

    @Schema(description = "Unique external identifier of the product", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID externalId;

    @Schema(description = "Product name", example = "Cemento Portland 50kg")
    private String name;

    @Schema(description = "Product description", example = "Cemento Portland de alta resistencia para construcción")
    private String description;

    @Schema(description = "Indicates if the product is active", example = "true")
    private Boolean active;

    @Schema(description = "External identifier of the supplier", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID supplierId;

    @Schema(description = "Supplier name", example = "Proveedores S.A.")
    private String supplierName;

    @Schema(description = "External identifier of the category", example = "550e8400-e29b-41d4-a716-446655440002")
    private UUID categoryId;

    @Schema(description = "Category name", example = "Cementos")
    private String categoryName;

    @Schema(description = "External identifier of the brand", example = "550e8400-e29b-41d4-a716-446655440003")
    private UUID brandId;

    @Schema(description = "Brand name", example = "Loma Negra")
    private String brandName;

}
