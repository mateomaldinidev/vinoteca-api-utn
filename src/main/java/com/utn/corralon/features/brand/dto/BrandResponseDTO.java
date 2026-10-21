package com.utn.corralon.features.brand.dto;

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
public class BrandResponseDTO {

    @Schema(description = "Unique external identifier of the brand", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID externalId;

    @Schema(description = "Brand name", example = "Loma Negra")
    private String name;

    @Schema(description = "Indicates if the brand is active", example = "true")
    private Boolean active;
}
