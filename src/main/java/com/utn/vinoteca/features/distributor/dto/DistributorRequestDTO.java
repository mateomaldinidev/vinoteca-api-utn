package com.utn.vinoteca.features.distributor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.* ;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DistributorRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be less than 255 characters")
    @Schema(description = "Distributor name", example = "Proveedores S.A.")
    private String name;

    @NotBlank(message = "Contact is required")
    @Size(max = 255, message = "Contact must be less than 255 characters")
    @Schema(description = "Distributor contact information", example = "contacto@proveedores.com")
    private String contact;

    @Schema(description = "Indicates if the distributor is active", example = "true")
    private Boolean active;
}
