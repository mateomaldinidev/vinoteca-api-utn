package com.utn.corralon.features.user.service;

import com.utn.corralon.exception.EmailAlreadyExistsException;
import com.utn.corralon.exception.ResourceNotFoundException;
import com.utn.corralon.features.auth.CredentialsEntity;
import com.utn.corralon.features.auth.CredentialsRepository;
import com.utn.corralon.features.auth.Roles;
import com.utn.corralon.features.auth.RolesRepository;
import com.utn.corralon.features.notifications.service.IEmailService;
import com.utn.corralon.features.user.dto.UserProfileUpdateDTO;
import com.utn.corralon.features.user.dto.UserResponseDTO;
import com.utn.corralon.features.user.entity.UserEntity;
import com.utn.corralon.features.user.mapper.UserMapper;
import com.utn.corralon.features.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceProfileTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CredentialsRepository credentialsRepository;

    @Mock
    private RolesRepository rolesRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private IEmailService emailService;

    @InjectMocks
    private UserService userService;

    @Test
    void updateProfile_shouldUpdateNameLastNameAndEmail() {
        String email = "cliente@test.com";
        String newEmail = "nuevo@test.com";

        UserEntity user = UserEntity.builder()
                .externalId(UUID.randomUUID())
                .email(email)
                .name("Nombre viejo")
                .lastName("Apellido viejo")
                .active(true)
                .build();

        CredentialsEntity credentials = CredentialsEntity.builder()
                .username(email)
                .roles(new java.util.HashSet<>())
                .build();

        UserResponseDTO response = UserResponseDTO.builder()
                .externalId(user.getExternalId())
                .email(newEmail)
                .name("Nombre nuevo")
                .lastName("Apellido nuevo")
                .roles(List.of(Roles.ROLE_CUSTOMER))
                .active(true)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail(newEmail)).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(credentialsRepository.findByUsername(email)).thenReturn(Optional.of(credentials));
        when(userMapper.toResponse(user, credentials)).thenReturn(response);

        UserResponseDTO result = userService.updateProfile(
                email,
                new UserProfileUpdateDTO("  Nombre nuevo  ", " Apellido nuevo ", "  nuevo@test.com  ")
        );

        assertThat(user.getName()).isEqualTo("Nombre nuevo");
        assertThat(user.getLastName()).isEqualTo("Apellido nuevo");
        assertThat(user.getEmail()).isEqualTo(newEmail);
        assertThat(credentials.getUsername()).isEqualTo(newEmail);
        assertThat(result).isSameAs(response);
        verify(userRepository).save(user);
        verify(credentialsRepository).save(credentials);
    }

    @Test
    void updateProfile_shouldNotTouchCredentialsWhenEmailDoesNotChange() {
        String email = "cliente@test.com";

        UserEntity user = UserEntity.builder()
                .externalId(UUID.randomUUID())
                .email(email)
                .name("Nombre viejo")
                .lastName("Apellido viejo")
                .active(true)
                .build();

        CredentialsEntity credentials = CredentialsEntity.builder()
                .username(email)
                .roles(new java.util.HashSet<>())
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(credentialsRepository.findByUsername(email)).thenReturn(Optional.of(credentials));
        when(userMapper.toResponse(user, credentials)).thenReturn(new UserResponseDTO());

        userService.updateProfile(
                email,
                new UserProfileUpdateDTO("Nombre nuevo", "Apellido nuevo", email)
        );

        verify(userRepository, never()).existsByEmail(any());
        verify(credentialsRepository, never()).save(any(CredentialsEntity.class));
    }

    @Test
    void updateProfile_shouldRejectEmailUsedByAnotherUser() {
        String email = "cliente@test.com";
        String newEmail = "ocupado@test.com";

        UserEntity user = UserEntity.builder()
                .externalId(UUID.randomUUID())
                .email(email)
                .name("Nombre")
                .lastName("Apellido")
                .active(true)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail(newEmail)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(
                email,
                new UserProfileUpdateDTO("Nombre", "Apellido", newEmail)
        ))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("El email ya está registrado");

        assertThat(user.getEmail()).isEqualTo(email);
        verify(userRepository, never()).save(any(UserEntity.class));
        verify(credentialsRepository, never()).save(any(CredentialsEntity.class));
    }

    @Test
    void updateProfile_shouldRejectUnknownUser() {
        String email = "desconocido@test.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(
                email,
                new UserProfileUpdateDTO("Nombre", "Apellido", email)
        ))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void updateProfile_shouldRejectInactiveUser() {
        String email = "inactivo@test.com";
        UserEntity user = UserEntity.builder()
                .externalId(UUID.randomUUID())
                .email(email)
                .name("Nombre")
                .lastName("Apellido")
                .active(false)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateProfile(
                email,
                new UserProfileUpdateDTO("Nombre", "Apellido", email)
        ))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void profileUpdateDto_shouldOnlyExposeAllowedFields() {
        Set<String> fields = Arrays.stream(UserProfileUpdateDTO.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertThat(fields).containsExactlyInAnyOrder("name", "lastName", "email");
    }
}
