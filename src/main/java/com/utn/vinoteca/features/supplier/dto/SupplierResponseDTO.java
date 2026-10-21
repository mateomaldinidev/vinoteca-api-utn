package com.utn.vinoteca.features.supplier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SupplierResponseDTO{
    @NotBlank
    @Schema(description = "Unique external identifier of the supplier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID externalId;

    @NotBlank
    @Schema(description = "Supplier name", example = "Proveedores S.A.")
    private String name;

    @NotBlank
    @Schema(description = "Supplier contact information", example = "contacto@proveedores.com")
    private String contact;

    @NotBlank
    @Schema(description = "Indicates if the supplier is active", example = "true")
    private Boolean active;
}
