package com.kisansetu.auth;

import com.kisansetu.domain.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UserRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{3,50}", message = "3-50 letters, digits, '.', '_' or '-'") String username,
        @NotBlank String password,
        @NotNull Role role) {
}
