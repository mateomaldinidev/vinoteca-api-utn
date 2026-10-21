package com.utn.vinoteca.features.grapeVariety.controller;

import com.utn.vinoteca.features.grapeVariety.dto.GrapeVarietyRequestDTO;
import com.utn.vinoteca.features.grapeVariety.dto.GrapeVarietyResponseDTO;
import com.utn.vinoteca.features.grapeVariety.service.IGrapeVarietyService;
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
@RequestMapping("/api/grape-varieties")
@RequiredArgsConstructor
@Tag(name = "Grape Varieties", description = "Endpoints para la gestion de varietales")
public class GrapeVarietyController {

    private final IGrapeVarietyService grapeVarietyService;

    @PostMapping
    @PreAuthorize("hasAuthority('GRAPE_VARIETY_CREATE')")
    @Operation(summary = "Crear varietal", description = "Crea un nuevo varietal")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Varietal creado exitosamente",
                    content = @Content(schema = @Schema(implementation = GrapeVarietyResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "El varietal ya existe",
                    content = @Content)
    })
    public ResponseEntity<GrapeVarietyResponseDTO> create(@RequestBody @Valid GrapeVarietyRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(grapeVarietyService.create(dto));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GRAPE_VARIETY_LIST')")
    @Operation(summary = "Listar varietales activos", description = "Retorna todos los varietales activos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Varietales encontrados",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GrapeVarietyResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<GrapeVarietyResponseDTO>> getAll() {
        return ResponseEntity.ok(grapeVarietyService.getAll());
    }

    @GetMapping("/{externalId}")
    @PreAuthorize("hasAuthority('GRAPE_VARIETY_READ')")
    @Operation(summary = "Obtener varietal por ID", description = "Retorna un varietal activo por su externalId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Varietal encontrado",
                    content = @Content(schema = @Schema(implementation = GrapeVarietyResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Varietal no encontrado")
    })
    public ResponseEntity<GrapeVarietyResponseDTO> getByExternalId(
            @Parameter(description = "ID externo del varietal") @PathVariable UUID externalId) {
        return ResponseEntity.ok(grapeVarietyService.getByExternalId(externalId));
    }

    @PutMapping("/{externalId}")
    @PreAuthorize("hasAuthority('GRAPE_VARIETY_UPDATE')")
    @Operation(summary = "Actualizar varietal", description = "Actualiza los datos de un varietal existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Varietal actualizado",
                    content = @Content(schema = @Schema(implementation = GrapeVarietyResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Varietal no encontrado"),
            @ApiResponse(responseCode = "409", description = "El varietal ya existe",
                    content = @Content)
    })
    public ResponseEntity<GrapeVarietyResponseDTO> update(
            @Parameter(description = "ID externo del varietal") @PathVariable UUID externalId,
            @RequestBody @Valid GrapeVarietyRequestDTO dto) {
        return ResponseEntity.ok(grapeVarietyService.update(externalId, dto));
    }

    @DeleteMapping("/{externalId}")
    @PreAuthorize("hasAuthority('GRAPE_VARIETY_DELETE')")
    @Operation(summary = "Eliminar varietal", description = "Eliminacion logica de un varietal")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Varietal eliminado"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Varietal no encontrado")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID externo del varietal") @PathVariable UUID externalId) {
        grapeVarietyService.delete(externalId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{externalId}/activate")
    @PreAuthorize("hasAuthority('GRAPE_VARIETY_ACTIVATE')")
    @Operation(summary = "Activar varietal", description = "Activa un varietal previamente desactivado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Varietal activado"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Varietal no encontrado")
    })
    public ResponseEntity<Void> activate(
            @Parameter(description = "ID externo del varietal") @PathVariable UUID externalId) {
        grapeVarietyService.activate(externalId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inactive")
    @PreAuthorize("hasAuthority('GRAPE_VARIETY_LIST_INACTIVE')")
    @Operation(summary = "Listar varietales inactivos", description = "Retorna todos los varietales inactivos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Varietales inactivos encontrados",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GrapeVarietyResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<GrapeVarietyResponseDTO>> getInactive() {
        return ResponseEntity.ok(grapeVarietyService.getInactive());
    }
}
