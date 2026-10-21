package com.utn.vinoteca.features.notifications.service;

import com.utn.vinoteca.exception.ResourceNotFoundException;
import com.utn.vinoteca.features.notifications.dto.NotificationRequestDTO;
import com.utn.vinoteca.features.notifications.dto.NotificationResponseDTO;
import com.utn.vinoteca.features.notifications.entity.NotificationEntity;
import com.utn.vinoteca.features.notifications.mapper.NotificationMapper;
import com.utn.vinoteca.features.notifications.repository.NotificationRepository;
import com.utn.vinoteca.features.user.entity.UserEntity;
import com.utn.vinoteca.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final UserRepository userRepository; // Necesario para buscar el usuario en create

    @Override
    @Transactional
    public NotificationResponseDTO create(NotificationRequestDTO dto) {
        UserEntity user = userRepository.findByExternalId(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: ", dto.getUserId()));

        NotificationEntity notification = notificationMapper.toEntity(dto);
        notification.setUser(user);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false); // Por defecto, una notificacion se crea como no leida

        NotificationEntity savedNotification = notificationRepository.save(notification);
        return notificationMapper.toResponse(savedNotification);
    }

    @Override
    public List<NotificationResponseDTO> getAllByUserId(UUID userId) {
        // No es necesario buscar el UserEntity completo si solo necesitamos el userId para el repositorio
        if (!userRepository.existsByExternalId(userId)) {
            throw new ResourceNotFoundException("User not found with ID: ", userId);
        }

        return notificationRepository.findAllByUser_ExternalIdOrderByCreatedAtDesc(userId).stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public NotificationResponseDTO markAsRead(UUID notificationExternalId) {
        NotificationEntity notification = notificationRepository.findByExternalId(notificationExternalId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: ", notificationExternalId));

        notification.setRead(true);
        NotificationEntity updatedNotification = notificationRepository.save(notification);
        return notificationMapper.toResponse(updatedNotification);
    }

    @Override
    @Transactional
    public void delete(UUID notificationExternalId) {
        NotificationEntity notification = notificationRepository.findByExternalId(notificationExternalId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: ", notificationExternalId));
        notificationRepository.delete(notification);
    }

    @Override
    @Transactional
    public void markAllAsReadByUserId(UUID userId) {
        // No es necesario buscar el UserEntity completo si solo necesitamos el userId para el repositorio
        if (!userRepository.existsByExternalId(userId)) {
            throw new ResourceNotFoundException("User not found with ID: ", userId);
        }

        List<NotificationEntity> notifications = notificationRepository.findAllByUser_ExternalIdOrderByCreatedAtDesc(userId);
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }
}
