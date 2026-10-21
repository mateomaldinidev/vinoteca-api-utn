package com.utn.vinoteca.features.cart.controller;

import com.utn.vinoteca.features.cart.dto.CartRequestDTO;
import com.utn.vinoteca.features.cart.dto.CartResponseDTO;
import com.utn.vinoteca.features.cart.service.ICartService;
import com.utn.vinoteca.features.cart_item.dto.CartItemQuantityUpdateDTO;
import com.utn.vinoteca.features.order.dto.OrderResponseDTO;
import com.utn.vinoteca.features.order.enums.DeliveryType;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/carts")
@Tag(name = "Cart", description = "Endpoints para la gestion del carrito de compras")
public class CartController {

    private final ICartService cartService;

    public CartController(ICartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CART_CREATE_OR_UPDATE')")
    @Operation(summary = "Crear o actualizar carrito", description = "Crea un nuevo carrito o actualiza uno existente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Carrito creado o actualizado",
                    content = @Content(schema = @Schema(implementation = CartResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<CartResponseDTO> createOrUpdateCart(@Valid @RequestBody CartRequestDTO cartRequest) {

        return ResponseEntity.ok( cartService.createOrUpdateCart(cartRequest));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('CART_READ')")
    @Operation(summary = "Obtener carrito de usuario", description = "Retorna el carrito de un usuario especifico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carrito encontrado",
                    content = @Content(schema = @Schema(implementation = CartResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Carrito no encontrado")
    })
    public ResponseEntity<CartResponseDTO> getCartByUserId(
            @Parameter(description = "ID externo del usuario") @PathVariable UUID userId) {
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    @PatchMapping("/{userId}/items/{wineVariantId}")
    @PreAuthorize("hasAuthority('CART_UPDATE_ITEM_QUANTITY')")
    @Operation(summary = "Actualizar cantidad de item", description = "Actualiza la cantidad de un vino en el carrito")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cantidad actualizada",
                    content = @Content(schema = @Schema(implementation = CartResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Carrito o item no encontrado")
    })
    public ResponseEntity<CartResponseDTO> updateCartItemQuantity(
            @Parameter(description = "ID externo del usuario") @PathVariable UUID userId,
            @Parameter(description = "ID externo del formato de vino") @PathVariable UUID wineVariantId,
            @Valid @RequestBody CartItemQuantityUpdateDTO updateDTO) {
        return ResponseEntity.ok(cartService.updateCartItemQuantity(userId, wineVariantId, updateDTO));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('CART_CLEAR')")
    @Operation(summary = "Vaciar carrito", description = "Elimina todos los items del carrito de un usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Carrito vaciado"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Carrito no encontrado")
    })
    public ResponseEntity<Void> clearCart(
            @Parameter(description = "ID externo del usuario") @PathVariable UUID userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/checkout")
    @PreAuthorize("hasAuthority('CART_CHECKOUT')")
    @Operation(summary = "Finalizar compra", description = "Convierte el carrito en una orden")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Orden creada exitosamente",
                    content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Error en el checkout",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<OrderResponseDTO> checkout(
            @Parameter(description = "ID externo del usuario") @PathVariable UUID userId,
            @Parameter(description = "Tipo de entrega") @RequestParam DeliveryType deliveryType,
            @Parameter(description = "ID de la direccion de entrega (opcional para retiro en tienda)") @RequestParam(required = false) UUID addressId
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cartService.checkout(userId, addressId, deliveryType));
    }
}
