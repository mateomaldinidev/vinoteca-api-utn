package com.utn.corralon.features.auth;

import com.utn.corralon.features.user.dto.UserRequestDTO;
import com.utn.corralon.features.user.dto.UserResponseDTO;
import com.utn.corralon.features.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
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
    public ResponseEntity<AuthResponse> authenticateUser(
            @RequestBody AuthRequest authRequest){
        CredentialsEntity user = (CredentialsEntity) authService.authenticate(authRequest);
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Crea un nuevo usuario en el sistema")
    public ResponseEntity<UserResponseDTO> registerUser(
            @RequestBody UserRequestDTO userRequestDTO) {
        return new ResponseEntity<>(
                userService.register(userRequestDTO),
                HttpStatus.CREATED
        );
    }

}
