package com.utn.vinoteca.features.user.mapper;

import com.utn.vinoteca.features.auth.CredentialsEntity;
import com.utn.vinoteca.features.auth.RoleEntity;
import com.utn.vinoteca.features.auth.Roles;
import com.utn.vinoteca.features.user.dto.UserRequestDTO;
import com.utn.vinoteca.features.user.dto.UserResponseDTO;
import com.utn.vinoteca.features.user.entity.UserEntity;
import jakarta.validation.constraints.NotNull;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {

    private final ModelMapper modelMapper;

    public UserMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public UserResponseDTO toResponse(UserEntity user) {
        return modelMapper.map(user, UserResponseDTO.class);
    }

    public UserResponseDTO toResponse(UserEntity user, CredentialsEntity credentials) {
        UserResponseDTO dto = modelMapper.map(user, UserResponseDTO.class);
        List<Roles> roles = credentials.getRoles().stream()
                .map(RoleEntity::getRole)
                .toList();
        dto.setRoles(roles);
        return dto;
    }

    public UserEntity toEntity(@NotNull UserRequestDTO dto) {
        return modelMapper.map(dto, UserEntity.class);
    }

    public void updateEntity(UserEntity user, UserRequestDTO dto) {
        modelMapper.map(dto, user);
    }
}