package com.utn.vinoteca.features.offer.dto;

import com.utn.vinoteca.features.offerProduct.dto.OfferProductResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OfferResponseDTO(
    @Schema(description = "Unique external identifier of the offer", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID externalId,
    @Schema(description = "Offer name", example = "Descuento de Invierno")
    String name,
    @Schema(description = "Discount percentage applied by the offer", example = "15.00")
    BigDecimal discountPercentage,
    @Schema(description = "Offer start date and time", example = "2025-01-15T10:30:00")
    LocalDateTime startDate,
    @Schema(description = "Offer end date and time", example = "2025-02-15T23:59:59")
    LocalDateTime endDate,
    @Schema(description = "Indicates if the offer is active", example = "true")
    Boolean active,
    @Schema(description = "List of wines included in the offer")
    List<OfferProductResponseDTO> offerProducts
){}
