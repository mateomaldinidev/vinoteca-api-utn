package com.utn.vinoteca.features.distributor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DistributorResponseDTO{
    @NotBlank
    @Schema(description = "Unique external identifier of the distributor", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID externalId;

    @NotBlank
    @Schema(description = "Distributor name", example = "Distribuidora del Oeste")
    private String name;

    @NotBlank
    @Schema(description = "Distributor contact information", example = "contacto@distribuidores.com")
    private String contact;

    @NotBlank
    @Schema(description = "Indicates if the distributor is active", example = "true")
    private Boolean active;
}
