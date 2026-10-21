package com.utn.vinoteca.features.notifications.mapper;

import com.utn.vinoteca.features.notifications.dto.NotificationRequestDTO;
import com.utn.vinoteca.features.notifications.dto.NotificationResponseDTO;
import com.utn.vinoteca.features.notifications.entity.NotificationEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    private final ModelMapper modelMapper;

    public NotificationMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public NotificationResponseDTO toResponse(NotificationEntity notification) {
        NotificationResponseDTO dto = modelMapper.map(notification, NotificationResponseDTO.class);
        // Asegurarse de que el userId se mapee correctamente desde el UserEntity
        if (notification.getUser() != null) {
            dto.setUserId(notification.getUser().getExternalId());
        }
        return dto;
    }

    public NotificationEntity toEntity(NotificationRequestDTO dto) {
        return modelMapper.map(dto, NotificationEntity.class);
    }
}