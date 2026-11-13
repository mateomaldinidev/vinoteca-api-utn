package com.utn.corralon.features.user.service;

import com.utn.corralon.features.user.dto.UserProfileUpdateDTO;
import com.utn.corralon.features.user.dto.UserRequestDTO;
import com.utn.corralon.features.user.dto.UserResponseDTO;

import java.util.List;
import java.util.UUID;

public interface IUserService {
    UserResponseDTO create(UserRequestDTO dto);
    UserResponseDTO register(UserRequestDTO dto);
    List<UserResponseDTO> getAll();
    UserResponseDTO getByExternalId(UUID externalId);
    UserResponseDTO getByEmail(String email);
    UserResponseDTO update(UUID externalId, UserRequestDTO dto);
    UserResponseDTO updateProfile(String email, UserProfileUpdateDTO dto);
    void delete(UUID externalId);
    List<UserResponseDTO> getInactiveUsers();
    void activate(UUID externalId);
}