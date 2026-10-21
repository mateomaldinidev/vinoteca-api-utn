package com.utn.vinoteca.features.wineType.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WineTypeResponseDTO{
    @NotBlank
    @Schema(description = "Unique external identifier of the wineType", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID externalId;

    @NotBlank
    @Schema(description = "Wine type name", example = "Tintos")
    private String name;

    @NotBlank
    @Schema(description = "Indicates if the wineType is active", example = "true")
    private boolean active;
}
