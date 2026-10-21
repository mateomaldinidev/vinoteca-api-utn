package com.utn.vinoteca.features.wineVariant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DisabledVariantDTO {
    @Schema(description = "Unique external identifier of the disabled variant", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID externalId;

    @Schema(description = "Variant attribute (e.g. bottle size, packaging)", example = "Botella 750 ml")
    private String attribute;
}
