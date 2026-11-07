package com.utn.corralon.features.brand.controller;

import com.utn.corralon.features.brand.dto.BrandRequestDTO;
import com.utn.corralon.features.brand.dto.BrandResponseDTO;
import com.utn.corralon.features.brand.service.IBrandService;
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
@RequestMapping("/api/brands")
@RequiredArgsConstructor
@Tag(name = "Brands", description = "Endpoints para la gestion de marcas")
public class BrandController {
    private final IBrandService brandService;

    @PostMapping
    @PreAuthorize("hasAuthority('BRAND_CREATE')")
    @Operation(summary = "Crear marca", description = "Crea una nueva marca")
    @ApiResponse(responseCode = "201", description = "Marca creada exitosamente")
    public ResponseEntity<BrandResponseDTO> create(@RequestBody @Valid BrandRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(brandService.create(dto));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('BRAND_LIST')")
    @Operation(summary = "Listar marcas activas", description = "Retorna todas las marcas activas")
    public ResponseEntity<List<BrandResponseDTO>> getAll() {
        return ResponseEntity.ok(brandService.getAll());
    }

    @GetMapping("/{externalId}")
    @PreAuthorize("hasAuthority('BRAND_READ')")
    @Operation(summary = "Obtener marca por ID", description = "Retorna una marca activa por su externalId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Marca encontrada"),
            @ApiResponse(responseCode = "404", description = "Marca no encontrada")
    })
    public ResponseEntity<BrandResponseDTO> getByExternalId(
            @Parameter(description = "ID externo de la marca") @PathVariable UUID externalId) {
        return ResponseEntity.ok(brandService.getByExternalId(externalId));
    }

    @PutMapping("/{externalId}")
    @PreAuthorize("hasAuthority('BRAND_UPDATE')")
    @Operation(summary = "Actualizar marca", description = "Actualiza los datos de una marca existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Marca actualizada"),
            @ApiResponse(responseCode = "404", description = "Marca no encontrada")
    })
    public ResponseEntity<BrandResponseDTO> update(
            @Parameter(description = "ID externo de la marca") @PathVariable UUID externalId,
            @RequestBody @Valid BrandRequestDTO dto) {
        return ResponseEntity.ok(brandService.update(externalId, dto));
    }

    @DeleteMapping("/{externalId}")
    @PreAuthorize("hasAuthority('BRAND_DELETE')")
    @Operation(summary = "Eliminar marca", description = "Eliminacion logica de una marca. Tambien desactiva sus productos y variantes")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Marca eliminada"),
            @ApiResponse(responseCode = "404", description = "Marca no encontrada")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID externo de la marca") @PathVariable UUID externalId) {
        brandService.delete(externalId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{externalId}/products/count")
    @PreAuthorize("hasAuthority('BRAND_DELETE')")
    @Operation(summary = "Contar productos asociados", description = "Retorna la cantidad de productos relacionados con la marca")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cantidad de productos asociados"),
            @ApiResponse(responseCode = "404", description = "Marca no encontrada")
    })
    public ResponseEntity<RelatedProductsCountDTO> countAssociatedProducts(
            @Parameter(description = "ID externo de la marca") @PathVariable UUID externalId) {
        return ResponseEntity.ok(new RelatedProductsCountDTO(brandService.countAssociatedProducts(externalId)));
    }

    @PatchMapping("/{externalId}/activate")
    @PreAuthorize("hasAuthority('BRAND_ACTIVATE')")
    @Operation(summary = "Activar marca", description = "Activa una marca previamente desactivada")
    @ApiResponse(responseCode = "204", description = "Marca activada")
    public ResponseEntity<Void> activate(
            @Parameter(description = "ID externo de la marca") @PathVariable UUID externalId) {
        brandService.activate(externalId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inactive")
    @PreAuthorize("hasAuthority('BRAND_LIST_INACTIVE')")
    @Operation(summary = "Listar marcas inactivas", description = "Retorna todas las marcas inactivas")
    public ResponseEntity<List<BrandResponseDTO>> getInactive() {
        return ResponseEntity.ok(brandService.getInactive());
    }
}
