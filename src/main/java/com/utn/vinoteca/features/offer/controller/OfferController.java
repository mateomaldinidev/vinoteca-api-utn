package com.utn.vinoteca.features.offer.controller;

import com.utn.vinoteca.features.offer.dto.OfferRequestDTO;
import com.utn.vinoteca.features.offer.dto.OfferResponseDTO;
import com.utn.vinoteca.features.offer.service.IOfferService;
import com.utn.vinoteca.features.offer_product.dto.OfferProductRequestDTO;
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
@RequestMapping("/api/offers")
@RequiredArgsConstructor
@Tag(name = "Offers", description = "Endpoints para la gestion de ofertas")
public class OfferController {

    private final IOfferService offerService;

    @PostMapping
    @PreAuthorize("hasAuthority('OFFER_CREATE')")
    @Operation(summary = "Crear oferta", description = "Crea una nueva oferta")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Oferta creada exitosamente",
                    content = @Content(schema = @Schema(implementation = OfferResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<OfferResponseDTO> create(@RequestBody @Valid OfferRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(offerService.create(dto));
    }

    @PostMapping("/{offerExternalId}/wines")
    @PreAuthorize("hasAuthority('OFFER_ADD_PRODUCT')")
    @Operation(summary = "Agregar vino a oferta", description = "Agrega un vino a una oferta existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vino agregado",
                    content = @Content(schema = @Schema(implementation = OfferResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Oferta no encontrada")
    })
    public ResponseEntity<OfferResponseDTO> addProduct(
            @Parameter(description = "ID externo de la oferta") @PathVariable UUID offerExternalId,
            @RequestBody @Valid OfferProductRequestDTO dto) {
        return ResponseEntity.ok(offerService.addProductToOffer(dto));
    }

    @DeleteMapping("/{offerExternalId}/wines/{wineVariantExternalId}")
    @PreAuthorize("hasAuthority('OFFER_REMOVE_PRODUCT')")
    @Operation(summary = "Remover vino de oferta", description = "Elimina un vino de una oferta existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vino removido",
                    content = @Content(schema = @Schema(implementation = OfferResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Oferta o vino no encontrado")
    })
    public ResponseEntity<OfferResponseDTO> removeProduct(
            @Parameter(description = "ID externo de la oferta") @PathVariable UUID offerExternalId,
            @Parameter(description = "ID externo de la variante de vino") @PathVariable UUID wineVariantExternalId) {
        return ResponseEntity.ok(offerService.removeProductFromOffer(offerExternalId, wineVariantExternalId));
    }

    @GetMapping("/{externalId}")
    @PreAuthorize("hasAuthority('OFFER_READ')")
    @Operation(summary = "Obtener oferta por ID", description = "Retorna una oferta por su externalId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Oferta encontrada",
                    content = @Content(schema = @Schema(implementation = OfferResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Oferta no encontrada")
    })
    public ResponseEntity<OfferResponseDTO> getByExternalId(
            @Parameter(description = "ID externo de la oferta") @PathVariable UUID externalId) {
        return ResponseEntity.ok(offerService.getByExternalId(externalId));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('OFFER_LIST')")
    @Operation(summary = "Listar ofertas activas", description = "Retorna todas las ofertas activas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ofertas encontradas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OfferResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<OfferResponseDTO>> getAllActive() {
        return ResponseEntity.ok(offerService.getAllActive());
    }

    @GetMapping("/inactive")
    @PreAuthorize("hasAuthority('OFFER_LIST_INACTIVE')")
    @Operation(summary = "Listar ofertas inactivas", description = "Retorna todas las ofertas inactivas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ofertas inactivas encontradas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OfferResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<OfferResponseDTO>> getAllInactive() {
        return ResponseEntity.ok(offerService.getAllInactive());
    }

    @PatchMapping("/{externalId}/activate")
    @PreAuthorize("hasAuthority('OFFER_ACTIVATE')")
    @Operation(summary = "Activar oferta", description = "Activa una oferta previamente desactivada")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Oferta activada"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Oferta no encontrada")
    })
    public ResponseEntity<Void> activate(
            @Parameter(description = "ID externo de la oferta") @PathVariable UUID externalId) {
        offerService.activate(externalId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{externalId}/deactivate")
    @PreAuthorize("hasAuthority('OFFER_DEACTIVATE')")
    @Operation(summary = "Desactivar oferta", description = "Desactiva una oferta activa")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Oferta desactivada"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Oferta no encontrada")
    })
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "ID externo de la oferta") @PathVariable UUID externalId) {
        offerService.deactivate(externalId);
        return ResponseEntity.noContent().build();
    }
}
