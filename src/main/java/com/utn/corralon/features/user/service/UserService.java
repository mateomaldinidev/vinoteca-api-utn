package com.utn.corralon.features.user.service;

import com.utn.corralon.exception.EmailAlreadyExistsException;
import com.utn.corralon.exception.ResourceNotFoundException;
import com.utn.corralon.features.auth.*;
import com.utn.corralon.features.notifications.service.IEmailService;
import com.utn.corralon.features.user.dto.UserRequestDTO;
import com.utn.corralon.features.user.dto.UserResponseDTO;
import com.utn.corralon.features.user.entity.UserEntity;
import com.utn.corralon.features.user.mapper.UserMapper;
import com.utn.corralon.features.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CredentialsRepository credentialsRepository;
    private final RolesRepository rolesRepository;
    private final PasswordEncoder passwordEncoder;
    private final IEmailService emailService;

    @Override
    @Transactional
    public UserResponseDTO create(UserRequestDTO dto) {
        validateAdministrativeRole(dto.getRole());

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyExistsException("El email ya está registrado");
        }
        UserEntity entity = userMapper.toEntity(dto);

        entity.setPassword(
                passwordEncoder.encode(dto.getPassword())
        );

        entity.setCreatedAt(LocalDateTime.now());

        UserEntity saved = userRepository.save(entity);

        RoleEntity role = rolesRepository.findByRole(dto.getRole())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Role not found: " + dto.getRole()
                        )
                );

        CredentialsEntity credentials =
                CredentialsEntity.builder()
                        .username(saved.getEmail())
                        .password(saved.getPassword())
                        .enabled(true)
                        .usuario(saved)
                        .roles(Set.of(role))
                        .build();

        credentialsRepository.save(credentials);

        emailService.sendWelcomeEmail(saved.getEmail(), saved.getName());

        return userMapper.toResponse(saved, credentials);
    }

    @Override
    @Transactional
    public UserResponseDTO register(UserRequestDTO dto) {

        UserRequestDTO customerDto = UserRequestDTO.builder()
                .email(dto.getEmail())
                .name(dto.getName())
                .lastName(dto.getLastName())
                .password(dto.getPassword())
                .role(Roles.ROLE_CUSTOMER)
                .build();

        return createUser(customerDto);
    }

    @Override
    public List<UserResponseDTO> getAll() {
        return userRepository.findAllByActiveTrue().stream()
                .map(user -> {
                    CredentialsEntity credentials = credentialsRepository.findByUsername(user.getEmail())
                            .orElse(null);
                    return userMapper.toResponse(user, credentials);
                })
                .toList();
    }

    @Override
    public UserResponseDTO getByExternalId(UUID externalId) {
        UserEntity user = userRepository.findByExternalId(externalId)
                .filter(UserEntity::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: ", externalId));
        CredentialsEntity credentials = credentialsRepository.findByUsername(user.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Credentials not found for user: ", externalId));
        return userMapper.toResponse(user, credentials);
    }

    @Override
    @Transactional
    public UserResponseDTO update(UUID externalId, UserRequestDTO dto) {
        validateAdministrativeRole(dto.getRole());

        UserEntity entity = userRepository.findByExternalId(externalId)
                .filter(UserEntity::getActive)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cannot be updated. User not found with ID: ",
                                externalId)
                );

        CredentialsEntity credentials =
                credentialsRepository.findByUsername(entity.getEmail())
                        .orElseThrow(() ->
                                new RuntimeException("Credentials not found")
                        );

        boolean passwordChanged =
                !passwordEncoder.matches(
                        dto.getPassword(),
                        entity.getPassword()
                );

        userMapper.updateEntity(entity, dto);

        if (passwordChanged) {
            entity.setPassword(
                    passwordEncoder.encode(dto.getPassword())
            );
        }

        UserEntity updated = userRepository.save(entity);

        RoleEntity role = rolesRepository.findByRole(dto.getRole())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Role not found: " + dto.getRole())
                );

        credentials.setUsername(updated.getEmail());
        credentials.setPassword(updated.getPassword());

        Set<RoleEntity> roles = new HashSet<>();
        roles.add(role);

        credentials.setRoles(roles);

        credentialsRepository.save(credentials);

        if (passwordChanged) {
            emailService.sendPasswordChangedEmail(
                    updated.getEmail(),
                    updated.getName());
        }

        return userMapper.toResponse(updated, credentials);
    }

    @Override
    @Transactional
    public void delete(UUID externalId) {
        UserEntity entity = userRepository.findByExternalId(externalId)
                .filter(UserEntity::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot be deleted. User not found wit ID: ", externalId));
        CredentialsEntity credentials =
                credentialsRepository.findByUsername(entity.getEmail())
                        .orElseThrow(() ->
                                new RuntimeException("Credentials not found")
                        );

        entity.setActive(false);
        credentials.setEnabled(false);

        userRepository.save(entity);    
        credentialsRepository.save(credentials);
    }

    private void validateAdministrativeRole(Roles role) {

        if (role != Roles.ROLE_ADMIN && role != Roles.ROLE_EMPLOYEE) {
            throw new IllegalArgumentException(
                    "Solo se pueden crear o asignar usuarios ADMIN o EMPLOYEE"
            );
        }
    }

    private UserResponseDTO createUser(UserRequestDTO dto) {

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyExistsException("El email ya está registrado");
        }

        UserEntity entity = userMapper.toEntity(dto);

        entity.setPassword(
                passwordEncoder.encode(dto.getPassword())
        );

        entity.setCreatedAt(LocalDateTime.now());

        UserEntity saved = userRepository.save(entity);

        RoleEntity role = rolesRepository.findByRole(dto.getRole())
                .orElseThrow(() ->
                        new RuntimeException("Role not found: " + dto.getRole())
                );

        CredentialsEntity credentials = CredentialsEntity.builder()
                .username(saved.getEmail())
                .password(saved.getPassword())
                .enabled(true)
                .usuario(saved)
                .roles(Set.of(role))
                .build();

        credentialsRepository.save(credentials);

        emailService.sendWelcomeEmail(saved.getEmail(), saved.getName());

        return userMapper.toResponse(saved, credentials);
    }

}