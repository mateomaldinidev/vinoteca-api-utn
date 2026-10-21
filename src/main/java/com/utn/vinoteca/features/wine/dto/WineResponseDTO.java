package com.utn.vinoteca.features.wine.dto;

import com.utn.vinoteca.features.grapeVariety.dto.GrapeVarietySummaryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WineResponseDTO {

    @Schema(description = "Unique external identifier of the wine", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID externalId;

    @Schema(description = "Wine name", example = "Malbec Reserva")
    private String name;

    @Schema(description = "Wine description", example = "Malbec de guarda con notas de frutos rojos")
    private String description;

    @Schema(description = "Harvest year", example = "2021")
    private Integer vintage;

    @Schema(description = "Wine region", example = "Valle de Uco")
    private String region;

    @Schema(description = "Alcohol by volume percentage", example = "13.5")
    private BigDecimal alcohol;

    @Schema(description = "Tasting notes", example = "Aromas de ciruela y violetas, final persistente")
    private String tastingNotes;

    @Schema(description = "Indicates if the wine is active", example = "true")
    private Boolean active;

    @Schema(description = "Grape varieties that compose the wine")
    private List<GrapeVarietySummaryDTO> grapeVarieties;

    @Schema(description = "External identifier of the distributor", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID distributorId;

    @Schema(description = "Distributor name", example = "Distribuidora del Oeste")
    private String distributorName;

    @Schema(description = "External identifier of the wine type", example = "550e8400-e29b-41d4-a716-446655440002")
    private UUID wineTypeId;

    @Schema(description = "Wine type name", example = "Tintos")
    private String wineTypeName;

    @Schema(description = "External identifier of the winery", example = "550e8400-e29b-41d4-a716-446655440003")
    private UUID wineryId;

    @Schema(description = "Winery name", example = "Zuccardi")
    private String wineryName;
}
