package com.utn.corralon.features.notifications.dto;

import com.utn.corralon.features.notifications.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationResponseDTO {
    @Schema(description = "Unique external identifier of the notification", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID externalId;

    @Schema(description = "External identifier of the user receiving the notification", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID userId;

    @Schema(description = "Notification message", example = "Tu pedido ha sido confirmado")
    private String message;

    @Schema(description = "Notification creation date and time", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Indicates if the notification has been read", example = "false")
    private Boolean read;

    @Schema(description = "Type of notification", example = "ORDER_STATUS")
    private NotificationType type;

    @Schema(description = "Indicates if the notification has been sent", example = "true")
    private Boolean sent;
}
