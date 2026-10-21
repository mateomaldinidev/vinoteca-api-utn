package com.utn.vinoteca.features.user.dto;

import com.utn.vinoteca.features.auth.Roles;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRequestDTO {
    @NotBlank
    @Schema(description = "User email address", example = "usuario@corralon.com")
    private String email;

    @NotBlank
    @Schema(description = "User first name", example = "Juan")
    private String name;

    @NotBlank
    @Schema(description = "User last name", example = "Perez")
    private String lastName;

    @NotBlank
    @Schema(description = "User password", example = "Password123!")
    private String password;

    @NotNull
    @Schema(description = "User role", example = "ROLE_CUSTOMER")
    private Roles role;
}
