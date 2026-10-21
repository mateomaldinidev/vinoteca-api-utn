package com.utn.vinoteca.features.wine.dto;

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
public class WineResponseDTO{

    @Schema(description = "Unique external identifier of the wine", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID externalId;

    @Schema(description = "Wine name", example = "Cemento Portland 50kg")
    private String name;

    @Schema(description = "Wine description", example = "Cemento Portland de alta resistencia para construcción")
    private String description;

    @Schema(description = "Indicates if the wine is active", example = "true")
    private Boolean active;

    @Schema(description = "External identifier of the distributor", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID distributorId;

    @Schema(description = "Distributor name", example = "Proveedores S.A.")
    private String distributorName;

    @Schema(description = "External identifier of the wineType", example = "550e8400-e29b-41d4-a716-446655440002")
    private UUID wineTypeId;

    @Schema(description = "WineType name", example = "Cementos")
    private String wineTypeName;

    @Schema(description = "External identifier of the winery", example = "550e8400-e29b-41d4-a716-446655440003")
    private UUID wineryId;

    @Schema(description = "Winery name", example = "Loma Negra")
    private String wineryName;

}
