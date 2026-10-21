package com.utn.corralon.features.address.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequestDTO {
    @NotBlank(message = "Street is required")
    @Schema(description = "Street name", example = "Av. Corrientes")
    private String street;

    @NotBlank(message = "Street number is required")
    @Schema(description = "Street number", example = "1234")
    private String streetNumber;

    @Schema(description = "Floor number", example = "3")
    private String floor;

    @Schema(description = "Apartment number", example = "B")
    private String apartmentNumber;

    @NotBlank(message = "City is required")
    @Schema(description = "City name", example = "Buenos Aires")
    private String city;

    @NotBlank(message = "Zip code is required")
    @Schema(description = "Zip code", example = "1043")
    private String zipCode;

    @NotNull(message = "User ID cannot be null")
    @Schema(description = "External identifier of the user", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID userExternalId;
}
