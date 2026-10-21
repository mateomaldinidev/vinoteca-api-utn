package com.utn.corralon.features.product.dto;

import com.utn.corralon.features.productVariant.dto.DisabledVariantDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ProductDeleteResponseDTO {
    @Schema(description = "Unique external identifier of the product", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID productExternalId;

    @Schema(description = "Product name", example = "Cemento Portland 50kg")
    private String productName;

    @Schema(description = "Indicates if the product is active", example = "false")
    private Boolean active;

    @Schema(description = "List of disabled product variants")
    private List<DisabledVariantDTO> disabledVariants;

}
