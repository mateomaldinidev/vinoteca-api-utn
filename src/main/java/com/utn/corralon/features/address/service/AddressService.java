package com.utn.corralon.features.address.service;

import com.utn.corralon.exception.ResourceNotFoundException;
import com.utn.corralon.exception.UnauthorizedException;
import com.utn.corralon.features.address.dto.AddressRequestDTO;
import com.utn.corralon.features.address.dto.AddressResponseDTO;
import com.utn.corralon.features.address.entity.AddressEntity;
import com.utn.corralon.features.address.mapper.AddressMapper;
import com.utn.corralon.features.address.repository.AddressRepository;
import com.utn.corralon.features.user.entity.UserEntity;
import com.utn.corralon.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class AddressService implements IAddressService {
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AddressResponseDTO create(AddressRequestDTO dto, String username) {
        UserEntity authenticatedUser = getAuthenticatedUser(username);
        assertNoReassignment(dto, authenticatedUser);

        AddressEntity entity = addressMapper.toEntity(dto);
        entity.setUser(authenticatedUser);

        AddressEntity savedEntity = addressRepository.save(entity);
        return addressMapper.toResponse(savedEntity);
    }

    @Override
    public List<AddressResponseDTO> getAll() {
        return addressRepository.findAll().stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Override
    public AddressResponseDTO getByExternalId(UUID externalId, String username) {
        UserEntity authenticatedUser = getAuthenticatedUser(username);
        AddressEntity entity = findAddressOrThrow(externalId);

        assertOwnership(entity, authenticatedUser);

        return addressMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public AddressResponseDTO update(UUID externalId, AddressRequestDTO dto, String username) {
        UserEntity authenticatedUser = getAuthenticatedUser(username);
        AddressEntity entity = findAddressOrThrow(externalId);

        assertOwnership(entity, authenticatedUser);
        assertNoReassignment(dto, authenticatedUser);

        // El propietario nunca se modifica desde el request.
        addressMapper.updateEntity(entity, dto);

        AddressEntity updatedEntity = addressRepository.save(entity);
        return addressMapper.toResponse(updatedEntity);
    }

    @Override
    @Transactional
    public void delete(UUID externalId, UUID userExternalId, String username) {
        UserEntity authenticatedUser = getAuthenticatedUser(username);
        AddressEntity address = findAddressOrThrow(externalId);

        assertOwnership(address, authenticatedUser);

        // Compatibility: if the client still sends userExternalId, it must
        // match the authenticated user. It is never used as identity source.
        if (userExternalId != null
                && !userExternalId.equals(authenticatedUser.getExternalId())) {
            throw new UnauthorizedException("User ID does not match authenticated user");
        }

        address.setActive(false);
        addressRepository.save(address);
    }

    @Override
    public List<AddressResponseDTO> getAllByUserExternalId(UUID userExternalId, String username) {
        UserEntity authenticatedUser = getAuthenticatedUser(username);

        if (!authenticatedUser.getExternalId().equals(userExternalId)) {
            throw new UnauthorizedException("Cannot list addresses from another user");
        }

        return addressRepository.findAllByUser_ExternalId(authenticatedUser.getExternalId()).stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    private UserEntity getAuthenticatedUser(String username) {
        return userRepository.findByEmail(username)
                .filter(UserEntity::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: ", username));
    }

    private AddressEntity findAddressOrThrow(UUID externalId) {
        return addressRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with ID: ", externalId));
    }

    private void assertOwnership(AddressEntity address, UserEntity authenticatedUser) {
        if (address.getUser() == null
                || !address.getUser().getExternalId().equals(authenticatedUser.getExternalId())) {
            throw new UnauthorizedException("Address does not belong to user");
        }
    }

    private void assertNoReassignment(AddressRequestDTO dto, UserEntity authenticatedUser) {
        if (dto.getUserExternalId() != null
                && !dto.getUserExternalId().equals(authenticatedUser.getExternalId())) {
            throw new UnauthorizedException("Cannot assign address to another user");
        }
    }
}
