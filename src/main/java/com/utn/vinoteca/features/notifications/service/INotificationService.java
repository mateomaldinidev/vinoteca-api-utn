package com.utn.vinoteca.features.notifications.service;

import com.utn.vinoteca.features.notifications.dto.NotificationRequestDTO;
import com.utn.vinoteca.features.notifications.dto.NotificationResponseDTO;

import java.util.List;
import java.util.UUID;

public interface INotificationService {
    NotificationResponseDTO create(NotificationRequestDTO dto);
    List<NotificationResponseDTO> getAllByUserId(UUID userId);
    NotificationResponseDTO markAsRead(UUID notificationExternalId);
    void delete(UUID notificationExternalId);
    void markAllAsReadByUserId(UUID userId);
}
