package com.utn.vinoteca.features.wineType.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok. *;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WineTypeRequestDTO {
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be less than 255 characters")
    @Schema(description = "WineType name", example = "Cementos")
    private String name;

    @Schema(description = "Indicates if the wineType is active", example = "true")
    private Boolean active;
}
