package com.utn.corralon.features.address.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequestDTO {
    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "Street number is required")
    private String streetNumber;

    private String floor;

    private String apartmentNumber;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Zip code is required")
    private String zipCode;

    /**
     * Campo mantenido por compatibilidad con clientes existentes.
     * Nunca se utiliza como fuente de identidad: el propietario de la
     * dirección siempre se deriva del usuario autenticado (JWT).
     */
    private UUID userExternalId;
}