package com.utn.vinoteca.features.notifications.dto;

import com.utn.vinoteca.features.notifications.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class NotificationRequestDTO {
    @NotNull(message = "User ID cannot be null")
    @Schema(description = "External identifier of the user receiving the notification", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID userId;

    @NotBlank(message = "Message cannot be blank")
    @Schema(description = "Notification message", example = "Tu pedido ha sido confirmado")
    private String message;

    @NotNull(message = "Notification type cannot be null")
    @Schema(description = "Type of notification", example = "ORDER_STATUS")
    private NotificationType type;
}
