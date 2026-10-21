package com.utn.corralon.features.notifications.controller;

import com.utn.corralon.features.notifications.dto.NotificationRequestDTO;
import com.utn.corralon.features.notifications.dto.NotificationResponseDTO;
import com.utn.corralon.features.notifications.service.INotificationService;
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
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Endpoints para la gestion de notificaciones")
public class NotificationController {

    private final INotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasAuthority('NOTIFICATION_CREATE')")
    @Operation(summary = "Crear notificacion", description = "Crea una nueva notificacion")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Notificacion creada exitosamente",
                    content = @Content(schema = @Schema(implementation = NotificationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<NotificationResponseDTO> create(@RequestBody @Valid NotificationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.create(dto));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('NOTIFICATION_READ_BY_USER')")
    @Operation(summary = "Obtener notificaciones de usuario", description = "Retorna todas las notificaciones de un usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificaciones encontradas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NotificationResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<List<NotificationResponseDTO>> getAllByUserId(
            @Parameter(description = "ID externo del usuario") @PathVariable UUID userId) {
        return ResponseEntity.ok(notificationService.getAllByUserId(userId));
    }

    @PatchMapping("/{notificationExternalId}/read")
    @PreAuthorize("hasAuthority('NOTIFICATION_MARK_AS_READ')")
    @Operation(summary = "Marcar notificacion como leida", description = "Marca una notificacion especifica como leida")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificacion marcada como leida",
                    content = @Content(schema = @Schema(implementation = NotificationResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Notificacion no encontrada")
    })
    public ResponseEntity<NotificationResponseDTO> markAsRead(
            @Parameter(description = "ID externo de la notificacion") @PathVariable UUID notificationExternalId) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationExternalId));
    }

    @DeleteMapping("/{notificationExternalId}")
    @PreAuthorize("hasAuthority('NOTIFICATION_DELETE')")
    @Operation(summary = "Eliminar notificacion", description = "Elimina una notificacion existente")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notificacion eliminada"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Notificacion no encontrada")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID externo de la notificacion") @PathVariable UUID notificationExternalId) {
        notificationService.delete(notificationExternalId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/user/{userId}/read-all")
    @PreAuthorize("hasAuthority('NOTIFICATION_MARK_ALL_AS_READ')")
    @Operation(summary = "Marcar todas como leidas", description = "Marca todas las notificaciones de un usuario como leidas")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Todas las notificaciones marcadas como leidas"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content)
    })
    public ResponseEntity<Void> markAllAsReadByUserId(
            @Parameter(description = "ID externo del usuario") @PathVariable UUID userId) {
        notificationService.markAllAsReadByUserId(userId);
        return ResponseEntity.noContent().build();
    }
}
