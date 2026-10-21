package com.utn.corralon.features.offer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OfferRequestDTO {
    @NotBlank(message = "Name must not be blank")
    @Schema(description = "Offer name", example = "Descuento de Invierno")
    private String name;

    @NotNull(message = "Discount percentage is required")
    @Positive(message = "Discount percentage must be greater than 0")
    @Schema(description = "Discount percentage applied by the offer", example = "15.00")
    private BigDecimal discountPercentage;

    @NotNull(message = "Start date is required")
    @Schema(description = "Offer start date and time", example = "2025-01-15T10:30:00")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    @Schema(description = "Offer end date and time", example = "2025-02-15T23:59:59")
    private LocalDateTime endDate;
}
