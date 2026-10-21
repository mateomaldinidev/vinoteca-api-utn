package com.utn.vinoteca.features.grapeVariety.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GrapeVarietySummaryDTO {

    @Schema(description = "Unique external identifier of the grape variety", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID externalId;

    @Schema(description = "Grape variety name", example = "Malbec")
    private String name;
}
