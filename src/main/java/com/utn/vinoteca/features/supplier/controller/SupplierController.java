package com.utn.vinoteca.features.supplier.controller;

import com.utn.vinoteca.features.supplier.dto.SupplierRequestDTO;
import com.utn.vinoteca.features.supplier.dto.SupplierResponseDTO;
import com.utn.vinoteca.features.supplier.service.ISupplierService;
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
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@Tag(name = "Suppliers", description = "Endpoints para la gestion de proveedores")
public class SupplierController {

    private final ISupplierService supplierService;

    @PostMapping
    @PreAuthorize("hasAuthority('SUPPLIER_CREATE')")
    @Operation(summary = "Crear proveedor", description = "Crea un nuevo proveedor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proveedor creado exitosamente",
                    content = @Content(schema = @Schema(implementation = SupplierResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<SupplierResponseDTO> create(@RequestBody @Valid SupplierRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(dto));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIER_LIST')")
    @Operation(summary = "Listar proveedores activos", description = "Retorna todos los proveedores activos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proveedores encontrados",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SupplierResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<SupplierResponseDTO>> getAll() {
        return ResponseEntity.ok(supplierService.getAll());
    }

    @GetMapping("/{externalId}")
    @PreAuthorize("hasAuthority('SUPPLIER_READ')")
    @Operation(summary = "Obtener proveedor por ID", description = "Retorna un proveedor activo por su externalId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proveedor encontrado",
                    content = @Content(schema = @Schema(implementation = SupplierResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Proveedor no encontrado")
    })
    public ResponseEntity<SupplierResponseDTO> getByExternalId(
            @Parameter(description = "ID externo del proveedor") @PathVariable UUID externalId) {
        return ResponseEntity.ok(supplierService.getByExternalId(externalId));
    }

    @PutMapping("/{externalId}")
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    @Operation(summary = "Actualizar proveedor", description = "Actualiza los datos de un proveedor existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proveedor actualizado",
                    content = @Content(schema = @Schema(implementation = SupplierResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Proveedor no encontrado")
    })
    public ResponseEntity<SupplierResponseDTO> update(
            @Parameter(description = "ID externo del proveedor") @PathVariable UUID externalId,
            @RequestBody @Valid SupplierRequestDTO dto) {
        return ResponseEntity.ok(supplierService.update(externalId, dto));
    }

    @DeleteMapping("/{externalId}")
    @PreAuthorize("hasAuthority('SUPPLIER_DELETE')")
    @Operation(summary = "Eliminar proveedor", description = "Eliminacion logica de un proveedor")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Proveedor eliminado"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Proveedor no encontrado")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID externo del proveedor") @PathVariable UUID externalId) {
        supplierService.delete(externalId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{externalId}/activate")
    @PreAuthorize("hasAuthority('SUPPLIER_ACTIVATE')")
    @Operation(summary = "Activar proveedor", description = "Activa un proveedor previamente desactivado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Proveedor activado"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Proveedor no encontrado")
    })
    public ResponseEntity<Void> activate(
            @Parameter(description = "ID externo del proveedor") @PathVariable UUID externalId) {
        supplierService.activate(externalId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inactive")
    @PreAuthorize("hasAuthority('SUPPLIER_LIST_INACTIVE')")
    @Operation(summary = "Listar proveedores inactivos", description = "Retorna todos los proveedores inactivos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proveedores inactivos encontrados",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SupplierResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<SupplierResponseDTO>> getInactive() {
        return ResponseEntity.ok(supplierService.getInactive());
    }
}
