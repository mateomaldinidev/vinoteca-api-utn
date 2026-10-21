package com.utn.vinoteca.features.user.service;

import com.utn.vinoteca.exception.EmailAlreadyExistsException;
import com.utn.vinoteca.exception.ResourceNotFoundException;
import com.utn.vinoteca.features.auth.CredentialsEntity;
import com.utn.vinoteca.features.auth.CredentialsRepository;
import com.utn.vinoteca.features.auth.RoleEntity;
import com.utn.vinoteca.features.auth.RolesRepository;
import com.utn.vinoteca.features.notifications.service.IEmailService;
import com.utn.vinoteca.features.user.dto.UserRequestDTO;
import com.utn.vinoteca.features.user.dto.UserResponseDTO;
import com.utn.vinoteca.features.user.entity.UserEntity;
import com.utn.vinoteca.features.user.mapper.UserMapper;
import com.utn.vinoteca.features.user.repository.UserRepository;
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
    public UserResponseDTO create(UserRequestDTO dto) {
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
    public UserResponseDTO update(UUID externalId, UserRequestDTO dto) {
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

}