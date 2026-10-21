package com.utn.corralon.features.auth;

import com.utn.corralon.features.user.dto.UserRequestDTO;
import com.utn.corralon.features.user.dto.UserResponseDTO;
import com.utn.corralon.features.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints para autenticacion y registro de usuarios")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion", description = "Autentica un usuario y retorna un token JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario autenticado exitosamente",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Credenciales invalidas")
    })
    public ResponseEntity<AuthResponse> authenticateUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Credenciales de acceso")
            @RequestBody AuthRequest authRequest){
        CredentialsEntity user = (CredentialsEntity) authService.authenticate(authRequest);
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Crea un nuevo usuario en el sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente",
                    content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos")
    })
    public ResponseEntity<UserResponseDTO> registerUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos del nuevo usuario")
            @RequestBody UserRequestDTO userRequestDTO){
        return new ResponseEntity<>(userService.create(userRequestDTO),
                HttpStatus.CREATED);
    }

}
