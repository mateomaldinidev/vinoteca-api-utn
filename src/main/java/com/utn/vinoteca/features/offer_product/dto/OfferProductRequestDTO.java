package com.utn.vinoteca.features.offer_product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class OfferProductRequestDTO {
    @NotNull(message = "Offer ID is required")
    @Schema(description = "External identifier of the offer", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID offerId;

    @NotNull(message = "Wine variant ID is required")
    @Schema(description = "External identifier of the wine variant", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID wineVariantId;

    @NotNull(message = "Discounted price is required")
    @Positive(message = "Discounted price must be greater than 0")
    @Schema(description = "Discounted price for the wine variant in the offer", example = "1275.00")
    private BigDecimal discountedPrice;
}
