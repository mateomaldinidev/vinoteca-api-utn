package com.utn.vinoteca.features.winery.controller;

import com.utn.vinoteca.features.winery.dto.WineryRequestDTO;
import com.utn.vinoteca.features.winery.dto.WineryResponseDTO;
import com.utn.vinoteca.features.winery.service.IWineryService;
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
@RequestMapping("/api/wineries")
@RequiredArgsConstructor
@Tag(name = "Wineries", description = "Endpoints para la gestion de marcas")
public class WineryController {
    private final IWineryService wineryService;

    @PostMapping
    @PreAuthorize("hasAuthority('WINERY_CREATE')")
    @Operation(summary = "Crear marca", description = "Crea una nueva marca")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Marca creada exitosamente",
                    content = @Content(schema = @Schema(implementation = WineryResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<WineryResponseDTO> create(@RequestBody @Valid WineryRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(wineryService.create(dto));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('WINERY_LIST')")
    @Operation(summary = "Listar marcas activas", description = "Retorna todas las marcas activas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Marcas encontradas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = WineryResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<WineryResponseDTO>> getAll() {
        return ResponseEntity.ok(wineryService.getAll());
    }

    @GetMapping("/{externalId}")
    @PreAuthorize("hasAuthority('WINERY_READ')")
    @Operation(summary = "Obtener marca por ID", description = "Retorna una marca activa por su externalId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Marca encontrada",
                    content = @Content(schema = @Schema(implementation = WineryResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Marca no encontrada")
    })
    public ResponseEntity<WineryResponseDTO> getByExternalId(
            @Parameter(description = "ID externo de la marca") @PathVariable UUID externalId) {
        return ResponseEntity.ok(wineryService.getByExternalId(externalId));
    }

    @PutMapping("/{externalId}")
    @PreAuthorize("hasAuthority('WINERY_UPDATE')")
    @Operation(summary = "Actualizar marca", description = "Actualiza los datos de una marca existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Marca actualizada",
                    content = @Content(schema = @Schema(implementation = WineryResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Marca no encontrada")
    })
    public ResponseEntity<WineryResponseDTO> update(
            @Parameter(description = "ID externo de la marca") @PathVariable UUID externalId,
            @RequestBody @Valid WineryRequestDTO dto) {
        return ResponseEntity.ok(wineryService.update(externalId, dto));
    }

    @DeleteMapping("/{externalId}")
    @PreAuthorize("hasAuthority('WINERY_DELETE')")
    @Operation(summary = "Eliminar marca", description = "Eliminacion logica de una marca")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Marca eliminada"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Marca no encontrada")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID externo de la marca") @PathVariable UUID externalId) {
        wineryService.delete(externalId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{externalId}/activate")
    @PreAuthorize("hasAuthority('WINERY_ACTIVATE')")
    @Operation(summary = "Activar marca", description = "Activa una marca previamente desactivada")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Marca activada"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Marca no encontrada")
    })
    public ResponseEntity<Void> activate(
            @Parameter(description = "ID externo de la marca") @PathVariable UUID externalId) {
        wineryService.activate(externalId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inactive")
    @PreAuthorize("hasAuthority('WINERY_LIST_INACTIVE')")
    @Operation(summary = "Listar marcas inactivas", description = "Retorna todas las marcas inactivas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Marcas inactivas encontradas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = WineryResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<WineryResponseDTO>> getInactive() {
        return ResponseEntity.ok(wineryService.getInactive());
    }
}
