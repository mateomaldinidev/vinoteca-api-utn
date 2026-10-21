package com.utn.vinoteca.features.distributor.controller;

import com.utn.vinoteca.features.distributor.dto.DistributorRequestDTO;
import com.utn.vinoteca.features.distributor.dto.DistributorResponseDTO;
import com.utn.vinoteca.features.distributor.service.IDistributorService;
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
@RequestMapping("/api/distributors")
@RequiredArgsConstructor
@Tag(name = "Distributors", description = "Endpoints para la gestion de distribuidores")
public class DistributorController {

    private final IDistributorService distributorService;

    @PostMapping
    @PreAuthorize("hasAuthority('DISTRIBUTOR_CREATE')")
    @Operation(summary = "Crear distribuidor", description = "Crea un nuevo distribuidor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Distribuidor creado exitosamente",
                    content = @Content(schema = @Schema(implementation = DistributorResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<DistributorResponseDTO> create(@RequestBody @Valid DistributorRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(distributorService.create(dto));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DISTRIBUTOR_LIST')")
    @Operation(summary = "Listar distribuidores activos", description = "Retorna todos los distribuidores activos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distribuidores encontrados",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DistributorResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<DistributorResponseDTO>> getAll() {
        return ResponseEntity.ok(distributorService.getAll());
    }

    @GetMapping("/{externalId}")
    @PreAuthorize("hasAuthority('DISTRIBUTOR_READ')")
    @Operation(summary = "Obtener distribuidor por ID", description = "Retorna un distribuidor activo por su externalId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distribuidor encontrado",
                    content = @Content(schema = @Schema(implementation = DistributorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Distribuidor no encontrado")
    })
    public ResponseEntity<DistributorResponseDTO> getByExternalId(
            @Parameter(description = "ID externo del distribuidor") @PathVariable UUID externalId) {
        return ResponseEntity.ok(distributorService.getByExternalId(externalId));
    }

    @PutMapping("/{externalId}")
    @PreAuthorize("hasAuthority('DISTRIBUTOR_UPDATE')")
    @Operation(summary = "Actualizar distribuidor", description = "Actualiza los datos de un distribuidor existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distribuidor actualizado",
                    content = @Content(schema = @Schema(implementation = DistributorResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Distribuidor no encontrado")
    })
    public ResponseEntity<DistributorResponseDTO> update(
            @Parameter(description = "ID externo del distribuidor") @PathVariable UUID externalId,
            @RequestBody @Valid DistributorRequestDTO dto) {
        return ResponseEntity.ok(distributorService.update(externalId, dto));
    }

    @DeleteMapping("/{externalId}")
    @PreAuthorize("hasAuthority('DISTRIBUTOR_DELETE')")
    @Operation(summary = "Eliminar distribuidor", description = "Eliminacion logica de un distribuidor")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Distribuidor eliminado"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Distribuidor no encontrado")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID externo del distribuidor") @PathVariable UUID externalId) {
        distributorService.delete(externalId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{externalId}/activate")
    @PreAuthorize("hasAuthority('DISTRIBUTOR_ACTIVATE')")
    @Operation(summary = "Activar distribuidor", description = "Activa un distribuidor previamente desactivado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Distribuidor activado"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Distribuidor no encontrado")
    })
    public ResponseEntity<Void> activate(
            @Parameter(description = "ID externo del distribuidor") @PathVariable UUID externalId) {
        distributorService.activate(externalId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inactive")
    @PreAuthorize("hasAuthority('DISTRIBUTOR_LIST_INACTIVE')")
    @Operation(summary = "Listar distribuidores inactivos", description = "Retorna todos los distribuidores inactivos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distribuidores inactivos encontrados",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DistributorResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<DistributorResponseDTO>> getInactive() {
        return ResponseEntity.ok(distributorService.getInactive());
    }
}
