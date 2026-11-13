package com.utn.corralon.features.cart.controller;

import com.utn.corralon.features.cart.dto.CartRequestDTO;
import com.utn.corralon.features.cart.dto.CartResponseDTO;
import com.utn.corralon.features.cart.dto.CheckoutResultDTO;
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
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<CartResponseDTO> createOrUpdateCart(
            @Valid @RequestBody CartRequestDTO cartRequest,
            Authentication authentication) {

        return ResponseEntity.ok(
                cartService.createOrUpdateCart(authentication.getName(), cartRequest)
        );
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CART_READ')")
    @Operation(summary = "Obtener carrito del usuario autenticado", description = "Retorna el carrito del usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carrito encontrado"),
            @ApiResponse(responseCode = "404", description = "Carrito no encontrado")
    })
    public ResponseEntity<CartResponseDTO> getCart(Authentication authentication) {

        return ResponseEntity.ok(
                cartService.getCart(authentication.getName())
        );
    }

    @PatchMapping("/items/{productVariantId}")
    @PreAuthorize("hasAuthority('CART_UPDATE_ITEM_QUANTITY')")
    @Operation(summary = "Actualizar cantidad de item", description = "Actualiza la cantidad de un producto en el carrito del usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Cantidad actualizada")
    public ResponseEntity<CartResponseDTO> updateCartItemQuantity(
            @Parameter(description = "ID externo de la variante de producto")
            @PathVariable UUID productVariantId,
            @Valid @RequestBody CartItemQuantityUpdateDTO updateDTO,
            Authentication authentication) {

        return ResponseEntity.ok(
                cartService.updateCartItemQuantity(
                        authentication.getName(),
                        productVariantId,
                        updateDTO
                )
        );
    }

    @DeleteMapping("/items/{productVariantId}")
    @PreAuthorize("hasAuthority('CART_UPDATE_ITEM_QUANTITY')")
    @Operation(summary = "Eliminar item del carrito", description = "Elimina una variante de producto del carrito del usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item eliminado y carrito actualizado"),
            @ApiResponse(responseCode = "404", description = "Carrito o item no encontrado")
    })
    public ResponseEntity<CartResponseDTO> removeCartItem(
            @Parameter(description = "ID externo de la variante de producto")
            @PathVariable UUID productVariantId,
            Authentication authentication) {

        return ResponseEntity.ok(
                cartService.removeCartItem(
                        authentication.getName(),
                        productVariantId
                )
        );
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('CART_CLEAR')")
    @Operation(summary = "Vaciar carrito", description = "Elimina todos los items del carrito del usuario autenticado")
    @ApiResponse(responseCode = "204", description = "Carrito vaciado")
    public ResponseEntity<Void> clearCart(Authentication authentication) {

        cartService.clearCart(authentication.getName());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAuthority('CART_CHECKOUT')")
    @Operation(summary = "Finalizar compra", description = "Convierte el carrito del usuario autenticado en una orden con soporte de idempotencia")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Orden creada exitosamente"),
            @ApiResponse(responseCode = "200", description = "Orden recuperada por reintento idempotente"),
            @ApiResponse(responseCode = "400", description = "Error en el checkout o encabezado invalido"),
            @ApiResponse(responseCode = "409", description = "Conflicto de idempotencia o reglas de negocio")
    })
    public ResponseEntity<OrderResponseDTO> checkout(
            @Parameter(description = "Clave de idempotencia obligatoria para pedidos nuevos")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Parameter(description = "Tipo de entrega")
            @RequestParam DeliveryType deliveryType,
            @Parameter(description = "ID de la direccion de entrega (opcional para retiro en tienda)")
            @RequestParam(required = false) UUID addressId,
            Authentication authentication) {

        CheckoutResultDTO result = cartService.checkout(
                authentication.getName(),
                addressId,
                deliveryType,
                idempotencyKey
        );

        HttpStatus status = result.isReplay() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result.getOrder());
    }
}
