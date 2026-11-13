package com.utn.corralon.features.user.controller;

import com.utn.corralon.features.user.dto.UserProfileUpdateDTO;
import com.utn.corralon.features.user.dto.UserRequestDTO;
import com.utn.corralon.features.user.dto.UserResponseDTO;
import com.utn.corralon.features.user.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Endpoints para la gestion de usuarios")
public class UserController {
    private final IUserService userService;

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    @Operation(summary = "Crear usuario", description = "Crea un nuevo usuario en el sistema")
    @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente")
    public ResponseEntity<UserResponseDTO> create(@RequestBody @Valid UserRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(dto));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_LIST')")
    @Operation(summary = "Listar usuarios", description = "Retorna todos los usuarios del sistema")
    public ResponseEntity<List<UserResponseDTO>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener usuario autenticado",
            description = "Retorna los datos del usuario que realizó la petición, identificado a partir del token JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario autenticado encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<UserResponseDTO> getMe(Authentication authentication) {
        return ResponseEntity.ok(userService.getByEmail(authentication.getName()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    @Operation(summary = "Actualizar perfil propio",
            description = "Actualiza únicamente los datos personales del usuario autenticado. No permite modificar roles, permisos, identificadores ni credenciales")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil actualizado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<UserResponseDTO> updateMe(
            Authentication authentication,
            @RequestBody @Valid UserProfileUpdateDTO dto) {
        return ResponseEntity.ok(userService.updateProfile(authentication.getName(), dto));
    }

    @GetMapping("/{externalId}")
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Obtener usuario por ID", description = "Retorna un usuario por su externalId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<UserResponseDTO> getByExternalId(
            @Parameter(description = "ID externo del usuario") @PathVariable UUID externalId) {
        return ResponseEntity.ok(userService.getByExternalId(externalId));
    }

    @PutMapping("/{externalId}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Actualizar usuario", description = "Actualiza los datos de un usuario existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actualizado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<UserResponseDTO> update(
            @Parameter(description = "ID externo del usuario") @PathVariable UUID externalId,
            @RequestBody @Valid UserRequestDTO dto) {
        return ResponseEntity.ok(userService.update(externalId, dto));
    }

    @DeleteMapping("/{externalId}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    @Operation(summary = "Eliminar usuario", description = "Elimina un usuario del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario eliminado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID externo del usuario") @PathVariable UUID externalId) {
        userService.delete(externalId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inactive")
    @PreAuthorize("hasAuthority('USER_LIST')")
    @Operation(summary = "Listar usuarios inactivos")
    public ResponseEntity<List<UserResponseDTO>> getInactiveUsers() {
        return ResponseEntity.ok(userService.getInactiveUsers());
    }

    @PatchMapping("/{externalId}/activate")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Reactivar usuario")
    public ResponseEntity<Void> activate(
            @PathVariable UUID externalId) {
        userService.activate(externalId);
        return ResponseEntity.noContent().build();
    }
}
