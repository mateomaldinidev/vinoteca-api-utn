package com.utn.corralon.features.cart.controller;

import com.utn.corralon.features.cart.dto.CartRequestDTO;
import com.utn.corralon.features.cart.dto.CartResponseDTO;
import com.utn.corralon.features.cart.service.ICartService;
import com.utn.corralon.features.cart_item.dto.CartItemQuantityUpdateDTO;
import com.utn.corralon.features.order.dto.OrderResponseDTO;
import com.utn.corralon.features.order.enums.DeliveryType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @ApiResponse(responseCode = "200", description = "Carrito creado o actualizado")
    public ResponseEntity<CartResponseDTO> createOrUpdateCart(@Valid @RequestBody CartRequestDTO cartRequest) {

        return ResponseEntity.ok( cartService.createOrUpdateCart(cartRequest));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('CART_READ')")
    @Operation(summary = "Obtener carrito de usuario", description = "Retorna el carrito de un usuario especifico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carrito encontrado"),
            @ApiResponse(responseCode = "404", description = "Carrito no encontrado")
    })
    public ResponseEntity<CartResponseDTO> getCartByUserId(
            @Parameter(description = "ID externo del usuario") @PathVariable UUID userId) {
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    @PatchMapping("/{userId}/items/{productVariantId}")
    @PreAuthorize("hasAuthority('CART_UPDATE_ITEM_QUANTITY')")
    @Operation(summary = "Actualizar cantidad de item", description = "Actualiza la cantidad de un producto en el carrito")
    @ApiResponse(responseCode = "200", description = "Cantidad actualizada")
    public ResponseEntity<CartResponseDTO> updateCartItemQuantity(
            @Parameter(description = "ID externo del usuario") @PathVariable UUID userId,
            @Parameter(description = "ID externo de la variante de producto") @PathVariable UUID productVariantId,
            @Valid @RequestBody CartItemQuantityUpdateDTO updateDTO) {
        return ResponseEntity.ok(cartService.updateCartItemQuantity(userId, productVariantId, updateDTO));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('CART_CLEAR')")
    @Operation(summary = "Vaciar carrito", description = "Elimina todos los items dvel carrito de un usuario")
    @ApiResponse(responseCode = "204", description = "Carrito vaciado")
    public ResponseEntity<Void> clearCart(
            @Parameter(description = "ID externo del usuario") @PathVariable UUID userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/checkout")
    @PreAuthorize("hasAuthority('CART_CHECKOUT')")
    @Operation(summary = "Finalizar compra", description = "Convierte el carrito en una orden")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Orden creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error en el checkout")
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
