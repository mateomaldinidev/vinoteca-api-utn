package com.utn.corralon.features.stockMovement.controller;

import com.utn.corralon.features.stockMovement.dto.StockMovementResponseDTO;
import com.utn.corralon.features.stockMovement.service.IStockMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stock-movements")
@RequiredArgsConstructor
@Tag(name = "Stock Movements", description = "Endpoints para consultar movimientos de stock")
public class StockMovementController {

    private final IStockMovementService stockMovementService;

    @GetMapping("/variant/{variantId}")
    @PreAuthorize("hasAuthority('STOCK_MOVEMENT_LIST_BY_VARIANT')")
    @Operation(summary = "Obtener movimientos por variante", description = "Retorna todos los movimientos de stock de una variante de producto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movimientos encontrados",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = StockMovementResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<StockMovementResponseDTO>> getMovementsByVariant(
            @Parameter(description = "ID externo de la variante de producto") @PathVariable UUID variantId) {
        return ResponseEntity.ok(stockMovementService.getMovementsByVariant(variantId));
    }

    @GetMapping("/{externalId}")
    @PreAuthorize("hasAuthority('STOCK_MOVEMENT_READ')")
    @Operation(summary = "Obtener movimiento por ID", description = "Retorna un movimiento de stock por su externalId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movimiento encontrado",
                    content = @Content(schema = @Schema(implementation = StockMovementResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Movimiento no encontrado")
    })
    public ResponseEntity<StockMovementResponseDTO> getMovementById(
            @Parameter(description = "ID externo del movimiento de stock") @PathVariable UUID externalId) {
        return ResponseEntity.ok(stockMovementService.getMovementById(externalId));
    }
}
