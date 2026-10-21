package com.utn.vinoteca.features.auth;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthRequest(
        @Schema(description = "User username", example = "juan.perez")
        String username,
        @Schema(description = "User password", example = "Password123!")
        String password
) {
}
