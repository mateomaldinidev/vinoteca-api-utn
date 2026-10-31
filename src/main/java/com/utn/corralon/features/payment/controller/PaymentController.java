package com.utn.corralon.features.payment.controller;

import com.utn.corralon.features.payment.dto.PaymentRequestDTO;
import com.utn.corralon.features.payment.dto.PaymentResponseDTO;
import com.utn.corralon.features.payment.service.IPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
            @ApiResponse(responseCode = "200", description = "Pago procesado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error al procesar el pago")
    })
    public ResponseEntity<PaymentResponseDTO> pay(
            @Valid @RequestBody PaymentRequestDTO request,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        return ResponseEntity.ok(
                paymentService.pay(request, authentication.getName(), isAdmin)
        );
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAuthority('PAYMENT_READ')")
    @Operation(summary = "Obtener pago por orden", description = "Retorna el pago asociado a una orden")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago encontrado"),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    public ResponseEntity<PaymentResponseDTO> getByOrder(
            @Parameter(description = "ID externo de la orden")
            @PathVariable UUID orderId,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        return ResponseEntity.ok(
                paymentService.getByOrderId(orderId, authentication.getName(), isAdmin)
        );
    }
}
