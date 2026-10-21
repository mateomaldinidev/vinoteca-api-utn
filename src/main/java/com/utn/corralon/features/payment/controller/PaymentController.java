package com.utn.corralon.features.payment.controller;

import com.utn.corralon.features.payment.dto.PaymentRequestDTO;
import com.utn.corralon.features.payment.dto.PaymentResponseDTO;
import com.utn.corralon.features.payment.service.IPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Endpoints para la gestion de pagos")
public class PaymentController {

    private final IPaymentService paymentService;

    @PostMapping("/pay")
    @PreAuthorize("hasAuthority('PAYMENT_CREATE')")
    @Operation(summary = "Procesar pago", description = "Procesa el pago de una orden")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago procesado exitosamente",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Error al procesar el pago",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<PaymentResponseDTO> pay(
            @Valid @RequestBody PaymentRequestDTO request
    ) {

        return ResponseEntity.ok(
                paymentService.pay(request)
        );
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAuthority('PAYMENT_READ')")
    @Operation(summary = "Obtener pago por orden", description = "Retorna el pago asociado a una orden")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago encontrado",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    public ResponseEntity<PaymentResponseDTO> getByOrder(
            @Parameter(description = "ID externo de la orden") @PathVariable UUID orderId
    ) {
        return ResponseEntity.ok(
                paymentService.getByOrderId(orderId)
        );
    }
}
