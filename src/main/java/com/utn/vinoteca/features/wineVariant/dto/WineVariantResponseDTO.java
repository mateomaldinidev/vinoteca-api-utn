package com.utn.vinoteca.features.wineVariant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WineVariantResponseDTO {
    @Schema(description = "Unique external identifier of the wine variant", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID externalId;

    @Schema(description = "Variant attribute (e.g. bottle size, packaging)", example = "Botella 750 ml")
    private String attribute;

    @Schema(description = "Selling price of the variant", example = "1500.00")
    private BigDecimal price;

    @Schema(description = "Available stock quantity", example = "100")
    private Integer stock;

    @Schema(description = "Indicates if the variant is active", example = "true")
    private Boolean active;

    @Schema(description = "Wholesale price of the variant", example = "1200.00")
    private BigDecimal wholesalePrice;

    @Schema(description = "Minimum quantity required for wholesale price", example = "10")
    private Integer wholesaleMinQty;

    @Schema(description = "External identifier of the wine", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID wineId;

    @Schema(description = "Wine name", example = "Malbec Reserva")
    private String wineName;
}
