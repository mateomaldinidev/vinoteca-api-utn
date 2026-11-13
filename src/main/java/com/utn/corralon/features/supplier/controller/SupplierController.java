package com.utn.corralon.features.supplier.controller;

import com.utn.corralon.features.supplier.dto.SupplierRequestDTO;
import com.utn.corralon.features.supplier.dto.SupplierResponseDTO;
import com.utn.corralon.features.supplier.service.ISupplierService;
import com.utn.corralon.features.product.dto.RelatedProductsCountDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @ApiResponse(responseCode = "201", description = "Proveedor creado exitosamente")
    public ResponseEntity<SupplierResponseDTO> create(@RequestBody @Valid SupplierRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(dto));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIER_LIST')")
    @Operation(summary = "Listar proveedores activos", description = "Retorna todos los proveedores activos")
    public ResponseEntity<List<SupplierResponseDTO>> getAll() {
        return ResponseEntity.ok(supplierService.getAll());
    }

    @GetMapping("/{externalId}")
    @PreAuthorize("hasAuthority('SUPPLIER_READ')")
    @Operation(summary = "Obtener proveedor por ID", description = "Retorna un proveedor activo por su externalId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proveedor encontrado"),
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
            @ApiResponse(responseCode = "200", description = "Proveedor actualizado"),
            @ApiResponse(responseCode = "404", description = "Proveedor no encontrado")
    })
    public ResponseEntity<SupplierResponseDTO> update(
            @Parameter(description = "ID externo del proveedor") @PathVariable UUID externalId,
            @RequestBody @Valid SupplierRequestDTO dto) {
        return ResponseEntity.ok(supplierService.update(externalId, dto));
    }

    @DeleteMapping("/{externalId}")
    @PreAuthorize("hasAuthority('SUPPLIER_DELETE')")
    @Operation(summary = "Eliminar proveedor", description = "Eliminacion logica de un proveedor. Tambien desactiva sus productos y variantes")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Proveedor eliminado"),
            @ApiResponse(responseCode = "404", description = "Proveedor no encontrado")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID externo del proveedor") @PathVariable UUID externalId) {
        supplierService.delete(externalId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{externalId}/products/count")
    @PreAuthorize("hasAuthority('SUPPLIER_DELETE')")
    @Operation(summary = "Contar productos asociados", description = "Retorna la cantidad de productos relacionados con el proveedor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cantidad de productos asociados"),
            @ApiResponse(responseCode = "404", description = "Proveedor no encontrado")
    })
    public ResponseEntity<RelatedProductsCountDTO> countAssociatedProducts(
            @Parameter(description = "ID externo del proveedor") @PathVariable UUID externalId) {
        return ResponseEntity.ok(new RelatedProductsCountDTO(supplierService.countAssociatedProducts(externalId)));
    }

    @PatchMapping("/{externalId}/activate")
    @PreAuthorize("hasAuthority('SUPPLIER_ACTIVATE')")
    @Operation(summary = "Activar proveedor", description = "Activa un proveedor previamente desactivado")
    @ApiResponse(responseCode = "204", description = "Proveedor activado")
    public ResponseEntity<Void> activate(
            @Parameter(description = "ID externo del proveedor") @PathVariable UUID externalId) {
        supplierService.activate(externalId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inactive")
    @PreAuthorize("hasAuthority('SUPPLIER_LIST_INACTIVE')")
    @Operation(summary = "Listar proveedores inactivos", description = "Retorna todos los proveedores inactivos")
    public ResponseEntity<List<SupplierResponseDTO>> getInactive() {
        return ResponseEntity.ok(supplierService.getInactive());
    }
}
