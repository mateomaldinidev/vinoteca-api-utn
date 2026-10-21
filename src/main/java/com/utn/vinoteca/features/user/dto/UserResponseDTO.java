package com.utn.vinoteca.features.user.dto;

import com.utn.vinoteca.security.enums.Roles;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {

    @Schema(description = "Unique external identifier of the user", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID externalId;

    @Schema(description = "User email address", example = "usuario@vinoteca.com")
    private String email;

    @Schema(description = "User first name", example = "Juan")
    private String name;

    @Schema(description = "User last name", example = "Perez")
    private String lastName;

    @Schema(description = "List of user roles")
    private List<Roles> roles;

    @Schema(description = "Indicates if the user account is active", example = "true")
    private Boolean active;
}
