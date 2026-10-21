package com.utn.vinoteca.features.wineVariant.controller;

import com.utn.vinoteca.features.wineVariant.dto.WineVariantRequestDTO;
import com.utn.vinoteca.features.wineVariant.dto.WineVariantResponseDTO;
import com.utn.vinoteca.features.wineVariant.service.IWineVariantService;
import com.utn.vinoteca.features.stockMovement.dto.StockMovementRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/wine-variants")
@Tag(name = "Wine Variants", description = "Endpoints para la gestion de variantes de vino y stock")
public class WineVariantController {
    private final IWineVariantService wineVariantService;

    public WineVariantController(IWineVariantService wineVariantService) {
        this.wineVariantService = wineVariantService;
    }

    @GetMapping("/{externalId}")
    @PreAuthorize("hasAuthority('WINE_VARIANT_READ')")
    @Operation(summary = "Obtener variante por ID", description = "Retorna una variante de vino por su externalId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Variante encontrada",
                    content = @Content(schema = @Schema(implementation = WineVariantResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Variante no encontrada")
    })
    public ResponseEntity<WineVariantResponseDTO> getById(
            @Parameter(description = "ID externo de la variante") @PathVariable UUID externalId)
    {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(wineVariantService.getById(externalId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WINE_VARIANT_CREATE')")
    @Operation(summary = "Crear variante", description = "Crea una nueva variante de vino")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Variante creada exitosamente",
                    content = @Content(schema = @Schema(implementation = WineVariantResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<WineVariantResponseDTO> create(
            @Valid
            @RequestBody WineVariantRequestDTO wineVariantRequestDTO)
    {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(wineVariantService.create(wineVariantRequestDTO));
    }

    @PutMapping("/{externalId}")
    @PreAuthorize("hasAuthority('WINE_VARIANT_UPDATE')")
    @Operation(summary = "Actualizar variante", description = "Actualiza los datos de una variante existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Variante actualizada",
                    content = @Content(schema = @Schema(implementation = WineVariantResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Variante no encontrada")
    })
    public ResponseEntity<WineVariantResponseDTO> update(
            @Parameter(description = "ID externo de la variante") @PathVariable UUID externalId,
            @RequestBody WineVariantRequestDTO wineVariantRequestDTO)
    {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(wineVariantService.update(
                        externalId,
                        wineVariantRequestDTO
                ));
    }

    @DeleteMapping("/{externalId}")
    @PreAuthorize("hasAuthority('WINE_VARIANT_DELETE')")
    @Operation(summary = "Eliminar variante", description = "Eliminacion logica de una variante de vino")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Variante eliminada"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Variante no encontrada")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID externo de la variante") @PathVariable UUID externalId)
    {
        wineVariantService.delete(externalId);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @PatchMapping("/{externalId}/activate")
    @PreAuthorize("hasAuthority('WINE_VARIANT_ACTIVATE')")
    @Operation(summary = "Activar variante", description = "Activa una variante previamente desactivada")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Variante activada"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Variante no encontrada")
    })
    public ResponseEntity<Void> activate(
            @Parameter(description = "ID externo de la variante") @PathVariable UUID externalId)
    {
        wineVariantService.activate(externalId);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('WINE_VARIANT_LIST')")
    @Operation(summary = "Buscar variantes activas", description = "Busca variantes activas con filtros opcionales")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Variantes encontradas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = WineVariantResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<WineVariantResponseDTO>> search(
            @Parameter(description = "Atributo de la variante") @RequestParam(required = false) String attribute,
            @Parameter(description = "Precio minimo") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Precio maximo") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Stock minimo") @RequestParam(required = false) Integer minStock,
            @Parameter(description = "ID del vino") @RequestParam(required = false) UUID wineId,
            @Parameter(description = "ID de la categoria") @RequestParam(required = false) UUID wineTypeId,
            @Parameter(description = "ID de la marca") @RequestParam(required = false) UUID wineryId,
            @Parameter(description = "Nombre del vino") @RequestParam(required = false) String wineName)
    {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(wineVariantService.search(
                        attribute,
                        minPrice,
                        maxPrice,
                        minStock,
                        wineId,
                        wineTypeId,
                        wineryId,
                        wineName
                ));
    }

    @GetMapping("/inactive")
    @PreAuthorize("hasAuthority('WINE_VARIANT_LIST_INACTIVE')")
    @Operation(summary = "Buscar variantes inactivas", description = "Busca variantes inactivas con filtros opcionales")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Variantes inactivas encontradas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = WineVariantResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<WineVariantResponseDTO>> getInactive(
            @Parameter(description = "Atributo de la variante") @RequestParam(required = false) String attribute,
            @Parameter(description = "Precio minimo") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Precio maximo") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Stock minimo") @RequestParam(required = false) Integer minStock,
            @Parameter(description = "ID del vino") @RequestParam(required = false) UUID wineId,
            @Parameter(description = "ID de la categoria") @RequestParam(required = false) UUID wineTypeId,
            @Parameter(description = "ID de la marca") @RequestParam(required = false) UUID wineryId,
            @Parameter(description = "Nombre del vino") @RequestParam(required = false) String wineName)
    {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(wineVariantService.getInactive(
                        attribute,
                        minPrice,
                        maxPrice,
                        minStock,
                        wineId,
                        wineTypeId,
                        wineryId,
                        wineName
                ));
    }

    @PostMapping("/stock/entry")
    @PreAuthorize("hasAuthority('STOCK_ENTRY')")
    @Operation(summary = "Registrar entrada de stock", description = "Registra una entrada de stock para una variante")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrada de stock registrada",
                    content = @Content(schema = @Schema(implementation = WineVariantResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<WineVariantResponseDTO> registerEntry(
            @RequestBody StockMovementRequestDTO dto)
    {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(wineVariantService.registerEntry(dto));
    }

    @PostMapping("/stock/adjustment")
    @PreAuthorize("hasAuthority('STOCK_ADJUSTMENT')")
    @Operation(summary = "Ajustar stock", description = "Realiza un ajuste de stock para una variante")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock ajustado",
                    content = @Content(schema = @Schema(implementation = WineVariantResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<WineVariantResponseDTO> adjustStock(
            @RequestBody StockMovementRequestDTO dto)
    {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(wineVariantService.adjustStock(dto));
    }

    @GetMapping("/{externalId}/stock")
    @PreAuthorize("hasAuthority('STOCK_READ')")
    @Operation(summary = "Obtener stock disponible", description = "Retorna el stock disponible de una variante")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock obtenido",
                    content = @Content(schema = @Schema(implementation = Integer.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Variante no encontrada")
    })
    public ResponseEntity<Integer> getAvailableStock(
            @Parameter(description = "ID externo de la variante") @PathVariable UUID externalId)
    {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(wineVariantService.getAvailableStock(externalId));
    }

}
