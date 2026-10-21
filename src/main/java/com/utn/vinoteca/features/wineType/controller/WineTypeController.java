package com.utn.vinoteca.features.wineType.controller;

import com.utn.vinoteca.features.wineType.dto.WineTypeRequestDTO;
import com.utn.vinoteca.features.wineType.dto.WineTypeResponseDTO;
import com.utn.vinoteca.features.wineType.service.IWineTypeService;
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
@RequestMapping("/api/wine-types")
@RequiredArgsConstructor
@Tag(name = "Wine Types", description = "Endpoints para la gestion de categorias")
public class WineTypeController {

    private final IWineTypeService wineTypeService;

    @PostMapping
    @PreAuthorize("hasAuthority('WINE_TYPE_CREATE')")
    @Operation(summary = "Crear categoria", description = "Crea una nueva categoria")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoria creada exitosamente",
                    content = @Content(schema = @Schema(implementation = WineTypeResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<WineTypeResponseDTO> create(@RequestBody @Valid WineTypeRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(wineTypeService.create(dto));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('WINE_TYPE_LIST')")
    @Operation(summary = "Listar categorias activas", description = "Retorna todas las categorias activas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categorias encontradas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = WineTypeResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<WineTypeResponseDTO>> getAll() {
        return ResponseEntity.ok(wineTypeService.getAll());
    }

    @GetMapping("/{externalId}")
    @PreAuthorize("hasAuthority('WINE_TYPE_READ')")
    @Operation(summary = "Obtener categoria por ID", description = "Retorna una categoria activa por su externalId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria encontrada",
                    content = @Content(schema = @Schema(implementation = WineTypeResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Categoria no encontrada")
    })
    public ResponseEntity<WineTypeResponseDTO> getByExternalId(
            @Parameter(description = "ID externo de la categoria") @PathVariable UUID externalId) {
        return ResponseEntity.ok(wineTypeService.getByExternalId(externalId));
    }

    @PutMapping("/{externalId}")
    @PreAuthorize("hasAuthority('WINE_TYPE_UPDATE')")
    @Operation(summary = "Actualizar categoria", description = "Actualiza los datos de una categoria existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria actualizada",
                    content = @Content(schema = @Schema(implementation = WineTypeResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Categoria no encontrada")
    })
    public ResponseEntity<WineTypeResponseDTO> update(
            @Parameter(description = "ID externo de la categoria") @PathVariable UUID externalId,
            @RequestBody @Valid WineTypeRequestDTO dto) {
        return ResponseEntity.ok(wineTypeService.update(externalId, dto));
    }

    @DeleteMapping("/{externalId}")
    @PreAuthorize("hasAuthority('WINE_TYPE_DELETE')")
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
        wineTypeService.delete(externalId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{externalId}/activate")
    @PreAuthorize("hasAuthority('WINE_TYPE_ACTIVATE')")
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
        wineTypeService.activate(externalId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inactive")
    @PreAuthorize("hasAuthority('WINE_TYPE_LIST_INACTIVE')")
    @Operation(summary = "Listar categorias inactivas", description = "Retorna todas las categorias inactivas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categorias inactivas encontradas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = WineTypeResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<WineTypeResponseDTO>> getInactive() {
        return ResponseEntity.ok(wineTypeService.getInactive());
    }
}
