package com.utn.corralon.features.supplier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.* ;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SupplierRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be less than 255 characters")
    @Schema(description = "Supplier name", example = "Proveedores S.A.")
    private String name;

    @NotBlank(message = "Contact is required")
    @Size(max = 255, message = "Contact must be less than 255 characters")
    @Schema(description = "Supplier contact information", example = "contacto@proveedores.com")
    private String contact;

    @Schema(description = "Indicates if the supplier is active", example = "true")
    private Boolean active;
}
