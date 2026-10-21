package com.utn.vinoteca.features.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponseDTO{
    @NotBlank
    @Schema(description = "Unique external identifier of the category", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID externalId;

    @NotBlank
    @Schema(description = "Category name", example = "Cementos")
    private String name;

    @NotBlank
    @Schema(description = "Indicates if the category is active", example = "true")
    private boolean active;
}
