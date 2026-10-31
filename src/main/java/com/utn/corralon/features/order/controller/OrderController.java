package com.utn.corralon.features.order.controller;

import com.utn.corralon.features.order.dto.OrderAdminResponseDTO;
import com.utn.corralon.features.order.dto.OrderResponseDTO;
import com.utn.corralon.features.order.dto.OrderSummaryDTO;
import com.utn.corralon.features.order.service.IOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Endpoints para la gestion de ordenes")
public class OrderController {

    private final IOrderService orderService;

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ORDER_READ_ALL')")
    @Operation(summary = "Listar todas las ordenes (Admin)", description = "Retorna todas las ordenes del sistema. Solo admin y empleado.")
    public ResponseEntity<List<OrderAdminResponseDTO>> getAll() {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(orderService.getAll());
    }

    @GetMapping("/{externalId}")
    @PreAuthorize("hasAnyAuthority('ORDER_READ', 'ORDER_READ_ALL')")
    @Operation(summary = "Obtener orden por ID", description = "Retorna una orden por su externalId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orden encontrada"),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    })
    public ResponseEntity<OrderResponseDTO> getByExternalId(
            @PathVariable UUID externalId,
            Authentication authentication) {

        return ResponseEntity.ok(
                orderService.getByExternalId(externalId, authentication.getName())
        );
    }

    @PostMapping("/{externalId}/cancel")
    @PreAuthorize("hasAnyAuthority('ORDER_CANCEL_OWN', 'ORDER_CANCEL_ANY')")
    @Operation(summary = "Cancelar orden", description = "Cancela una orden existente")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Orden cancelada"),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    })
    public ResponseEntity<Void> cancelOrder(
            @PathVariable UUID externalId,
            Authentication authentication
    ) {
        orderService.cancelOrder(externalId, authentication.getName());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user")
    @PreAuthorize("hasAuthority('ORDER_READ_BY_USER')")
    @Operation(summary = "Obtener ordenes de un usuario", description = "Retorna las órdenes del usuario que inició sesión")
    public ResponseEntity<List<OrderSummaryDTO>> getOrdersByUser(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                orderService.getOrdersByUser(authentication.getName())
        );
    }

    @GetMapping("/admin/{externalId}")
    @PreAuthorize("hasAuthority('ORDER_READ_ALL')")
    @Operation(summary = "Obtener orden detalle (Admin)", description = "Retorna el detalle completo de una orden. Solo admin y empleado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orden encontrada"),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    })
    public ResponseEntity<OrderAdminResponseDTO> getAdminOrder(
            @Parameter(description = "ID externo de la orden") @PathVariable UUID externalId) {

        return ResponseEntity.ok(orderService.getAdminOrder(externalId));
    }
}
