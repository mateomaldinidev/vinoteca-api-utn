package com.utn.vinoteca.features.wineVariant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WineVariantRequestDTO {
    @NotBlank(message = "Attribute is required")
    @Size(max = 255, message = "Attribute must be less than 255 characters")
    @Schema(description = "Variant attribute (e.g. bottle size, packaging)", example = "Botella 750 ml")
    private String attribute;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    @Schema(description = "Selling price of the variant", example = "1500.00")
    private BigDecimal price;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    @Schema(description = "Available stock quantity", example = "100")
    private Integer stock;

    @Positive(message = "Wholesale price must be positive")
    @Schema(description = "Wholesale price of the variant", example = "1200.00")
    private BigDecimal wholesalePrice;

    @Min(value = 1, message = "Wholesale minimum quantity must be at least 1")
    @Schema(description = "Minimum quantity required for wholesale price", example = "10")
    private Integer wholesaleMinQty;

    @NotNull(message = "Wine is required")
    @Schema(description = "External identifier of the wine", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID wineId;

}
