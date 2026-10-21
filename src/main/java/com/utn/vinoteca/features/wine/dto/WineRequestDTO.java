package com.utn.vinoteca.features.wine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WineRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be less than 255 characters")
    @Schema(description = "Wine name", example = "Malbec Reserva")
    private String name;

    @Size(max = 255, message = "Description must be less than 255 characters")
    @Schema(description = "Wine description", example = "Malbec de guarda con notas de frutos rojos")
    private String description;

    @Min(value = 1900, message = "Vintage must be 1900 or later")
    @Schema(description = "Harvest year", example = "2021")
    private Integer vintage;

    @Size(max = 120, message = "Region must be less than 120 characters")
    @Schema(description = "Wine region", example = "Valle de Uco")
    private String region;

    @DecimalMin(value = "0.0", message = "Alcohol must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Alcohol must be between 0 and 100")
    @Schema(description = "Alcohol by volume percentage", example = "13.5")
    private BigDecimal alcohol;

    @Size(max = 1000, message = "Tasting notes must be less than 1000 characters")
    @Schema(description = "Tasting notes", example = "Aromas de ciruela y violetas, final persistente")
    private String tastingNotes;

    @NotEmpty(message = "At least one grape variety is required")
    @Schema(description = "External identifiers of the grape varieties")
    private Set<UUID> grapeVarietyIds;

    @NotNull(message = "Distributor is required")
    @Schema(description = "External identifier of the distributor", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID distributorId;

    @NotNull(message = "Wine type is required")
    @Schema(description = "External identifier of the wine type", example = "550e8400-e29b-41d4-a716-446655440002")
    private UUID wineTypeId;

    @NotNull(message = "Winery is required")
    @Schema(description = "External identifier of the winery", example = "550e8400-e29b-41d4-a716-446655440003")
    private UUID wineryId;
}
