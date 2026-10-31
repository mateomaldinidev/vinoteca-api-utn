package com.utn.corralon.features.user.service;

import com.utn.corralon.features.user.dto.UserRequestDTO;
import com.utn.corralon.features.user.dto.UserResponseDTO;

import java.util.List;
import java.util.UUID;

public interface IUserService {
    UserResponseDTO create(UserRequestDTO dto);
    UserResponseDTO register(UserRequestDTO dto);
    List<UserResponseDTO> getAll();
    UserResponseDTO getByExternalId(UUID externalId);
    UserResponseDTO update(UUID externalId, UserRequestDTO dto);
    void delete(UUID externalId);

}