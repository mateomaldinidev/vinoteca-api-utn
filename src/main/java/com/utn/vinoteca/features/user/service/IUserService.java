package com.utn.vinoteca.features.user.service;

import com.utn.vinoteca.features.user.dto.UserRequestDTO;
import com.utn.vinoteca.features.user.dto.UserResponseDTO;

import java.util.List;
import java.util.UUID;

public interface IUserService {
    UserResponseDTO create(UserRequestDTO dto);
    List<UserResponseDTO> getAll();
    UserResponseDTO getByExternalId(UUID externalId);
    UserResponseDTO update(UUID externalId, UserRequestDTO dto);
    void delete(UUID externalId);
}