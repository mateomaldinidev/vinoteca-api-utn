package com.utn.corralon.features.category.controller;

import com.utn.corralon.features.category.dto.CategoryRequestDTO;
import com.utn.corralon.features.category.dto.CategoryResponseDTO;
import com.utn.corralon.features.category.service.ICategoryService;
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
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Endpoints para la gestion de categorias")
public class CategoryController {

    private final ICategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasAuthority('CATEGORY_CREATE')")
    @Operation(summary = "Crear categoria", description = "Crea una nueva categoria")
    @ApiResponse(responseCode = "201", description = "Categoria creada exitosamente")
    public ResponseEntity<CategoryResponseDTO> create(@RequestBody @Valid CategoryRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(dto));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CATEGORY_LIST')")
    @Operation(summary = "Listar categorias activas", description = "Retorna todas las categorias activas")
    public ResponseEntity<List<CategoryResponseDTO>> getAll() {
        return ResponseEntity.ok(categoryService.getAll());
    }

    @GetMapping("/{externalId}")
    @PreAuthorize("hasAuthority('CATEGORY_READ')")
    @Operation(summary = "Obtener categoria por ID", description = "Retorna una categoria activa por su externalId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria encontrada"),
            @ApiResponse(responseCode = "404", description = "Categoria no encontrada")
    })
    public ResponseEntity<CategoryResponseDTO> getByExternalId(
            @Parameter(description = "ID externo de la categoria") @PathVariable UUID externalId) {
        return ResponseEntity.ok(categoryService.getByExternalId(externalId));
    }

    @PutMapping("/{externalId}")
    @PreAuthorize("hasAuthority('CATEGORY_UPDATE')")
    @Operation(summary = "Actualizar categoria", description = "Actualiza los datos de una categoria existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria actualizada"),
            @ApiResponse(responseCode = "404", description = "Categoria no encontrada")
    })
    public ResponseEntity<CategoryResponseDTO> update(
            @Parameter(description = "ID externo de la categoria") @PathVariable UUID externalId,
            @RequestBody @Valid CategoryRequestDTO dto) {
        return ResponseEntity.ok(categoryService.update(externalId, dto));
    }

    @DeleteMapping("/{externalId}")
    @PreAuthorize("hasAuthority('CATEGORY_DELETE')")
    @Operation(summary = "Eliminar categoria", description = "Eliminacion logica de una categoria. Tambien desactiva sus productos y variantes")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoria eliminada"),
            @ApiResponse(responseCode = "404", description = "Categoria no encontrada")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID externo de la categoria") @PathVariable UUID externalId) {
        categoryService.delete(externalId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{externalId}/products/count")
    @PreAuthorize("hasAuthority('CATEGORY_DELETE')")
    @Operation(summary = "Contar productos asociados", description = "Retorna la cantidad de productos relacionados con la categoria")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cantidad de productos asociados"),
            @ApiResponse(responseCode = "404", description = "Categoria no encontrada")
    })
    public ResponseEntity<RelatedProductsCountDTO> countAssociatedProducts(
            @Parameter(description = "ID externo de la categoria") @PathVariable UUID externalId) {
        return ResponseEntity.ok(new RelatedProductsCountDTO(categoryService.countAssociatedProducts(externalId)));
    }

    @PatchMapping("/{externalId}/activate")
    @PreAuthorize("hasAuthority('CATEGORY_ACTIVATE')")
    @Operation(summary = "Activar categoria", description = "Activa una categoria previamente desactivada")
    @ApiResponse(responseCode = "204", description = "Categoria activada")
    public ResponseEntity<Void> activate(
            @Parameter(description = "ID externo de la categoria") @PathVariable UUID externalId) {
        categoryService.activate(externalId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inactive")
    @PreAuthorize("hasAuthority('CATEGORY_LIST_INACTIVE')")
    @Operation(summary = "Listar categorias inactivas", description = "Retorna todas las categorias inactivas")
    public ResponseEntity<List<CategoryResponseDTO>> getInactive() {
        return ResponseEntity.ok(categoryService.getInactive());
    }
}
