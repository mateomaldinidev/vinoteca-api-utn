package com.utn.corralon.features.product.controller;


import com.utn.corralon.features.product.dto.ProductDeleteResponseDTO;
import com.utn.corralon.features.product.dto.ProductRequestDTO;
import com.utn.corralon.features.product.dto.ProductResponseDTO;
import com.utn.corralon.features.product.service.IProductService;
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
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Endpoints para la gestion de productos")
public class ProductController {
    private final IProductService productService;

    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    @GetMapping("/{externalId}")
    @Operation(summary = "Obtener producto por ID", description = "Retorna un producto activo por su externalId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado",
                    content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    public ResponseEntity<ProductResponseDTO> getById(
            @Parameter(description = "ID externo del producto") @PathVariable UUID externalId) {
        return  ResponseEntity
                .status(HttpStatus.OK)
                .body(productService.getById(externalId));
    }

    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    @PostMapping
    @Operation(summary = "Crear producto", description = "Crea un nuevo producto en el sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto creado exitosamente",
                    content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<ProductResponseDTO> create(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos del producto a crear")
            @RequestBody ProductRequestDTO productRequestDTO) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productService.create(productRequestDTO));
    }

    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    @PutMapping("/{externalId}")
    @Operation(summary = "Actualizar producto", description = "Actualiza los datos de un producto existente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto actualizado",
                    content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    public ResponseEntity<ProductResponseDTO> update(
            @Parameter(description = "ID externo del producto") @PathVariable UUID externalId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos del producto a actualizar")
            @RequestBody ProductRequestDTO productRequestDTO) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productService.update(externalId, productRequestDTO)
        );
    }

    @PreAuthorize("hasAuthority('PRODUCT_DELETE')")
    @DeleteMapping("/{externalId}")
    @Operation(summary = "Eliminar producto", description = "Eliminacion logica de un producto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto eliminado",
                    content = @Content(schema = @Schema(implementation = ProductDeleteResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    public ResponseEntity<ProductDeleteResponseDTO> delete(
            @Parameter(description = "ID externo del producto") @PathVariable UUID externalId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(productService.delete(externalId));
    }

    @PreAuthorize("hasAuthority('PRODUCT_ACTIVATE')")
    @PatchMapping("/{externalId}/activate")
    @Operation(summary = "Activar producto", description = "Activa un producto previamente desactivado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Producto activado"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    public ResponseEntity<Void> activate(
            @Parameter(description = "ID externo del producto") @PathVariable UUID externalId) {
        productService.activate(externalId);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @PreAuthorize("hasAuthority('PRODUCT_LIST')")
    @GetMapping("/search")
    @Operation(summary = "Buscar productos activos", description = "Busca productos activos con filtros opcionales")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Productos encontrados",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProductResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<List<ProductResponseDTO>> search(
            @Parameter(description = "Nombre del producto") @RequestParam(required = false) String name,
            @Parameter(description = "ID del proveedor") @RequestParam(required = false) UUID supplierId,
            @Parameter(description = "ID de la categoria") @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "ID de la marca") @RequestParam(required = false) UUID brandId
    ) {
        return ResponseEntity
                .status(200)
                .body(productService.search(name, supplierId, categoryId, brandId));
    }

    @PreAuthorize("hasAuthority('PRODUCT_LIST_INACTIVE')")
    @GetMapping("/inactive")
    @Operation(summary = "Buscar productos inactivos", description = "Busca productos inactivos con filtros opcionales")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Productos inactivos encontrados",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProductResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<List<ProductResponseDTO>> getInactive(
            @Parameter(description = "Nombre del producto") @RequestParam(required = false) String name,
            @Parameter(description = "ID del proveedor") @RequestParam(required = false) UUID supplierId,
            @Parameter(description = "ID de la categoria") @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "ID de la marca") @RequestParam(required = false) UUID brandId) {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(productService.getInactive(
                                name,
                                supplierId,
                                categoryId,
                                brandId));
    }

}
