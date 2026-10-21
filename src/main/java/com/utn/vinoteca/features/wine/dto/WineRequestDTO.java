package com.utn.vinoteca.features.wine.dto;

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
public class WineRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be less than 255 characters")
    @Schema(description = "Wine name", example = "Cemento Portland 50kg")
    private String name;

    @Size(max = 255, message = "Description must be less than 255 characters")
    @Schema(description = "Wine description", example = "Cemento Portland de alta resistencia para construcción")
    private String description;

    @NotNull(message = "Distributor is required")
    @Schema(description = "External identifier of the distributor", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID distributorId;

    @NotNull(message = "WineType is required")
    @Schema(description = "External identifier of the wineType", example = "550e8400-e29b-41d4-a716-446655440002")
    private UUID wineTypeId;

    @NotNull(message = "Winery is required")
    @Schema(description = "External identifier of the winery", example = "550e8400-e29b-41d4-a716-446655440003")
    private UUID wineryId;
}
