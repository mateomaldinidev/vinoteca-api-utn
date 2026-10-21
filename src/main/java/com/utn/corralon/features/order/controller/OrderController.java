package com.utn.corralon.features.order.controller;

import com.utn.corralon.features.order.dto.OrderAdminResponseDTO;
import com.utn.corralon.features.order.dto.OrderResponseDTO;
import com.utn.corralon.features.order.dto.OrderSummaryDTO;
import com.utn.corralon.features.order.service.IOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Listar todas las ordenes (Admin)", description = "Retorna todas las ordenes del sistema. Solo admin y empleado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordenes encontradas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrderAdminResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<OrderAdminResponseDTO>> getAll() {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(orderService.getAll());
    }

    @GetMapping("/{externalId}")
    @PreAuthorize("hasAuthority('ORDER_READ')")
    @Operation(summary = "Obtener orden por ID", description = "Retorna una orden por su externalId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orden encontrada",
                    content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    })
    public ResponseEntity<OrderResponseDTO> getByExternalId(
            @Parameter(description = "ID externo de la orden") @PathVariable UUID externalId) {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(orderService.getByExternalId(externalId));
    }

    @PostMapping("/{externalId}/cancel")
    @PreAuthorize("hasAuthority('ORDER_CANCEL_OWN')")
    @Operation(summary = "Cancelar orden", description = "Cancela una orden existente")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Orden cancelada"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    })
    public ResponseEntity<Void> cancelOrder(
            @Parameter(description = "ID externo de la orden") @PathVariable UUID externalId,
            @Parameter(description = "ID externo del usuario") @RequestParam UUID userExternalId
    ) {

        orderService.cancelOrder(externalId, userExternalId);

        return ResponseEntity
                .noContent()
                .build();
    }

    @GetMapping("/user/{userExternalId}")
    @PreAuthorize("hasAuthority('ORDER_READ_BY_USER')")
    @Operation(summary = "Obtener ordenes de un usuario", description = "Retorna todas las ordenes de un usuario especifico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordenes encontradas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrderSummaryDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<OrderSummaryDTO>> getOrdersByUser(
            @Parameter(description = "ID externo del usuario") @PathVariable UUID userExternalId) {

        return ResponseEntity.ok(orderService.getOrdersByUser(userExternalId));
    }

    @GetMapping("/admin/{externalId}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Obtener orden detalle (Admin)", description = "Retorna el detalle completo de una orden. Solo admin y empleado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orden encontrada",
                    content = @Content(schema = @Schema(implementation = OrderAdminResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    })
    public ResponseEntity<OrderAdminResponseDTO> getAdminOrder(
            @Parameter(description = "ID externo de la orden") @PathVariable UUID externalId) {

        return ResponseEntity.ok(orderService.getAdminOrder(externalId));
    }
}
