package com.utn.corralon.features.address.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class AddressResponseDTO{

    @Schema(description = "Unique external identifier of the address", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID externalId;

    @Schema(description = "Street name", example = "Av. Corrientes")
    private String street;

    @Schema(description = "Street number", example = "1234")
    private String streetNumber;

    @Schema(description = "Floor number", example = "3")
    private String floor;

    @Schema(description = "Apartment number", example = "B")
    private String apartmentNumber;

    @Schema(description = "City name", example = "Buenos Aires")
    private String city;

    @Schema(description = "Zip code", example = "1043")
    private String zipCode;

    @Schema(description = "External identifier of the user", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID userExternalId;

}
