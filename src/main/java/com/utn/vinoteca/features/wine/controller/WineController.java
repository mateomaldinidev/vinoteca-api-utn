package com.utn.vinoteca.features.wine.controller;


import com.utn.vinoteca.features.wine.dto.WineDeleteResponseDTO;
import com.utn.vinoteca.features.wine.dto.WineRequestDTO;
import com.utn.vinoteca.features.wine.dto.WineResponseDTO;
import com.utn.vinoteca.features.wine.service.IWineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
@AllArgsConstructor

@RestController
@RequestMapping("/api/wines")
@Tag(name = "Wines", description = "Endpoints para la gestion de vinos")
public class WineController {
    private final IWineService wineService;

    @PreAuthorize("hasAuthority('WINE_READ')")
    @GetMapping("/{externalId}")
    @Operation(summary = "Obtener vino por ID", description = "Retorna un vino activo por su externalId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vino encontrado",
                    content = @Content(schema = @Schema(implementation = WineResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Vino no encontrado")
    })
    public ResponseEntity<WineResponseDTO> getById(
            @Parameter(description = "ID externo del vino") @PathVariable UUID externalId) {
        return  ResponseEntity
                .status(HttpStatus.OK)
                .body(wineService.getById(externalId));
    }

    @PreAuthorize("hasAuthority('WINE_CREATE')")
    @PostMapping
    @Operation(summary = "Crear vino", description = "Crea un nuevo vino en el sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vino creado exitosamente",
                    content = @Content(schema = @Schema(implementation = WineResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<WineResponseDTO> create(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos del vino a crear")
            @RequestBody WineRequestDTO wineRequestDTO) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(wineService.create(wineRequestDTO));
    }

    @PreAuthorize("hasAuthority('WINE_UPDATE')")
    @PutMapping("/{externalId}")
    @Operation(summary = "Actualizar vino", description = "Actualiza los datos de un vino existente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vino actualizado",
                    content = @Content(schema = @Schema(implementation = WineResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Vino no encontrado")
    })
    public ResponseEntity<WineResponseDTO> update(
            @Parameter(description = "ID externo del vino") @PathVariable UUID externalId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos del vino a actualizar")
            @RequestBody WineRequestDTO wineRequestDTO) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(wineService.update(externalId, wineRequestDTO)
        );
    }

    @PreAuthorize("hasAuthority('WINE_DELETE')")
    @DeleteMapping("/{externalId}")
    @Operation(summary = "Eliminar vino", description = "Eliminacion logica de un vino")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vino eliminado",
                    content = @Content(schema = @Schema(implementation = WineDeleteResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Vino no encontrado")
    })
    public ResponseEntity<WineDeleteResponseDTO> delete(
            @Parameter(description = "ID externo del vino") @PathVariable UUID externalId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(wineService.delete(externalId));
    }

    @PreAuthorize("hasAuthority('WINE_ACTIVATE')")
    @PatchMapping("/{externalId}/activate")
    @Operation(summary = "Activar vino", description = "Activa un vino previamente desactivado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vino activado"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Vino no encontrado")
    })
    public ResponseEntity<Void> activate(
            @Parameter(description = "ID externo del vino") @PathVariable UUID externalId) {
        wineService.activate(externalId);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @PreAuthorize("hasAuthority('WINE_LIST')")
    @GetMapping("/search")
    @Operation(summary = "Buscar vinos activos", description = "Busca vinos activos con filtros opcionales")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vinos encontrados",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = WineResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<List<WineResponseDTO>> search(
            @Parameter(description = "Nombre del vino") @RequestParam(required = false) String name,
            @Parameter(description = "ID del proveedor") @RequestParam(required = false) UUID distributorId,
            @Parameter(description = "ID de la categoria") @RequestParam(required = false) UUID wineTypeId,
            @Parameter(description = "ID de la marca") @RequestParam(required = false) UUID wineryId
    ) {
        return ResponseEntity
                .status(200)
                .body(wineService.search(name, distributorId, wineTypeId, wineryId));
    }

    @PreAuthorize("hasAuthority('WINE_LIST_INACTIVE')")
    @GetMapping("/inactive")
    @Operation(summary = "Buscar vinos inactivos", description = "Busca vinos inactivos con filtros opcionales")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vinos inactivos encontrados",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = WineResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<List<WineResponseDTO>> getInactive(
            @Parameter(description = "Nombre del vino") @RequestParam(required = false) String name,
            @Parameter(description = "ID del proveedor") @RequestParam(required = false) UUID distributorId,
            @Parameter(description = "ID de la categoria") @RequestParam(required = false) UUID wineTypeId,
            @Parameter(description = "ID de la marca") @RequestParam(required = false) UUID wineryId) {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(wineService.getInactive(
                                name,
                                distributorId,
                                wineTypeId,
                                wineryId));
    }

}
