package com.utn.vinoteca.features.offer_product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record OfferProductResponseDTO(
        @Schema(description = "External identifier of the offer", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID offerExternalId,
        @Schema(description = "External identifier of the wine variant", example = "550e8400-e29b-41d4-a716-446655440001")
        UUID wineVariantExternalId,
        @Schema(description = "Discounted price for the wine variant in the offer", example = "1275.00")
        BigDecimal discountedPrice


) { }
