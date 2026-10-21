package com.utn.vinoteca.features.productVariant.controller;

import com.utn.vinoteca.features.productVariant.dto.ProductVariantRequestDTO;
import com.utn.vinoteca.features.productVariant.dto.ProductVariantResponseDTO;
import com.utn.vinoteca.features.productVariant.service.IProductVariantService;
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
@RequestMapping("/api/product-variants")
@Tag(name = "Product Variants", description = "Endpoints para la gestion de variantes de producto y stock")
public class ProductVariantController {
    private final IProductVariantService productVariantService;

    public ProductVariantController(IProductVariantService productVariantService) {
        this.productVariantService = productVariantService;
    }

    @GetMapping("/{externalId}")
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_READ')")
    @Operation(summary = "Obtener variante por ID", description = "Retorna una variante de producto por su externalId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Variante encontrada",
                    content = @Content(schema = @Schema(implementation = ProductVariantResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Variante no encontrada")
    })
    public ResponseEntity<ProductVariantResponseDTO> getById(
            @Parameter(description = "ID externo de la variante") @PathVariable UUID externalId)
    {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(productVariantService.getById(externalId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_CREATE')")
    @Operation(summary = "Crear variante", description = "Crea una nueva variante de producto")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Variante creada exitosamente",
                    content = @Content(schema = @Schema(implementation = ProductVariantResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<ProductVariantResponseDTO> create(
            @Valid
            @RequestBody ProductVariantRequestDTO productVariantRequestDTO)
    {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productVariantService.create(productVariantRequestDTO));
    }

    @PutMapping("/{externalId}")
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_UPDATE')")
    @Operation(summary = "Actualizar variante", description = "Actualiza los datos de una variante existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Variante actualizada",
                    content = @Content(schema = @Schema(implementation = ProductVariantResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Variante no encontrada")
    })
    public ResponseEntity<ProductVariantResponseDTO> update(
            @Parameter(description = "ID externo de la variante") @PathVariable UUID externalId,
            @RequestBody ProductVariantRequestDTO productVariantRequestDTO)
    {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(productVariantService.update(
                        externalId,
                        productVariantRequestDTO
                ));
    }

    @DeleteMapping("/{externalId}")
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_DELETE')")
    @Operation(summary = "Eliminar variante", description = "Eliminacion logica de una variante de producto")
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
        productVariantService.delete(externalId);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @PatchMapping("/{externalId}/activate")
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_ACTIVATE')")
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
        productVariantService.activate(externalId);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_LIST')")
    @Operation(summary = "Buscar variantes activas", description = "Busca variantes activas con filtros opcionales")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Variantes encontradas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProductVariantResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<ProductVariantResponseDTO>> search(
            @Parameter(description = "Atributo de la variante") @RequestParam(required = false) String attribute,
            @Parameter(description = "Precio minimo") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Precio maximo") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Stock minimo") @RequestParam(required = false) Integer minStock,
            @Parameter(description = "ID del producto") @RequestParam(required = false) UUID productId,
            @Parameter(description = "ID de la categoria") @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "ID de la marca") @RequestParam(required = false) UUID brandId,
            @Parameter(description = "Nombre del producto") @RequestParam(required = false) String productName)
    {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(productVariantService.search(
                        attribute,
                        minPrice,
                        maxPrice,
                        minStock,
                        productId,
                        categoryId,
                        brandId,
                        productName
                ));
    }

    @GetMapping("/inactive")
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_LIST_INACTIVE')")
    @Operation(summary = "Buscar variantes inactivas", description = "Busca variantes inactivas con filtros opcionales")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Variantes inactivas encontradas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProductVariantResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<ProductVariantResponseDTO>> getInactive(
            @Parameter(description = "Atributo de la variante") @RequestParam(required = false) String attribute,
            @Parameter(description = "Precio minimo") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Precio maximo") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Stock minimo") @RequestParam(required = false) Integer minStock,
            @Parameter(description = "ID del producto") @RequestParam(required = false) UUID productId,
            @Parameter(description = "ID de la categoria") @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "ID de la marca") @RequestParam(required = false) UUID brandId,
            @Parameter(description = "Nombre del producto") @RequestParam(required = false) String productName)
    {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(productVariantService.getInactive(
                        attribute,
                        minPrice,
                        maxPrice,
                        minStock,
                        productId,
                        categoryId,
                        brandId,
                        productName
                ));
    }

    @PostMapping("/stock/entry")
    @PreAuthorize("hasAuthority('STOCK_ENTRY')")
    @Operation(summary = "Registrar entrada de stock", description = "Registra una entrada de stock para una variante")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrada de stock registrada",
                    content = @Content(schema = @Schema(implementation = ProductVariantResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<ProductVariantResponseDTO> registerEntry(
            @RequestBody StockMovementRequestDTO dto)
    {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(productVariantService.registerEntry(dto));
    }

    @PostMapping("/stock/adjustment")
    @PreAuthorize("hasAuthority('STOCK_ADJUSTMENT')")
    @Operation(summary = "Ajustar stock", description = "Realiza un ajuste de stock para una variante")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock ajustado",
                    content = @Content(schema = @Schema(implementation = ProductVariantResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<ProductVariantResponseDTO> adjustStock(
            @RequestBody StockMovementRequestDTO dto)
    {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(productVariantService.adjustStock(dto));
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
                .body(productVariantService.getAvailableStock(externalId));
    }

}
