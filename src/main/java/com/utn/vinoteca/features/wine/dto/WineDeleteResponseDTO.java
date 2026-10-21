package com.utn.vinoteca.features.wine.dto;

import com.utn.vinoteca.features.wineVariant.dto.DisabledVariantDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class WineDeleteResponseDTO {
    @Schema(description = "Unique external identifier of the wine", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID wineExternalId;

    @Schema(description = "Wine name", example = "Cemento Portland 50kg")
    private String wineName;

    @Schema(description = "Indicates if the wine is active", example = "false")
    private Boolean active;

    @Schema(description = "List of disabled wine variants")
    private List<DisabledVariantDTO> disabledVariants;

}
