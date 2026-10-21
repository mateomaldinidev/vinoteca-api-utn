package com.utn.corralon.features.category.controller;

import com.utn.corralon.features.category.dto.CategoryRequestDTO;
import com.utn.corralon.features.category.dto.CategoryResponseDTO;
import com.utn.corralon.features.category.service.ICategoryService;
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
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Endpoints para la gestion de categorias")
public class CategoryController {

    private final ICategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasAuthority('CATEGORY_CREATE')")
    @Operation(summary = "Crear categoria", description = "Crea una nueva categoria")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoria creada exitosamente",
                    content = @Content(schema = @Schema(implementation = CategoryResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<CategoryResponseDTO> create(@RequestBody @Valid CategoryRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(dto));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CATEGORY_LIST')")
    @Operation(summary = "Listar categorias activas", description = "Retorna todas las categorias activas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categorias encontradas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CategoryResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<CategoryResponseDTO>> getAll() {
        return ResponseEntity.ok(categoryService.getAll());
    }

    @GetMapping("/{externalId}")
    @PreAuthorize("hasAuthority('CATEGORY_READ')")
    @Operation(summary = "Obtener categoria por ID", description = "Retorna una categoria activa por su externalId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria encontrada",
                    content = @Content(schema = @Schema(implementation = CategoryResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
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
            @ApiResponse(responseCode = "200", description = "Categoria actualizada",
                    content = @Content(schema = @Schema(implementation = CategoryResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Categoria no encontrada")
    })
    public ResponseEntity<CategoryResponseDTO> update(
            @Parameter(description = "ID externo de la categoria") @PathVariable UUID externalId,
            @RequestBody @Valid CategoryRequestDTO dto) {
        return ResponseEntity.ok(categoryService.update(externalId, dto));
    }

    @DeleteMapping("/{externalId}")
    @PreAuthorize("hasAuthority('CATEGORY_DELETE')")
    @Operation(summary = "Eliminar categoria", description = "Eliminacion logica de una categoria")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoria eliminada"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Categoria no encontrada")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID externo de la categoria") @PathVariable UUID externalId) {
        categoryService.delete(externalId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{externalId}/activate")
    @PreAuthorize("hasAuthority('CATEGORY_ACTIVATE')")
    @Operation(summary = "Activar categoria", description = "Activa una categoria previamente desactivada")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoria activada"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Categoria no encontrada")
    })
    public ResponseEntity<Void> activate(
            @Parameter(description = "ID externo de la categoria") @PathVariable UUID externalId) {
        categoryService.activate(externalId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inactive")
    @PreAuthorize("hasAuthority('CATEGORY_LIST_INACTIVE')")
    @Operation(summary = "Listar categorias inactivas", description = "Retorna todas las categorias inactivas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categorias inactivas encontradas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CategoryResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<CategoryResponseDTO>> getInactive() {
        return ResponseEntity.ok(categoryService.getInactive());
    }
}
