package com.utn.vinoteca.features.address.controller;

import com.utn.vinoteca.features.address.dto.AddressRequestDTO;
import com.utn.vinoteca.features.address.dto.AddressResponseDTO;
import com.utn.vinoteca.features.address.service.IAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses", description = "Endpoints para la gestion de direcciones")
public class AddressController {

    private final IAddressService addressService;

    @PostMapping
    @PreAuthorize("hasAuthority('ADDRESS_CREATE')")
    @Operation(summary = "Crear direccion", description = "Crea una nueva direccion")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Direccion creada exitosamente",
                    content = @Content(schema = @Schema(implementation = AddressResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<AddressResponseDTO> create(@RequestBody @Valid AddressRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.create(dto));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADDRESS_LIST')")
    @Operation(summary = "Listar direcciones", description = "Retorna todas las direcciones")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Direcciones encontradas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AddressResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<AddressResponseDTO>> getAll() {
        return ResponseEntity.ok(addressService.getAll());
    }

    @GetMapping("/{externalId}")
    @PreAuthorize("hasAuthority('ADDRESS_READ')")
    @Operation(summary = "Obtener direccion por ID", description = "Retorna una direccion por su externalId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Direccion encontrada",
                    content = @Content(schema = @Schema(implementation = AddressResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Direccion no encontrada")
    })
    public ResponseEntity<AddressResponseDTO> getByExternalId(
            @Parameter(description = "ID externo de la direccion") @PathVariable UUID externalId) {
        return ResponseEntity.ok(addressService.getByExternalId(externalId));
    }

    @PutMapping("/{externalId}")
    @PreAuthorize("hasAuthority('ADDRESS_UPDATE')")
    @Operation(summary = "Actualizar direccion", description = "Actualiza los datos de una direccion existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Direccion actualizada",
                    content = @Content(schema = @Schema(implementation = AddressResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Direccion no encontrada")
    })
    public ResponseEntity<AddressResponseDTO> update(
            @Parameter(description = "ID externo de la direccion") @PathVariable UUID externalId,
            @RequestBody @Valid AddressRequestDTO dto) {
        return ResponseEntity.ok(addressService.update(externalId, dto));
    }

    @DeleteMapping("/{externalId}")
    @PreAuthorize("hasAuthority('ADDRESS_DELETE')")
    @Operation(summary = "Eliminar direccion", description = "Elimina una direccion existente")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Direccion eliminada"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Direccion no encontrada")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID externo de la direccion") @PathVariable UUID externalId,
            @Parameter(description = "ID externo del usuario") @RequestParam UUID userExternalId) {
        addressService.delete(externalId, userExternalId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userExternalId}")
    @PreAuthorize("hasAuthority('ADDRESS_LIST_BY_USER')")
    @Operation(summary = "Listar direcciones de usuario", description = "Retorna todas las direcciones de un usuario especifico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Direcciones encontradas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AddressResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<AddressResponseDTO>> getAllByUserExternalId(
            @Parameter(description = "ID externo del usuario") @PathVariable UUID userExternalId) {
        return ResponseEntity.ok(addressService.getAllByUserExternalId(userExternalId));
    }
}
