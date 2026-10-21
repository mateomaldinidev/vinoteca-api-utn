package com.utn.vinoteca.features.winery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WineryResponseDTO {

    @Schema(description = "Unique external identifier of the winery", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID externalId;

    @Schema(description = "Winery name", example = "Loma Negra")
    private String name;

    @Schema(description = "Indicates if the winery is active", example = "true")
    private Boolean active;
}
