package com.utn.corralon.features.productImage.controller;

import com.utn.corralon.features.productImage.dto.ProductImageResponseDTO;
import com.utn.corralon.features.productImage.service.IProductImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/api/products/{productExternalId}/images")
@Tag(name = "Product images", description = "Endpoints para la gestion de imagenes de productos")
public class ProductImageController {

    private final IProductImageService productImageService;

    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir imagenes", description = "Sube una o varias imagenes a un producto (maximo 5 en total)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Imagenes subidas correctamente"),
            @ApiResponse(responseCode = "400", description = "Archivo invalido o limite de imagenes excedido"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    public ResponseEntity<List<ProductImageResponseDTO>> upload(
            @Parameter(description = "ID externo del producto") @PathVariable UUID productExternalId,
            @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productImageService.upload(productExternalId, files));
    }

    @PreAuthorize("hasAnyAuthority('PRODUCT_READ', 'PRODUCT_UPDATE')")
    @GetMapping
    @Operation(summary = "Listar imagenes", description = "Retorna las imagenes de un producto")
    @ApiResponse(responseCode = "200", description = "Imagenes del producto")
    public ResponseEntity<List<ProductImageResponseDTO>> getByProduct(
            @Parameter(description = "ID externo del producto") @PathVariable UUID productExternalId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(productImageService.getByProduct(productExternalId));
    }

    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    @DeleteMapping("/{imageExternalId}")
    @Operation(summary = "Eliminar imagen", description = "Elimina una imagen del producto. No se permite eliminar la ultima")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Imagen eliminada"),
            @ApiResponse(responseCode = "404", description = "Producto o imagen no encontrados"),
            @ApiResponse(responseCode = "409", description = "El producto quedaria sin imagenes")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID externo del producto") @PathVariable UUID productExternalId,
            @Parameter(description = "ID externo de la imagen") @PathVariable UUID imageExternalId) {
        productImageService.delete(productExternalId, imageExternalId);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    @PatchMapping("/{imageExternalId}/principal")
    @Operation(summary = "Establecer imagen principal", description = "Marca una imagen como principal del producto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagen principal actualizada"),
            @ApiResponse(responseCode = "404", description = "Producto o imagen no encontrados")
    })
    public ResponseEntity<ProductImageResponseDTO> setPrincipal(
            @Parameter(description = "ID externo del producto") @PathVariable UUID productExternalId,
            @Parameter(description = "ID externo de la imagen") @PathVariable UUID imageExternalId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(productImageService.setPrincipal(productExternalId, imageExternalId));
    }
}
